import { QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const USER_ID = "11111111-1111-1111-1111-111111111111";
const ACCESS_TOKEN = "stub-access-token-123";

let UserAvatar: typeof import("@/components/UserAvatar")["default"];
let createQueryClient: typeof import("@/lib/server-state/query-client")["createQueryClient"];
let clearAuthSession: typeof import("@/lib/server-state/auth-store")["clearAuthSession"];
let setAuthSession: typeof import("@/lib/server-state/auth-store")["setAuthSession"];
let fetchMock: ReturnType<typeof vi.fn>;

function createJsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
    status: 200,
  });
}

describe("UserAvatar", () => {
  beforeEach(async () => {
    vi.resetModules();

    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    ({ default: UserAvatar } = await import("@/components/UserAvatar"));
    ({ createQueryClient } = await import("@/lib/server-state/query-client"));
    ({ clearAuthSession, setAuthSession } = await import("@/lib/server-state/auth-store"));

    setAuthSession({
      accessToken: ACCESS_TOKEN,
      expiresInMilliseconds: 60_000,
    });

    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: vi.fn(),
    });
    Object.defineProperty(URL, "revokeObjectURL", {
      configurable: true,
      value: vi.fn(),
    });
    vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:avatar-url");
    vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => {});
  });

  afterEach(() => {
    clearAuthSession();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("shows initials when the user has no avatar", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));

    renderWithClient(<UserAvatar userId={USER_ID} name="Jane Doe" />);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/users/${USER_ID}/avatar`,
        expect.objectContaining({ method: "GET" }),
      );
    });
    expect(screen.getByText("JD")).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("renders the avatar image when the backend returns a blob", async () => {
    fetchMock.mockResolvedValueOnce(
      new Response("avatar-bytes", {
        headers: { "Content-Type": "image/png" },
        status: 200,
      }),
    );

    renderWithClient(<UserAvatar userId={USER_ID} name="Jane Doe" />);

    expect(await screen.findByRole("img", { name: "Jane Doe" })).toHaveAttribute(
      "src",
      "blob:avatar-url",
    );
    const [avatarBlob] = vi.mocked(URL.createObjectURL).mock.calls[0];
    expect(avatarBlob.type).toBe("image/png");
    expect(await avatarBlob.text()).toBe("avatar-bytes");

    const [, requestInit] = fetchMock.mock.calls[0];
    const headers = new Headers(requestInit.headers);
    expect(headers.get("Authorization")).toBe(`Bearer ${ACCESS_TOKEN}`);
    expect(headers.get("Accept")).toBe("image/*");
  });

  it("loads and shows last seen information when hovering the avatar", async () => {
    const lastSeenAt = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString();
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(
        createJsonResponse({
          id: USER_ID,
          name: "Jane Doe",
          status: "ACTIVE",
          lastSeenAt,
          about: null,
          location: null,
        }),
      );

    renderWithClient(<UserAvatar userId={USER_ID} name="Jane Doe" />);

    const trigger = await screen.findByRole("button", { name: "Jane Doe presence" });
    fireEvent.pointerEnter(trigger, { pointerType: "mouse" });
    fireEvent.mouseEnter(trigger);

    expect(await screen.findByText("Last seen 2 hours ago")).toBeInTheDocument();
    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        `/api/users/${USER_ID}`,
        expect.objectContaining({ method: "GET" }),
      );
    });
  });

  it("shows provided last seen information without a user id", async () => {
    const lastSeenAt = new Date(Date.now() - 15 * 60 * 1000).toISOString();

    renderWithClient(<UserAvatar name="Alex Krüger" lastSeenAt={lastSeenAt} />);

    const trigger = screen.getByRole("button", { name: "Alex Krüger presence" });
    fireEvent.pointerEnter(trigger, { pointerType: "mouse" });
    fireEvent.mouseEnter(trigger);

    expect(await screen.findByText("Last seen 15 minutes ago")).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

function renderWithClient(ui: ReactElement) {
  const queryClient = createQueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>,
  );
}
