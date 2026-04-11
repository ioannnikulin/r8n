type PrimitiveQueryValue = string | number | boolean;
type QueryValue =
  | PrimitiveQueryValue
  | null
  | undefined
  | readonly (PrimitiveQueryValue | null | undefined)[];

export type HttpQueryParams = Record<string, QueryValue>;
export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface HttpRequestOptions<TBody = unknown> {
  body?: TBody;
  credentials?: RequestCredentials;
  headers?: HeadersInit;
  query?: HttpQueryParams;
  signal?: AbortSignal;
  timeoutMs?: number;
}

export interface HttpClientConfig {
  baseUrl?: string;
  credentials?: RequestCredentials;
  defaultHeaders?: HeadersInit;
  defaultTimeoutMs?: number;
  fetchFn?: typeof fetch;
}

export interface HttpClient {
  delete<TResponse = void>(
    path: string,
    options?: HttpRequestOptions,
  ): Promise<TResponse>;
  get<TResponse = void>(
    path: string,
    options?: Omit<HttpRequestOptions, "body">,
  ): Promise<TResponse>;
  patch<TResponse = void, TBody = unknown>(
    path: string,
    options?: HttpRequestOptions<TBody>,
  ): Promise<TResponse>;
  post<TResponse = void, TBody = unknown>(
    path: string,
    options?: HttpRequestOptions<TBody>,
  ): Promise<TResponse>;
  put<TResponse = void, TBody = unknown>(
    path: string,
    options?: HttpRequestOptions<TBody>,
  ): Promise<TResponse>;
  request<TResponse = void, TBody = unknown>(
    method: HttpMethod,
    path: string,
    options?: HttpRequestOptions<TBody>,
  ): Promise<TResponse>;
}

export class HttpError extends Error {
  readonly status: number;
  readonly responseBody: unknown;

  constructor(message: string, status: number, responseBody?: unknown) {
    super(message);
    this.name = "HttpError";
    this.status = status;
    this.responseBody = responseBody;
  }
}

const DEFAULT_API_BASE_URL = sanitizeConfiguredBaseUrl(
  import.meta.env.VITE_API_BASE_URL,
);
const DEFAULT_TIMEOUT_MS = sanitizeTimeoutMs(
  import.meta.env.VITE_API_TIMEOUT_MS ?? 10000,
);

export const httpClient = createHttpClient({
  baseUrl: DEFAULT_API_BASE_URL,
  defaultTimeoutMs: DEFAULT_TIMEOUT_MS,
});

export function createHttpClient(config: HttpClientConfig = {}): HttpClient {
  const baseUrl = sanitizeConfiguredBaseUrl(config.baseUrl);
  const defaultTimeoutMs = sanitizeTimeoutMs(config.defaultTimeoutMs);
  const fetchFn = config.fetchFn ?? fetch;
  const defaultCredentials = config.credentials ?? "same-origin";

  async function request<TResponse = void, TBody = unknown>(
    method: HttpMethod,
    path: string,
    options: HttpRequestOptions<TBody> = {},
  ): Promise<TResponse> {
    const { cleanup, signal, timeoutError } = createRequestSignal(
      options.signal,
      options.timeoutMs ?? defaultTimeoutMs,
    );
    const requestHeaders = createHeaders(config.defaultHeaders, options.headers);
    addCsrfToken(method, requestHeaders);
    const requestBody = serializeBody(method, options.body, requestHeaders);
    const requestUrl = buildUrl(baseUrl, path, options.query);

    try {
      const response = await fetchFn(requestUrl, {
        body: requestBody,
        credentials: options.credentials ?? defaultCredentials,
        headers: requestHeaders,
        method,
        signal,
      });

      const responseBody = await parseResponseBody(response);

      if (!response.ok) {
        throw new HttpError(
          getErrorMessage(response.status, responseBody),
          response.status,
          responseBody,
        );
      }

      return responseBody as TResponse;
    } catch (error) {
      if (timeoutError && signal.aborted && signal.reason === timeoutError) {
        throw new HttpError(timeoutError.message, 0);
      }

      if (isAbortError(error)) {
        throw error;
      }

      if (error instanceof HttpError) {
        throw error;
      }

      throw new HttpError("Network request failed", 0);
    } finally {
      cleanup();
    }
  }

  return {
    delete: <TResponse = void>(path: string, options?: HttpRequestOptions) =>
      request<TResponse>("DELETE", path, options),
    get: <TResponse = void>(
      path: string,
      options?: Omit<HttpRequestOptions, "body">,
    ) => request<TResponse>("GET", path, options),
    patch: <TResponse = void, TBody = unknown>(
      path: string,
      options?: HttpRequestOptions<TBody>,
    ) => request<TResponse, TBody>("PATCH", path, options),
    post: <TResponse = void, TBody = unknown>(
      path: string,
      options?: HttpRequestOptions<TBody>,
    ) => request<TResponse, TBody>("POST", path, options),
    put: <TResponse = void, TBody = unknown>(
      path: string,
      options?: HttpRequestOptions<TBody>,
    ) => request<TResponse, TBody>("PUT", path, options),
    request,
  };
}

function buildUrl(baseUrl: string, path: string, query?: HttpQueryParams): string {
  assertRelativePath(path);

  const url = new URL(`${baseUrl}${path}`, "https://placeholder.invalid");

  if (query) {
    appendQueryParams(url.searchParams, query);
  }

  return `${url.pathname}${url.search}`;
}

function appendQueryParams(
  searchParams: URLSearchParams,
  query: HttpQueryParams,
): void {
  Object.entries(query).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      value.forEach((item) => appendQueryParam(searchParams, key, item));
      return;
    }

    appendQueryParam(
      searchParams,
      key,
      value as PrimitiveQueryValue | null | undefined,
    );
  });
}

function appendQueryParam(
  searchParams: URLSearchParams,
  key: string,
  value: PrimitiveQueryValue | null | undefined,
): void {
  if (value === null || value === undefined) {
    return;
  }

  searchParams.append(key, String(value));
}

function assertRelativePath(path: string): void {
  if (path.startsWith("//") || isAbsoluteHttpUrl(path)) {
    throw new Error("Absolute request URLs are not allowed.");
  }

  if (!path.startsWith("/")) {
    throw new Error("HTTP client paths must start with '/'.");
  }
}

function isAbsoluteHttpUrl(value: string): boolean {
  return /^https?:\/\//i.test(value);
}

function createHeaders(
  defaultHeaders?: HeadersInit,
  requestHeaders?: HeadersInit,
): Headers {
  const headers = new Headers(defaultHeaders);
  const additionalHeaders = new Headers(requestHeaders);

  additionalHeaders.forEach((value, key) => {
    headers.set(key, value);
  });

  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  return headers;
}

function serializeBody(
  method: HttpMethod,
  body: unknown,
  headers: Headers,
): BodyInit | undefined {
  if (body === undefined) {
    return undefined;
  }

  if (method === "GET") {
    throw new Error("GET requests cannot include a request body.");
  }

  if (isBodyInitValue(body)) {
    return body;
  }

  if (!headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  return JSON.stringify(body);
}

function isBodyInitValue(value: unknown): value is BodyInit {
  if (typeof value === "string") {
    return true;
  }

  if (typeof FormData !== "undefined" && value instanceof FormData) {
    return true;
  }

  return false;
}

function isAbortError(error: unknown): boolean {
  if (typeof DOMException !== "undefined" && error instanceof DOMException) {
    return error.name === "AbortError";
  }

  return error instanceof Error && error.name === "AbortError";
}

async function parseResponseBody(response: Response): Promise<unknown> {
  if (response.status === 204 || response.status === 205) {
    return undefined;
  }

  const contentType = response.headers.get("content-type") ?? "";

  if (isJsonContentType(contentType)) {
    try {
      return await response.json();
    } catch {
      throw new HttpError("Server returned invalid JSON.", response.status);
    }
  }

  const text = await response.text();
  return text === "" ? undefined : text;
}

function isJsonContentType(contentType: string): boolean {
  return (
    contentType.includes("application/json") ||
    contentType.includes("+json")
  );
}

function getErrorMessage(status: number, responseBody: unknown): string {
  if (status >= 500) {
    return "Server error";
  }

  const serverMessage = extractServerMessage(responseBody);

  if (serverMessage) {
    return serverMessage;
  }

  return `Request failed with status ${status}`;
}

function extractServerMessage(responseBody: unknown): string | undefined {
  if (!responseBody || typeof responseBody !== "object") {
    return undefined;
  }

  const responseRecord = responseBody as Record<string, unknown>;

  const message = responseRecord.message;
  if (typeof message === "string" && message.trim() !== "") {
    return message.trim();
  }

  const detail = responseRecord.detail;
  if (typeof detail === "string" && detail.trim() !== "") {
    return detail.trim();
  }

  return undefined;
}

function addCsrfToken(method: HttpMethod, headers: Headers): void {
  if (method === "GET") {
    return;
  }

  const csrfToken = getCookie("XSRF-TOKEN");
  if (csrfToken) {
    headers.set("X-XSRF-TOKEN", csrfToken);
  }
}

function getCookie(name: string): string | undefined {
  if (typeof document === "undefined") {
    return undefined;
  }

  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) {
    return parts.pop()?.split(";").shift();
  }
  return undefined;
}

function createRequestSignal(signal?: AbortSignal, timeoutMs?: number) {
  const controller = new AbortController();
  const sanitizedTimeoutMs = sanitizeTimeoutMs(timeoutMs);
  const forwardAbort = () => {
    controller.abort(signal?.reason ?? new DOMException("Aborted", "AbortError"));
  };

  if (signal?.aborted) {
    forwardAbort();
  } else {
    signal?.addEventListener("abort", forwardAbort, { once: true });
  }

  let timeoutId: ReturnType<typeof setTimeout> | undefined;
  let timeoutError: Error | undefined;

  if (sanitizedTimeoutMs) {
    timeoutError = new Error(
      `Request timed out after ${sanitizedTimeoutMs} ms.`,
    );
    timeoutId = setTimeout(() => controller.abort(timeoutError), sanitizedTimeoutMs);
  }

  return {
    cleanup: () => {
      signal?.removeEventListener("abort", forwardAbort);
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    },
    signal: controller.signal,
    timeoutError,
  };
}

function sanitizeConfiguredBaseUrl(baseUrl?: string): string {
  if (!baseUrl) {
    return "/api";
  }

  const trimmedBaseUrl = baseUrl.trim();
  if (trimmedBaseUrl === "") {
    return "/api";
  }

  if (trimmedBaseUrl.startsWith("//")) {
    throw new Error("VITE_API_BASE_URL must not use a protocol-relative URL.");
  }

  if (!trimmedBaseUrl.startsWith("/")) {
    throw new Error("VITE_API_BASE_URL must start with '/'.");
  }

  return trimmedBaseUrl.endsWith("/")
    ? trimmedBaseUrl.slice(0, -1)
    : trimmedBaseUrl;
}

function sanitizeTimeoutMs(timeoutMs?: number | string): number | undefined {
  if (timeoutMs === undefined) {
    return undefined;
  }

  if (timeoutMs === "") {
    return 10000;
  }

  const normalizedTimeoutMs =
    typeof timeoutMs === "string" ? Number(timeoutMs) : timeoutMs;

  if (!Number.isFinite(normalizedTimeoutMs) || normalizedTimeoutMs <= 0) {
    throw new Error("HTTP timeout must be a positive number.");
  }

  return normalizedTimeoutMs;
}
