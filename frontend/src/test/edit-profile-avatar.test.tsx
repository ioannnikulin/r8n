import { QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const USER_ID = "00000000-0000-0000-0000-000000000000";
const ACCESS_TOKEN = "stub-access-token-123";
const AVATAR_MAX_SIZE_BYTES = 1024;
const PROFILE_NAME_MAX_LENGTH = 255;
const PROFILE_ABOUT_MAX_LENGTH = 255;
const PROFILE_LOCATION_MAX_LENGTH = 255;

const { toastMock } = vi.hoisted(() => ({
  toastMock: vi.fn(),
}));

vi.mock("@/hooks/use-toast", () => ({
  toast: toastMock,
}));

let EditProfile: typeof import("@/pages/EditProfile")["default"];
let createQueryClient: typeof import("@/lib/server-state/query-client")["createQueryClient"];
let clearAuthSession: typeof import("@/lib/server-state/auth-store")["clearAuthSession"];
let setAuthSession: typeof import("@/lib/server-state/auth-store")["setAuthSession"];
let fetchMock: ReturnType<typeof vi.fn>;

function createUserResponse() {
  return new Response(
    JSON.stringify({
      id: USER_ID,
      name: "Test Testsson",
    }),
    {
      headers: { "Content-Type": "application/json" },
      status: 200,
    },
  );
}

function createProfileResponse(
  overrides: Partial<{
    about: string | null;
    location: string | null;
    name: string;
  }> = {},
) {
  return new Response(
    JSON.stringify({
      about: overrides.about ?? "I am a coffee expert",
      id: USER_ID,
      lastSeenAt: null,
      location: overrides.location ?? "Berlin, Germany",
      name: overrides.name ?? "Test Testsson",
      status: "ACTIVE",
    }),
    {
      headers: { "Content-Type": "application/json" },
      status: 200,
    },
  );
}

function createAvatarResponse() {
  return new Response("avatar-bytes", {
    headers: { "Content-Type": "image/png" },
    status: 200,
  });
}

function createEmptyResponse() {
  return new Response(null, { status: 204 });
}

function mockInitialEditProfileRequests() {
  fetchMock
    .mockResolvedValueOnce(createUserResponse())
    .mockResolvedValueOnce(new Response(null, { status: 204 }))
    .mockResolvedValueOnce(createProfileResponse());
}

function renderEditProfile() {
  const renderResult = renderWithClient(
    <MemoryRouter>
      <EditProfile />
    </MemoryRouter>,
  );

  const fileInput = renderResult.container.querySelector(
    'input[type="file"]',
  ) as HTMLInputElement | null;

  if (!fileInput) {
    throw new Error("Expected profile image file input to be rendered.");
  }

  return {
    ...renderResult,
    fileInput,
  };
}

describe("EditProfile avatar controls", () => {
  beforeEach(async () => {
    vi.resetModules();

    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
    vi.stubEnv("VITE_AVATAR_MAX_SIZE_BYTES", String(AVATAR_MAX_SIZE_BYTES));
    vi.stubEnv("VITE_PROFILE_NAME_MAX_LENGTH", String(PROFILE_NAME_MAX_LENGTH));
    vi.stubEnv("VITE_PROFILE_ABOUT_MAX_LENGTH", String(PROFILE_ABOUT_MAX_LENGTH));
    vi.stubEnv("VITE_PROFILE_LOCATION_MAX_LENGTH", String(PROFILE_LOCATION_MAX_LENGTH));

    ({ default: EditProfile } = await import("@/pages/EditProfile"));
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
    toastMock.mockReset();
  });

  afterEach(() => {
    clearAuthSession();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  it("shows initials from the current user name when there is no avatar", async () => {
    fetchMock
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(createProfileResponse());

    renderEditProfile();

    expect(await screen.findByText("TT")).toBeInTheDocument();
    expect(screen.queryByText("JD")).not.toBeInTheDocument();
  });

  it("uploads a valid selected image file", async () => {
    fetchMock
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(createProfileResponse())
      .mockResolvedValueOnce(createEmptyResponse())
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(createProfileResponse())
      .mockResolvedValueOnce(createAvatarResponse());
    const { fileInput } = renderEditProfile();
    const file = new File(["avatar"], "avatar.png", { type: "image/png" });

    await waitForInitialAvatarFetch();
    fireEvent.change(fileInput, { target: { files: [file] } });

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/users/me/avatar",
        expect.objectContaining({ method: "POST" }),
      );
    });

    const uploadRequest = fetchMock.mock.calls.find(
      ([url, requestInit]) =>
        url === "/api/users/me/avatar" && requestInit.method === "POST",
    )?.[1];
    expect(uploadRequest?.body).toBeInstanceOf(FormData);

    const uploadHeaders = new Headers(uploadRequest?.headers);
    expect(uploadHeaders.get("Authorization")).toBe(`Bearer ${ACCESS_TOKEN}`);
    expect(uploadHeaders.get("Content-Type")).toBeNull();
    await waitFor(() => {
      expect(toastMock).toHaveBeenCalledWith({
        title: "Profile image updated",
        description: "Your new image has been saved.",
      });
    });
  });

  it("rejects unsupported file types before upload", async () => {
    fetchMock
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(createProfileResponse());
    const { fileInput } = renderEditProfile();
    const file = new File(["not-image"], "avatar.txt", { type: "text/plain" });

    await waitForInitialAvatarFetch();
    fireEvent.change(fileInput, { target: { files: [file] } });

    expect(fetchMock).not.toHaveBeenCalledWith(
      "/api/users/me/avatar",
      expect.objectContaining({ method: "POST" }),
    );
    expect(toastMock).toHaveBeenCalledWith({
      title: "Image not uploaded",
      description: "Please choose a PNG, JPEG, or WebP image.",
    });
  });

  it("rejects images larger than the configured size before upload", async () => {
    fetchMock
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(createProfileResponse());
    const { fileInput } = renderEditProfile();
    const file = new File(
      [new Uint8Array(AVATAR_MAX_SIZE_BYTES + 1)],
      "avatar.png",
      { type: "image/png" },
    );

    await waitForInitialAvatarFetch();
    fireEvent.change(fileInput, { target: { files: [file] } });

    expect(screen.getByText("PNG, JPEG, or WebP. Max 1KB.")).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(
      "/api/users/me/avatar",
      expect.objectContaining({ method: "POST" }),
    );
    expect(toastMock).toHaveBeenCalledWith({
      title: "Image not uploaded",
      description: "Profile image must be 1KB or smaller.",
    });
  });

  it("deletes the current profile image", async () => {
    fetchMock
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(createAvatarResponse())
      .mockResolvedValueOnce(createProfileResponse())
      .mockResolvedValueOnce(createEmptyResponse())
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(createProfileResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    renderEditProfile();

    await waitForInitialAvatarFetch();
    fireEvent.click(await screen.findByRole("button", { name: /remove image/i }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/users/me/avatar",
        expect.objectContaining({ method: "DELETE" }),
      );
    });
    const deleteRequest = fetchMock.mock.calls.find(
      ([url, requestInit]) =>
        url === "/api/users/me/avatar" && requestInit.method === "DELETE",
    )?.[1];
    const deleteHeaders = new Headers(deleteRequest?.headers);
    expect(deleteHeaders.get("Authorization")).toBe(`Bearer ${ACCESS_TOKEN}`);
    expect(toastMock).toHaveBeenCalledWith({
      title: "Profile image removed",
      description: "Your profile now shows initials again.",
    });
  });

  it("updates public profile text fields", async () => {
    fetchMock
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(createProfileResponse())
      .mockResolvedValueOnce(
        createProfileResponse({
          about: "Updated public bio",
          location: "Hamburg, Germany",
          name: "Updated Testsson",
        }),
      )
      .mockResolvedValueOnce(createUserResponse())
      .mockResolvedValueOnce(
        createProfileResponse({
          about: "Updated public bio",
          location: "Hamburg, Germany",
          name: "Updated Testsson",
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    renderEditProfile();

    fireEvent.change(await screen.findByLabelText("Display name"), {
      target: { value: "Updated Testsson" },
    });
    fireEvent.change(screen.getByLabelText("Bio"), {
      target: { value: "Updated public bio" },
    });
    fireEvent.change(screen.getByLabelText("Location"), {
      target: { value: "Hamburg, Germany" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save Profile" }));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/users/me/public-profile",
        expect.objectContaining({
          body: JSON.stringify({
            about: "Updated public bio",
            location: "Hamburg, Germany",
            name: "Updated Testsson",
          }),
          method: "PATCH",
        }),
      );
    });
    expect(toastMock).toHaveBeenCalledWith({
      title: "Profile updated",
      description: "Your changes have been saved.",
    });
  });

  it("sets frontend length limits matching the profile update API", async () => {
    mockInitialEditProfileRequests();
    renderEditProfile();

    expect(await screen.findByLabelText("Display name")).toHaveAttribute(
      "maxLength",
      String(PROFILE_NAME_MAX_LENGTH),
    );
    expect(screen.getByLabelText("Bio")).toHaveAttribute(
      "maxLength",
      String(PROFILE_ABOUT_MAX_LENGTH),
    );
    expect(screen.getByLabelText("Location")).toHaveAttribute(
      "maxLength",
      String(PROFILE_LOCATION_MAX_LENGTH),
    );
  });

  it("rejects blank display name before updating public profile", async () => {
    mockInitialEditProfileRequests();
    renderEditProfile();

    await waitForProfileForm();
    fireEvent.change(screen.getByLabelText("Display name"), {
      target: { value: "   " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save Profile" }));

    expect(fetchMock).not.toHaveBeenCalledWith(
      "/api/users/me/public-profile",
      expect.objectContaining({ method: "PATCH" }),
    );
    expect(toastMock).toHaveBeenCalledWith({
      title: "Name required",
      description: "Please enter your display name.",
    });
  });

  it.each([
    {
      description: `Display name must be ${PROFILE_NAME_MAX_LENGTH} characters or fewer.`,
      label: "Display name",
      value: "a".repeat(PROFILE_NAME_MAX_LENGTH + 1),
    },
    {
      description: `Bio must be ${PROFILE_ABOUT_MAX_LENGTH} characters or fewer.`,
      label: "Bio",
      value: "a".repeat(PROFILE_ABOUT_MAX_LENGTH + 1),
    },
    {
      description: `Location must be ${PROFILE_LOCATION_MAX_LENGTH} characters or fewer.`,
      label: "Location",
      value: "a".repeat(PROFILE_LOCATION_MAX_LENGTH + 1),
    },
  ])("rejects $label longer than the server limit", async ({ description, label, value }) => {
    mockInitialEditProfileRequests();
    renderEditProfile();

    await waitForProfileForm();
    fireEvent.change(screen.getByLabelText(label), {
      target: { value },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save Profile" }));

    expect(fetchMock).not.toHaveBeenCalledWith(
      "/api/users/me/public-profile",
      expect.objectContaining({ method: "PATCH" }),
    );
    expect(toastMock).toHaveBeenCalledWith({
      title: "Profile not saved",
      description,
    });
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

async function waitForInitialAvatarFetch() {
  await waitFor(() => {
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/users/${USER_ID}/avatar`,
      expect.objectContaining({ method: "GET" }),
    );
  });
}

async function waitForProfileForm() {
  await waitFor(() => {
    expect(screen.getByLabelText("Display name")).toHaveValue("Test Testsson");
  });
}
