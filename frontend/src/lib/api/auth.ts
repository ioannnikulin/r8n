import type { HttpClient } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";

export interface LoginRequestDto {
  login: string;
  password: string;
}

export interface RegisterRequestDto {
  name: string;
  email: string;
  password: string;
  privacyPolicyAccepted: boolean;
  termsOfServiceAccepted: boolean;
}

export interface AuthenticationTokenDto {
  accessToken: string;
  expiresInMilliseconds: number;
}

const XSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";

export function createAuthApi(client: HttpClient = httpClient) {
  let csrfBootstrapPromise: Promise<void> | null = null;

  async function ensureCsrfToken(): Promise<void> {
    if (hasCookie(XSRF_TOKEN_COOKIE_NAME)) {
      return;
    }

    csrfBootstrapPromise ??= client
      .get<void>("/auth/csrf", {
        credentials: "include",
      })
      .finally(() => {
        csrfBootstrapPromise = null;
      });

    await csrfBootstrapPromise;
  }

  return {
    async login(request: LoginRequestDto): Promise<AuthenticationTokenDto> {
      await ensureCsrfToken();

      return client.post<AuthenticationTokenDto, LoginRequestDto>("/auth/login", {
        body: request,
        credentials: "include",
      });
    },

    async register(request: RegisterRequestDto): Promise<void> {
      await ensureCsrfToken();

      return client.post<void, RegisterRequestDto>("/auth/register", {
        body: request,
        credentials: "include",
      });
    },

    async logout(): Promise<void> {
      await ensureCsrfToken();

      return client.post<void>("/auth/logout", {
        credentials: "include",
      });
    },

    async refresh(): Promise<AuthenticationTokenDto> {
      await ensureCsrfToken();

      return client.post<AuthenticationTokenDto>("/auth/refresh", {
        credentials: "include",
      });
    },
  };
}

export const authApi = createAuthApi();

function hasCookie(name: string): boolean {
  if (typeof document === "undefined") {
    return true;
  }

  const prefix = `${name}=`;

  return document.cookie
    .split(";")
    .some((cookie) => {
      const trimmedCookie = cookie.trim();
      return (
        trimmedCookie.startsWith(prefix) &&
        trimmedCookie.slice(prefix.length) !== ""
      );
    });
}
