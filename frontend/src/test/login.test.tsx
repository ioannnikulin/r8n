import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Login from "@/pages/Login";

const {
  clearSessionMock,
  getAccessTokenMock,
  getSessionMock,
  loginMock,
  navigateMock,
  registerMock,
  setSessionMock,
  subscribeSessionMock,
} = vi.hoisted(() => ({
  clearSessionMock: vi.fn(),
  getAccessTokenMock: vi.fn(),
  getSessionMock: vi.fn(),
  loginMock: vi.fn(),
  navigateMock: vi.fn(),
  registerMock: vi.fn(),
  setSessionMock: vi.fn(),
  subscribeSessionMock: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom",
  );

  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

vi.mock("@/lib/api", () => ({
  authApi: {
    login: loginMock,
    register: registerMock,
  },
}));

vi.mock("@/lib/auth/session", () => ({
  clearSession: clearSessionMock,
  getAccessToken: getAccessTokenMock,
  getSession: getSessionMock,
  setSession: setSessionMock,
  subscribeSession: subscribeSessionMock,
}));

function renderLoginPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      mutations: {
        retry: false,
      },
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/login"]}>
        <Routes>
          <Route path="/login" element={<Login />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("Login page", () => {
  beforeEach(() => {
    loginMock.mockReset();
    registerMock.mockReset();
    navigateMock.mockReset();
    clearSessionMock.mockReset();
    getAccessTokenMock.mockReset();
    getSessionMock.mockReset();
    setSessionMock.mockReset();
    subscribeSessionMock.mockReset();
    getAccessTokenMock.mockReturnValue(null);
    getSessionMock.mockReturnValue(null);
    subscribeSessionMock.mockImplementation(() => () => {});
  });

  it("stores the login session in memory before redirecting to the dashboard", async () => {
    loginMock.mockResolvedValue({
      accessToken: "stub-access-token-123",
      expiresInMilliseconds: 60_000,
    });

    renderLoginPage();

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "test@test.test" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "1234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith({
        login: "test@test.test",
        password: "1234",
      });
    });

    await waitFor(() => {
      expect(setSessionMock).toHaveBeenCalledWith({
        accessToken: "stub-access-token-123",
        expiresInMilliseconds: 60_000,
      });
      expect(navigateMock).toHaveBeenCalledWith("/", { replace: true });
    });

    expect(setSessionMock.mock.invocationCallOrder[0]).toBeLessThan(
      navigateMock.mock.invocationCallOrder[0],
    );
  });

  it("shows the seeded local development credentials", () => {
    renderLoginPage();

    expect(screen.getByText("test@test.test / 1234")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("test@test.test")).toBeInTheDocument();
  });

  it("registers a new account, signs in, and redirects to the dashboard", async () => {
    registerMock.mockResolvedValue(undefined);
    loginMock.mockResolvedValue({
      accessToken: "registered-access-token-123",
      expiresInMilliseconds: 60_000,
    });

    renderLoginPage();

    fireEvent.click(screen.getByRole("button", { name: "Create one" }));
    fireEvent.change(screen.getByLabelText("Display name"), {
      target: { value: "New Reviewer" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "new-user@test.test" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "long-enough-password" },
    });
    fireEvent.change(screen.getByLabelText("Confirm password"), {
      target: { value: "long-enough-password" },
    });
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    await waitFor(() => {
      expect(registerMock).toHaveBeenCalledWith({
        name: "New Reviewer",
        email: "new-user@test.test",
        password: "long-enough-password",
        privacyPolicyAccepted: true,
        termsOfServiceAccepted: true,
      });
    });

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith({
        login: "new-user@test.test",
        password: "long-enough-password",
      });
      expect(setSessionMock).toHaveBeenCalledWith({
        accessToken: "registered-access-token-123",
        expiresInMilliseconds: 60_000,
      });
      expect(navigateMock).toHaveBeenCalledWith("/", { replace: true });
    });
  });

  it("shows an inline error when registration name is empty", async () => {
    renderLoginPage();

    fireEvent.click(screen.getByRole("button", { name: "Create one" }));
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "new-user@test.test" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "long-enough-password" },
    });
    fireEvent.change(screen.getByLabelText("Confirm password"), {
      target: { value: "long-enough-password" },
    });
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Enter your display name.")).toBeInTheDocument();
    expect(screen.getByLabelText("Display name")).toHaveAttribute("aria-invalid", "true");
    expect(loginMock).not.toHaveBeenCalled();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it("shows an inline error when login email is empty", async () => {
    renderLoginPage();

    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "1234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Enter your email address.")).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
    expect(loginMock).not.toHaveBeenCalled();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it("shows an inline error when login email is invalid", async () => {
    renderLoginPage();

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "not-an-email" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "1234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Enter a valid email address.")).toBeInTheDocument();
    expect(loginMock).not.toHaveBeenCalled();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it("shows an inline error when registration password is too short", async () => {
    renderLoginPage();

    fireEvent.click(screen.getByRole("button", { name: "Create one" }));
    fireEvent.change(screen.getByLabelText("Display name"), {
      target: { value: "New Reviewer" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "new-user@test.test" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "short" },
    });
    fireEvent.change(screen.getByLabelText("Confirm password"), {
      target: { value: "short" },
    });
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Password must be at least 12 characters.")).toBeInTheDocument();
    expect(loginMock).not.toHaveBeenCalled();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it("shows an inline error when registration passwords do not match", async () => {
    renderLoginPage();

    fireEvent.click(screen.getByRole("button", { name: "Create one" }));
    fireEvent.change(screen.getByLabelText("Display name"), {
      target: { value: "New Reviewer" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "new-user@test.test" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "long-enough-password" },
    });
    fireEvent.change(screen.getByLabelText("Confirm password"), {
      target: { value: "different-password" },
    });
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Passwords do not match.")).toBeInTheDocument();
    expect(screen.getByLabelText("Confirm password")).toHaveAttribute("aria-invalid", "true");
    expect(loginMock).not.toHaveBeenCalled();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it("shows an inline error when registration consent is unchecked", async () => {
    renderLoginPage();

    fireEvent.click(screen.getByRole("button", { name: "Create one" }));
    fireEvent.change(screen.getByLabelText("Display name"), {
      target: { value: "New Reviewer" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "new-user@test.test" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "long-enough-password" },
    });
    fireEvent.change(screen.getByLabelText("Confirm password"), {
      target: { value: "long-enough-password" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(
      await screen.findByText("Accept the Privacy Policy and Terms of Service."),
    ).toBeInTheDocument();
    expect(screen.getByRole("checkbox")).toHaveAttribute("aria-invalid", "true");
    expect(loginMock).not.toHaveBeenCalled();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it("clears form values and validation errors when switching auth modes", async () => {
    renderLoginPage();

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "invalid-email" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "1234" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Enter a valid email address.")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Create one" }));

    expect(screen.getByLabelText("Display name")).toHaveValue("");
    expect(screen.getByLabelText("Email")).toHaveValue("");
    expect(screen.getByLabelText("Password")).toHaveValue("");
    expect(screen.getByLabelText("Confirm password")).toHaveValue("");
    expect(screen.getByRole("checkbox")).not.toBeChecked();
    expect(screen.queryByText("Enter a valid email address.")).not.toBeInTheDocument();
  });
});
