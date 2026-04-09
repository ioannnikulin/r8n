import { describe, expect, it, vi } from "vitest";
import { createHttpClient } from "@/lib/http-client";
import { createAccessRequestsApi } from "@/lib/api/access-requests";
import { createAuthApi } from "@/lib/api/auth";
import { createOpinionListsApi } from "@/lib/api/opinion-lists";
import { createOpinionsApi } from "@/lib/api/opinions";
import { createSelectorsApi } from "@/lib/api/selectors";

function createJsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
    status: 200,
  });
}

describe("API modules", () => {
  it("posts login credentials to the auth endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      createJsonResponse({
        accessToken: "access-token",
        expiresInMilliseconds: 1000,
        refreshToken: "refresh-token",
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

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/auth/login",
      expect.objectContaining({
        body: JSON.stringify({
          login: "test",
          password: "1234",
        }),
        method: "POST",
      }),
    );
  });

  it("adds bearer authorization for protected opinions endpoints", async () => {
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
      accessToken: "stub-access-token-123",
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
      accessToken: "stub-access-token-123",
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
      accessToken: "token",
      id: "11111111-1111-1111-1111-111111111111",
    });
    await opinionsApi.create({
      accessToken: "token",
      mark: 5,
      subjectId: "22222222-2222-2222-2222-222222222222",
    });
    await opinionsApi.update({
      accessToken: "token",
      opinionId: "33333333-3333-3333-3333-333333333333",
    });
    await opinionsApi.delete({
      accessToken: "token",
      opinionId: "44444444-4444-4444-4444-444444444444",
    });
    await opinionsApi.unlinkComponent({
      accessToken: "token",
      linkId: "55555555-5555-5555-5555-555555555555",
    });
    await opinionsApi.adjustComponentWeight({
      accessToken: "token",
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
      "/api/opinions/adjustWeight/66666666-6666-6666-6666-666666666666?weight=0.5",
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
  });

  it("uses backend path parameters and query names for opinion lists", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(createJsonResponse({})));
    const client = createHttpClient({
      baseUrl: "/api",
      fetchFn: fetchMock,
    });
    const opinionListsApi = createOpinionListsApi(client);

    await opinionListsApi.getSummary({
      accessToken: "token",
      listId: "11111111-1111-1111-1111-111111111111",
    });
    await opinionListsApi.getById({
      accessToken: "token",
      listId: "22222222-2222-2222-2222-222222222222",
    });
    await opinionListsApi.linkOpinion({
      accessToken: "token",
      listId: "33333333-3333-3333-3333-333333333333",
      opinionId: "44444444-4444-4444-4444-444444444444",
    });
    await opinionListsApi.rename({
      accessToken: "token",
      listId: "55555555-5555-5555-5555-555555555555",
      name: "Trusted cafes",
    });
    await opinionListsApi.setPrivacy({
      accessToken: "token",
      listId: "66666666-6666-6666-6666-666666666666",
      privacy: "PRIVATE",
    });
    await opinionListsApi.sync({
      accessToken: "token",
      addedListId: "77777777-7777-7777-7777-777777777777",
      existingListId: "88888888-8888-8888-8888-888888888888",
    });
    await opinionListsApi.unlinkOpinion({
      accessToken: "token",
      listId: "99999999-9999-9999-9999-999999999999",
      opinionId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    });
    await opinionListsApi.unsync({
      accessToken: "token",
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
      "/api/opinion-lists/88888888-8888-8888-8888-888888888888/sync?addedListId=77777777-7777-7777-7777-777777777777",
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
      accessToken: "token",
      pageable: { page: 0, size: 20 },
    });
    await accessRequestsApi.acceptIncoming({
      accessToken: "token",
      requestId: "11111111-1111-1111-1111-111111111111",
    });
    await accessRequestsApi.declineIncoming({
      accessToken: "token",
      requestId: "22222222-2222-2222-2222-222222222222",
    });
    await accessRequestsApi.hideIncoming({
      accessToken: "token",
      requestId: "33333333-3333-3333-3333-333333333333",
    });
    await accessRequestsApi.getOutgoing({
      accessToken: "token",
      pageable: { page: 1, size: 10 },
    });
    await accessRequestsApi.createOutgoing({
      accessToken: "token",
      listId: "44444444-4444-4444-4444-444444444444",
    });
    await accessRequestsApi.cancelOutgoing({
      accessToken: "token",
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
      expect.objectContaining({ method: "GET" }),
    );
    expect(fetchMock.mock.calls[6][1]).toEqual(
      expect.objectContaining({ method: "GET" }),
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
      accessToken: "token",
      pageable: { page: 0, size: 20 },
      url: "https://example.com/cafe",
    });
    await selectorsApi.getForSubject({
      accessToken: "token",
      pageable: { page: 1, size: 10 },
      subjectId: "11111111-1111-1111-1111-111111111111",
    });
    await selectorsApi.suggest({
      accessToken: "token",
      selector: ".review-card",
      subjectId: "22222222-2222-2222-2222-222222222222",
    });
    await selectorsApi.disagree({
      accessToken: "token",
      comment: "Selector is outdated",
      selectorId: "33333333-3333-3333-3333-333333333333",
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
    expect(fetchMock.mock.calls[3][0]).toBe(
      "/api/selectors/33333333-3333-3333-3333-333333333333/disagree?comment=Selector+is+outdated",
    );
    expect(fetchMock.mock.calls[3][1]).toEqual(
      expect.objectContaining({ method: "POST" }),
    );
  });
});
