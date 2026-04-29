import { beforeEach, describe, expect, it, vi } from "vitest";
import { clearSession, setSession } from "@/lib/auth/session";
import { createHttpClient } from "@/lib/http-client";
import { createAccessRequestsApi } from "@/lib/api/access-requests";
import { createAuthApi } from "@/lib/api/auth";
import { createMessagingApi } from "@/lib/api/messaging";
import { createOpinionListsApi } from "@/lib/api/opinion-lists";
import { createOpinionsApi } from "@/lib/api/opinions";
import { createReferentsApi } from "@/lib/api/referents";
import { createSelectorsApi } from "@/lib/api/selectors";
import { createUsersApi } from "@/lib/api/users";

function createJsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
    status: 200,
  });
}

function createEmptyResponse(): Response {
  return new Response(null, {
    status: 204,
  });
}

function clearCookie(name: string): void {
  document.cookie = `${name}=; Max-Age=0; Path=/`;
}

function setCookie(name: string, value: string): void {
  document.cookie = `${name}=${value}; Path=/`;
}

describe("API modules", () => {
  beforeEach(() => {
    clearSession();
    clearCookie("XSRF-TOKEN");
    setSession(
      {
        accessToken: "stub-access-token-123",
        expiresInMilliseconds: 60_000,
      },
      Date.now(),
    );
  });

  it("bootstraps csrf before posting login credentials when the xsrf cookie is missing", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(createEmptyResponse())
      .mockResolvedValueOnce(
        createJsonResponse({
          accessToken: "access-token",
          expiresInMilliseconds: 1000,
        }),
      );
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const authApi = createAuthApi(client);

    await authApi.login({
      login: "test",
      password: "1234",
    });

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "/api/auth/csrf",
      expect.objectContaining({
        credentials: "include",
        method: "GET",
      }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/auth/login",
      expect.objectContaining({
        body: JSON.stringify({
          login: "test",
          password: "1234",
        }),
        credentials: "include",
        method: "POST",
      }),
    );
  });

  it("skips csrf bootstrap when the xsrf cookie already exists", async () => {
    setCookie("XSRF-TOKEN", "existing-xsrf-token");
    const fetchMock = vi.fn().mockResolvedValue(
      createJsonResponse({
        accessToken: "access-token",
        expiresInMilliseconds: 1000,
      }),
    );
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const authApi = createAuthApi(client);

    await authApi.login({
      login: "test",
      password: "1234",
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/auth/login",
      expect.objectContaining({
        method: "POST",
      }),
    );

    const [, requestInit] = fetchMock.mock.calls[0];
    const headers = new Headers(requestInit.headers);
    expect(headers.get("X-XSRF-TOKEN")).toBe("existing-xsrf-token");
  });

  it("bootstraps csrf before registering a new account", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(createEmptyResponse())
      .mockResolvedValueOnce(createEmptyResponse());
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const authApi = createAuthApi(client);

    await authApi.register({
      name: "New Reviewer",
      email: "new-user@test.test",
      password: "long-enough-password",
      privacyPolicyAccepted: true,
      termsOfServiceAccepted: true,
    });

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "/api/auth/csrf",
      expect.objectContaining({
        credentials: "include",
        method: "GET",
      }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/auth/register",
      expect.objectContaining({
        body: JSON.stringify({
          name: "New Reviewer",
          email: "new-user@test.test",
          password: "long-enough-password",
          privacyPolicyAccepted: true,
          termsOfServiceAccepted: true,
        }),
        credentials: "include",
        method: "POST",
      }),
    );
  });

  it("bootstraps csrf before refresh and does not send a refresh token from JavaScript", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(createEmptyResponse())
      .mockResolvedValueOnce(
        createJsonResponse({
          accessToken: "access-token",
          expiresInMilliseconds: 1000,
        }),
      );
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const authApi = createAuthApi(client);

    await authApi.refresh();

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "/api/auth/csrf",
      expect.objectContaining({
        credentials: "include",
        method: "GET",
      }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/auth/refresh",
      expect.objectContaining({
        credentials: "include",
        method: "POST",
      }),
    );

    const [, requestInit] = fetchMock.mock.calls[1];
    expect(requestInit.body).toBeUndefined();
  });

  it("bootstraps csrf before logout", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(createEmptyResponse())
      .mockResolvedValueOnce(createEmptyResponse());
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const authApi = createAuthApi(client);

    await authApi.logout();

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "/api/auth/csrf",
      expect.objectContaining({
        credentials: "include",
        method: "GET",
      }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/auth/logout",
      expect.objectContaining({
        credentials: "include",
        method: "POST",
      }),
    );
  });

  it("reuses the shared auth flow for protected opinions endpoints", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      createJsonResponse({
        componentMark: null,
        components: [],
        id: "opinion-id",
        mark: 8,
        objective: [],
        owner: "owner-id",
        ownerName: "Jane Doe",
        status: "PUBLISHED",
        subject: "subject-id",
        subjective: [],
        subjectName: "Bonanza Coffee",
        timestamp: "2026-03-28T10:00:00Z",
      }),
    );
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const opinionsApi = createOpinionsApi(client);

    await opinionsApi.getForSubject({
      subjectId: "00000000-0000-0000-0000-000000000000",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/opinions/for/00000000-0000-0000-0000-000000000000",
      expect.objectContaining({
        method: "GET",
      }),
    );

    const [, requestInit] = fetchMock.mock.calls[0];
    const headers = new Headers(requestInit.headers);
    expect(headers.get("Authorization")).toBe("Bearer stub-access-token-123");
  });

  it("encodes pageable sort fields as nested query parameters", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      createJsonResponse({
        items: [],
        page: 0,
        size: 10,
        total: 0,
      }),
    );
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const referentsApi = createReferentsApi(client);

    await referentsApi.find({
      query: "cafe",
      pageable: {
        page: 0,
        size: 10,
        sort: [{ property: "name", direction: "ASC" }],
      },
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/referents/find?page=0&size=10&sort%5B0%5D.property=name&sort%5B0%5D.direction=ASC&query=cafe",
      expect.objectContaining({
        method: "GET",
      }),
    );
  });

  it("uses backend avatar routes with blob and multipart bodies", async () => {
    setCookie("XSRF-TOKEN", "xsrf-token");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response("avatar-bytes", {
          headers: { "Content-Type": "image/png" },
          status: 200,
        }),
      )
      .mockResolvedValueOnce(createEmptyResponse())
      .mockResolvedValueOnce(createEmptyResponse());
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const usersApi = createUsersApi(client);

    const avatar = await usersApi.getUserAvatar(
      "11111111-1111-1111-1111-111111111111",
    );
    await usersApi.uploadMyAvatar({
      file: new File(["avatar"], "avatar.png", { type: "image/png" }),
    });
    await usersApi.deleteMyAvatar();

    expect(avatar.type).toBe("image/png");
    expect(await avatar.text()).toBe("avatar-bytes");
    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/users/11111111-1111-1111-1111-111111111111/avatar",
    );
    expect(fetchMock.mock.calls[1][0]).toBe("/api/users/me/avatar");
    expect(fetchMock.mock.calls[2][0]).toBe("/api/users/me/avatar");

    const getHeaders = new Headers(fetchMock.mock.calls[0][1].headers);
    expect(getHeaders.get("Accept")).toBe("image/*");

    const uploadRequest = fetchMock.mock.calls[1][1];
    const uploadHeaders = new Headers(uploadRequest.headers);
    expect(uploadRequest).toEqual(expect.objectContaining({ method: "POST" }));
    expect(uploadRequest.body).toBeInstanceOf(FormData);
    expect(uploadHeaders.get("Content-Type")).toBeNull();
    expect(uploadHeaders.get("Authorization")).toBe("Bearer stub-access-token-123");
    expect(uploadHeaders.get("X-XSRF-TOKEN")).toBe("xsrf-token");

    expect(fetchMock.mock.calls[2][1]).toEqual(
      expect.objectContaining({ method: "DELETE" }),
    );
  });

  it("updates the current user's public profile through the users API", async () => {
    setCookie("XSRF-TOKEN", "xsrf-token");
    const fetchMock = vi.fn().mockResolvedValue(
      createJsonResponse({
        about: "Privacy-conscious coffee reviewer",
        id: "11111111-1111-1111-1111-111111111111",
        lastSeenAt: null,
        location: "Hamburg, Germany",
        name: "Jane Reviewer",
        status: "ACTIVE",
      }),
    );

    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const usersApi = createUsersApi(client);

    await usersApi.updateMyPublicProfile({
      about: "Privacy-conscious coffee reviewer",
      location: "Hamburg, Germany",
      name: "Jane Reviewer",
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/users/me/public-profile",
      expect.objectContaining({
        body: JSON.stringify({
          about: "Privacy-conscious coffee reviewer",
          location: "Hamburg, Germany",
          name: "Jane Reviewer",
        }),
        method: "PATCH",
      }),
    );

    const [, requestInit] = fetchMock.mock.calls[0];
    const headers = new Headers(requestInit.headers);
    expect(headers.get("Authorization")).toBe("Bearer stub-access-token-123");
    expect(headers.get("X-XSRF-TOKEN")).toBe("xsrf-token");
  });

  it("uses backend user profile DTO with last seen timestamp", async () => {
    const userId = "11111111-1111-1111-1111-111111111111";
    const profile = {
      id: userId,
      name: "Jane Doe",
      status: "ACTIVE",
      lastSeenAt: "2026-04-25T10:15:30Z",
      about: "Coffee reviewer",
      location: "Berlin, Germany",
    };
    const fetchMock = vi.fn().mockResolvedValue(createJsonResponse(profile));
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const usersApi = createUsersApi(client);

    await expect(usersApi.getUser(userId)).resolves.toEqual(profile);
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/users/${userId}`,
      expect.objectContaining({
        method: "GET",
      }),
    );
  });

  it("merges pagination and filters for opinion list searches", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      createJsonResponse({
        items: [],
        page: 0,
        size: 20,
        total: 0,
      }),
    );
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const opinionListsApi = createOpinionListsApi(client);

    await opinionListsApi.search({
      filters: {
        authorNameSubstring: "Jane",
        nameSubstring: "coffee",
      },
      pageable: {
        page: 0,
        size: 20,
      },
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/opinion-lists/search?page=0&size=20&authorNameSubstring=Jane&nameSubstring=coffee",
      expect.objectContaining({
        method: "GET",
      }),
    );
  });

  it("uses backend path parameters for opinion mutations", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(createJsonResponse({})));
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const opinionsApi = createOpinionsApi(client);

    await opinionsApi.getById({
      id: "11111111-1111-1111-1111-111111111111",
    });
    await opinionsApi.create({
      mark: 5,
      subjectId: "22222222-2222-2222-2222-222222222222",
    });
    await opinionsApi.update({
      opinionId: "33333333-3333-3333-3333-333333333333",
    });
    await opinionsApi.delete({
      opinionId: "44444444-4444-4444-4444-444444444444",
    });
    await opinionsApi.unlinkComponent({
      linkId: "55555555-5555-5555-5555-555555555555",
    });
    await opinionsApi.adjustComponentWeight({
      linkId: "66666666-6666-6666-6666-666666666666",
      weight: 0.5,
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/opinions/11111111-1111-1111-1111-111111111111",
    );
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/opinions?mark=5&subjectId=22222222-2222-2222-2222-222222222222",
    );
    expect(fetchMock.mock.calls[2][0]).toBe(
      "/api/opinions/33333333-3333-3333-3333-333333333333",
    );
    expect(fetchMock.mock.calls[3][0]).toBe(
      "/api/opinions/44444444-4444-4444-4444-444444444444",
    );
    expect(fetchMock.mock.calls[4][0]).toBe(
      "/api/opinions/unlink/55555555-5555-5555-5555-555555555555",
    );
    expect(fetchMock.mock.calls[5][0]).toBe(
      "/api/opinions/adjust-weight/66666666-6666-6666-6666-666666666666?weight=0.5",
    );

    expect(fetchMock.mock.calls[1][1]).toEqual(
      expect.objectContaining({ method: "POST" }),
    );
    expect(fetchMock.mock.calls[2][1]).toEqual(
      expect.objectContaining({ method: "PATCH" }),
    );
    expect(fetchMock.mock.calls[3][1]).toEqual(
      expect.objectContaining({ method: "DELETE" }),
    );
    expect(new Headers(fetchMock.mock.calls[0][1].headers).get("Authorization")).toBe(
      "Bearer stub-access-token-123",
    );
  });

  it("passes publishedAfter as a query param when set on getById", async () => {
    const fetchMock = vi.fn().mockResolvedValue(createJsonResponse({}));
    const client = createHttpClient({ baseUrl: "/api", fetchFn: fetchMock });
    const opinionListsApi = createOpinionListsApi(client);

    await opinionListsApi.getById({
      listId: "11111111-1111-1111-1111-111111111111",
      publishedAfter: "2024-06-01T00:00:00.000Z",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/opinion-lists/11111111-1111-1111-1111-111111111111?publishedAfter=2024-06-01T00%3A00%3A00.000Z",
    );
  });

  it("omits publishedAfter query param when not set on getById", async () => {
    const fetchMock = vi.fn().mockResolvedValue(createJsonResponse({}));
    const client = createHttpClient({ baseUrl: "/api", fetchFn: fetchMock });
    const opinionListsApi = createOpinionListsApi(client);

    await opinionListsApi.getById({
      listId: "22222222-2222-2222-2222-222222222222",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/opinion-lists/22222222-2222-2222-2222-222222222222",
    );
  });

  it("uses backend path parameters and query names for opinion lists", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(createJsonResponse({})));
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const opinionListsApi = createOpinionListsApi(client);

    await opinionListsApi.getSummary({
      listId: "11111111-1111-1111-1111-111111111111",
    });
    await opinionListsApi.getById({
      listId: "22222222-2222-2222-2222-222222222222",
    });
    await opinionListsApi.linkOpinion({
      listId: "33333333-3333-3333-3333-333333333333",
      opinionId: "44444444-4444-4444-4444-444444444444",
    });
    await opinionListsApi.rename({
      listId: "55555555-5555-5555-5555-555555555555",
      name: "Trusted cafes",
    });
    await opinionListsApi.setPrivacy({
      listId: "66666666-6666-6666-6666-666666666666",
      privacy: "PRIVATE",
    });
    await opinionListsApi.sync({
      addedListId: "77777777-7777-7777-7777-777777777777",
      existingListId: "88888888-8888-8888-8888-888888888888",
      weight: 0.75,
    });
    await opinionListsApi.unlinkOpinion({
      listId: "99999999-9999-9999-9999-999999999999",
      opinionId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    });
    await opinionListsApi.unsync({
      existingListId: "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      removedListId: "cccccccc-cccc-cccc-cccc-cccccccccccc",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/opinion-lists/11111111-1111-1111-1111-111111111111/summary",
    );
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/opinion-lists/22222222-2222-2222-2222-222222222222",
    );
    expect(fetchMock.mock.calls[2][0]).toBe(
      "/api/opinion-lists/33333333-3333-3333-3333-333333333333/link?opinionId=44444444-4444-4444-4444-444444444444",
    );
    expect(fetchMock.mock.calls[3][0]).toBe(
      "/api/opinion-lists/55555555-5555-5555-5555-555555555555/rename?name=Trusted+cafes",
    );
    expect(fetchMock.mock.calls[4][0]).toBe(
      "/api/opinion-lists/66666666-6666-6666-6666-666666666666/set-privacy?privacy=PRIVATE",
    );
    expect(fetchMock.mock.calls[5][0]).toBe(
      "/api/opinion-lists/88888888-8888-8888-8888-888888888888/sync?addedListId=77777777-7777-7777-7777-777777777777&weight=0.75",
    );
    expect(fetchMock.mock.calls[6][0]).toBe(
      "/api/opinion-lists/99999999-9999-9999-9999-999999999999/unlink?opinionId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    );
    expect(fetchMock.mock.calls[7][0]).toBe(
      "/api/opinion-lists/bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/unsync?removedListId=cccccccc-cccc-cccc-cccc-cccccccccccc",
    );
  });

  it("matches backend access request routes, including mock GET mutations", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(createJsonResponse({})));
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const accessRequestsApi = createAccessRequestsApi(client);

    await accessRequestsApi.getIncoming({
      pageable: { page: 0, size: 20 },
    });
    await accessRequestsApi.acceptIncoming({
      requestId: "11111111-1111-1111-1111-111111111111",
    });
    await accessRequestsApi.declineIncoming({
      requestId: "22222222-2222-2222-2222-222222222222",
    });
    await accessRequestsApi.hideIncoming({
      requestId: "33333333-3333-3333-3333-333333333333",
    });
    await accessRequestsApi.getOutgoing({
      pageable: { page: 1, size: 10 },
    });
    await accessRequestsApi.createOutgoing({
      listId: "44444444-4444-4444-4444-444444444444",
    });
    await accessRequestsApi.cancelOutgoing({
      requestId: "55555555-5555-5555-5555-555555555555",
    });

    expect(fetchMock.mock.calls[0][0]).toBe("/api/access-requests/incoming?page=0&size=20");
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/access-requests/incoming/11111111-1111-1111-1111-111111111111/accept",
    );
    expect(fetchMock.mock.calls[2][0]).toBe(
      "/api/access-requests/incoming/22222222-2222-2222-2222-222222222222/decline",
    );
    expect(fetchMock.mock.calls[3][0]).toBe(
      "/api/access-requests/incoming/33333333-3333-3333-3333-333333333333/hide",
    );
    expect(fetchMock.mock.calls[4][0]).toBe("/api/access-requests/outgoing?page=1&size=10");
    expect(fetchMock.mock.calls[5][0]).toBe(
      "/api/access-requests/outgoing/create/44444444-4444-4444-4444-444444444444",
    );
    expect(fetchMock.mock.calls[6][0]).toBe(
      "/api/access-requests/outgoing/cancel/55555555-5555-5555-5555-555555555555",
    );

    expect(fetchMock.mock.calls[5][1]).toEqual(
      expect.objectContaining({ method: "POST" }),
    );
    expect(fetchMock.mock.calls[6][1]).toEqual(
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("matches backend selector routes and path parameters", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(createJsonResponse({})));
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const selectorsApi = createSelectorsApi(client);

    await selectorsApi.getForUrl({
      pageable: { page: 0, size: 20 },
      url: "https://example.com/cafe",
    });
    await selectorsApi.getForSubject({
      pageable: { page: 1, size: 10 },
      subjectId: "11111111-1111-1111-1111-111111111111",
    });
    await selectorsApi.suggest({
      selector: ".review-card",
      subjectId: "22222222-2222-2222-2222-222222222222",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/selectors/for-url?page=0&size=20&url=https%3A%2F%2Fexample.com%2Fcafe",
    );
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/selectors/for-subject/11111111-1111-1111-1111-111111111111?page=1&size=10",
    );
    expect(fetchMock.mock.calls[2][0]).toBe(
      "/api/selectors/for-subject/22222222-2222-2222-2222-222222222222?selector=.review-card",
    );
  });

  it("matches backend messaging support thread route", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(createJsonResponse({})));
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const messagingApi = createMessagingApi(client);

    await messagingApi.createSupportThread({
      initialMessage: "Selector is outdated",
    });

    expect(fetchMock.mock.calls[0][0]).toBe("/api/messaging/support/threads");
    expect(fetchMock.mock.calls[0][1]).toEqual(
      expect.objectContaining({
        body: JSON.stringify({ initialMessage: "Selector is outdated" }),
        method: "POST",
      }),
    );
  });
});
