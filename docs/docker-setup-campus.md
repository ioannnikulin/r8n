# Docker Setup On This Machine

This note documents the setup that was required to make local Docker workflows work on this campus machine.

## Context

This machine runs Docker in rootless mode.
That has two practical consequences for this project:

- privileged host ports like `80` and `443` are not usable by default
- Docker storage location matters a lot

The working local URLs for this branch are:

- frontend HTTPS: `https://localhost:8443`
- gateway API: `https://localhost:8080`

## 1. Use Java 21

The backend build requires JDK 21 before `make docker-up` can build the backend JARs.

Persist Java 21 in the shell:

```sh
echo 'export JAVA_HOME=/sgoinfre/goinfre/Perso/$USER/jdk/jdk-21.0.10+7' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc
hash -r

java --version
javac --version
```

Expected: both commands report version `21`.

## 2. Move Gradle Cache Out Of Home

The home directory on this machine is too small for backend and Gradle caches.

```sh
mkdir -p /sgoinfre/goinfre/Perso/$USER/gradle
mv ~/.gradle ~/.gradle.backup-$(date +%Y%m%d-%H%M%S)
ln -s /sgoinfre/goinfre/Perso/$USER/gradle ~/.gradle
echo 'export GRADLE_USER_HOME=/sgoinfre/goinfre/Perso/$USER/gradle' >> ~/.zshrc
source ~/.zshrc
```

## 3. Clean Broken Docker Cache If Builds Fail Early

When Docker failed with `overlay2` / BuildKit errors, this fixed it:

```sh
docker system prune -af
systemctl --user restart docker
```

## 4. Do Not Keep Docker Data In Home

Home filled up quickly and broke image export/build.

Also, using `/sgoinfre/...` as Docker `data-root` did not work because it is NFS-backed and rootless Docker failed there during ownership changes.

The working Docker data root is on `/goinfre`, which is a local filesystem.

## 5. Configure Rootless Docker To Use /goinfre

The user Docker config path on this machine is symlinked, so write the config at the target:

```sh
mkdir -p /goinfre/$USER/.config/docker
mkdir -p /goinfre/$USER/docker
cat > /goinfre/$USER/.config/docker/daemon.json <<EOF
{
  "data-root": "/goinfre/$USER/docker"
}
EOF
```

Restart Docker and verify:

```sh
systemctl --user restart docker
docker info --format '{{.DockerRootDir}}'
```

Expected:

```sh
/goinfre/$USER/docker
```

## 6. Clean Old Docker Data From Home

After Docker is confirmed to use `/goinfre/$USER/docker`, remove the old home-directory Docker data to free space:

```sh
rm -rf ~/.local/share/docker
```

If some old `overlay2/.../work/work` directories resist deletion, removing most of the directory tree is still enough to recover space.

## 7. Use Unprivileged Frontend Host Ports

Because Docker is rootless here, binding host ports `80` and `443` failed.

The working frontend host ports are:

```env
FRONTEND_HTTP_HOST_PORT=8088
FRONTEND_HTTPS_HOST_PORT=8443
```

Those values live in:

- `deployment/config/docker.env`

## 8. Prevent Local Env Leakage Into Docker Compose

If `deployment/config/local.env` is exported in the shell before running Docker Compose, the gateway container can accidentally inherit local host values like:

- `SERVICES_MOCK_HOST=localhost`
- `SERVICES_OPINIONS_HOST=localhost`

That breaks routing inside Docker because `localhost` inside the gateway container is the gateway container itself, not the other services.

The Makefile was updated to unset the Docker-related env vars before invoking Docker Compose.

## 9. Start The Stack

From a shell with Java 21 active:

```sh
cd /home/$USER/trans
source ~/.zshrc
make docker-up
```

## 10. Verify The Working End State

Frontend:

```sh
curl -k -I https://localhost:8443
```

Gateway API via Nginx HTTPS:

```sh
curl -ik "https://localhost:8443/api/opinions/id?id=00000000-0000-0000-0000-000000000000" -H "Authorization: Bearer stub-access-token-123"
```

Gateway API:

```sh
make routed-request-opinion ENV=docker
```

Expected:

- frontend responds on `https://localhost:8443`
- gateway responds on `https://localhost:8080`
- Nginx proxies `/api` only to the gateway
- backend internal traffic stays on HTTP in the current local Docker setup

## Quick Checklist

Before `make docker-up`, verify:

```sh
java --version
docker info --format '{{.DockerRootDir}}'
df -h /home/$USER /goinfre
```

You want:

- Java 21 active
- Docker root at `/goinfre/$USER/docker`
- enough free space on `/goinfre`
