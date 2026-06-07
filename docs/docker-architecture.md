# Docker Architecture (HTTPS at the Edge + Internal HTTPS)

This document explains the Docker networking layout, TLS termination, and how the current local Docker setup uses HTTPS on the private network.

## Core Idea

- HTTPS is required at the **edge** (browser <-> Nginx) to protect user traffic.
- Inside the Docker network, services use HTTPS in the current local setup.
- Nginx terminates edge TLS and forwards requests to the gateway over HTTPS.
- The gateway calls backend services over HTTPS while still using the same internal ports.

## High-Level Flows

Local dev (no Docker):

```mermaid
graph LR
  Browser["Browser"]
  Vite["Vite Dev Server (localhost:5173)"]
  Gateway["Gateway API (localhost:8080)"]
  Opinions["Opinions Service (localhost:8081)"]
  Users["Users Service (localhost:8082)"]
Export["Export Service (localhost:8083)"]
Mock["Mock Service (localhost:8090)"]

  Browser -->|"http://localhost:5173"| Vite
  Vite -->|"/api -> https://localhost:8080"| Gateway
  Gateway -->|"https://localhost:8081"| Opinions
  Gateway -->|"https://localhost:8082"| Users
Gateway -->|"https://localhost:8083"| Export
Gateway -->|"https://localhost:8090"| Mock
```

Local production (Docker):

```mermaid
graph LR
  Browser["Browser"]
  Nginx["Nginx (frontend)"]
  Gateway["Gateway API (gateway:8080)"]
  Opinions["Opinions Service (opinions:8080)"]
  Users["Users Service (users:8080)"]
Export["Export Service (export:8080)"]
Mock["Mock Service (mock:8080)"]

  Browser -->|"HTTPS :8443 -> container :443"| Nginx
  Nginx -->|"HTTPS :8080"| Gateway
  Gateway -->|"HTTPS :8080"| Opinions
  Gateway -->|"HTTPS :8080"| Users
Gateway -->|"HTTPS :8080"| Export
Gateway -->|"HTTPS :8080"| Mock
```

## Runtime API Sequence (Nginx + Gateway)

```mermaid
sequenceDiagram
  participant B as Browser
  participant N as Nginx
  participant G as Gateway
  participant O as Opinions

  B->>N: GET https://localhost:8443/
  N-->>B: HTML/JS/CSS (Vue app)

  B->>N: API call /api/...
  N->>G: Proxy to https://gateway:8080/...
  G->>O: HTTPS https://opinions:8080/...
  O-->>G: JSON response
  N-->>B: JSON response
```

## Dev vs Runtime

Dev (Vite):
- Browser talks directly to the Vite dev server at `http://localhost:5173`.
- Vite proxies `/api` to the gateway at `https://localhost:8080`.
- The gateway reaches services on `https://localhost:8081` and `https://localhost:8090`.
- No Nginx involved.

Runtime (local production):
- Browser talks to Nginx over `https://localhost:8443` by default.
- Nginx serves static assets and proxies `/api` to the gateway at `https://gateway:8080`.
- The gateway reaches services by name on `https://opinions:8080`, `https://users:8080`, `https://export:8080`, and `https://mock:8080`.
- This mirrors a production edge setup with TLS termination at the edge and HTTPS on the private network.

## Ports by Mode

Local dev (no Docker):
- `gateway` -> `localhost:8080`
- `opinions` -> `localhost:8081`
- `mock` -> `localhost:8090`

Local production (Docker):
- browser -> `localhost:8443` (HTTPS)
- `gateway` -> `localhost:8080` from the host, `gateway:8080` inside Docker
- `opinions` -> `opinions:8080` inside Docker only
- `mock` -> `mock:8080` inside Docker only

Inside Docker, containers are isolated. Multiple services can use the same port because each service has its own hostname.
From the host machine, only published ports are reachable. In this setup:
- `localhost:8443` maps to `frontend:443`
- `localhost:8080` maps to `gateway:8080`

## Docker Network View

```mermaid
graph TB
  subgraph Host["Host Machine"]
    Browser["Browser"]
  end

  subgraph Docker["Docker Bridge Network (r8n_net)"]
    Nginx["frontend (nginx:80/443)"]
    Gateway["gateway (8080)"]
    Opinions["opinions (8080)"]
    Mock["mock (8080)"]
  end

  Browser -->|"HTTPS :8443"| Nginx
  Nginx -->|"HTTPS :8080"| Gateway
  Gateway -->|"HTTPS :8080"| Opinions
  Gateway -->|"HTTPS :8080"| Users
Gateway -->|"HTTPS :8080"| Export
Gateway -->|"HTTPS :8080"| Mock
```

## Why HTTPS at the Edge

- The browser is outside the Docker network, so traffic must be encrypted.
- The Docker bridge network is isolated and private to the host machine.
- The private Docker network keeps the internal traffic simple in local development.
- Internal TLS can be added later if needed, but it adds certificate management complexity.

## Notes

- If deployed to a public environment, internal TLS can be added as well.
- For local production, TLS at the edge is mandatory; internal service traffic stays on HTTP in the current setup.
- On rootless Docker, host ports `80` and `443` may not be bindable. This branch uses `8088` and `8443` by default for that reason.
