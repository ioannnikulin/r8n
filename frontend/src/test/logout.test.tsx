import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppSidebar } from "@/components/layout/AppSidebar";
import { SidebarProvider } from "@/components/ui/sidebar";
import { useLogoutMutation } from "@/lib/server-state";
import type { ThreadSummaryDto } from "@/lib/api/messages";

const {
  clearSessionMock,
  logoutMock,
  messageThreads,
  navigateMock,
} = vi.hoisted(() => ({
  clearSessionMock: vi.fn(),
  logoutMock: vi.fn(),
  messageThreads: [
    {
      createdAt: "2026-01-01T09:00:00Z",
      id: "thread-one",
      lastMessageOwn: false,
      lastMessagePreview: "Hello",
      participant: {
        name: "Marta",
        role: "USER",
        userId: "11111111-1111-1111-1111-111111111111",
      },
      type: "DIRECT",
      unreadCount: 2,
      updatedAt: "2026-01-01T10:00:00Z",
    },
    {
      createdAt: "2026-01-01T09:00:00Z",
      id: "thread-two",
      lastMessageOwn: false,
      lastMessagePreview: "Support",
      participant: {
        name: "R8N Support",
        role: "SUPPORT",
        userId: null,
      },
      type: "SUPPORT",
      unreadCount: 1,
      updatedAt: "2026-01-01T10:00:00Z",
    },
  ] satisfies ThreadSummaryDto[],
  navigateMock: vi.fn(),
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
    logout: logoutMock,
  },
}));

vi.mock("@/lib/auth/session", () => ({
  clearSession: clearSessionMock,
  configureSessionRefresh: vi.fn(),
  getAccessToken: vi.fn(),
  getSession: vi.fn(),
  hasValidSession: vi.fn(),
  refreshSession: vi.fn(),
  setSession: vi.fn(),
  subscribeSession: vi.fn(() => () => {}),
}));

vi.mock("@/lib/server-state/hooks/messages", () => ({
  useMessageThreads: () => ({
    data: {
      items: messageThreads,
    },
  }),
}));

function renderWithQueryClient(ui: ReactNode, queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>,
  );
}

describe("logout", () => {
  beforeEach(() => {
    clearSessionMock.mockReset();
    logoutMock.mockReset();
    navigateMock.mockReset();
  });

  it("calls the backend logout endpoint and clears cached auth state", async () => {
    logoutMock.mockResolvedValue(undefined);
    const queryClient = new QueryClient({
      defaultOptions: {
        mutations: { retry: false },
        queries: { retry: false },
      },
    });
    queryClient.setQueryData(["private-data"], "secret");

    function LogoutHarness() {
      const logoutMutation = useLogoutMutation();

      return (
        <button onClick={() => logoutMutation.mutate()}>
          Log out
        </button>
      );
    }

    renderWithQueryClient(<LogoutHarness />, queryClient);

    fireEvent.click(screen.getByRole("button", { name: "Log out" }));

    await waitFor(() => {
      expect(logoutMock).toHaveBeenCalledTimes(1);
    });

    expect(clearSessionMock).toHaveBeenCalledTimes(1);
    expect(queryClient.getQueryData(["private-data"])).toBeUndefined();
  });

  it("clears local auth state even if the backend logout request fails", async () => {
    logoutMock.mockRejectedValue(new Error("Network failed"));
    const queryClient = new QueryClient({
      defaultOptions: {
        mutations: { retry: false },
        queries: { retry: false },
      },
    });
    queryClient.setQueryData(["private-data"], "secret");

    function LogoutHarness() {
      const logoutMutation = useLogoutMutation();

      return (
        <button onClick={() => logoutMutation.mutate()}>
          Log out
        </button>
      );
    }

    renderWithQueryClient(<LogoutHarness />, queryClient);

    fireEvent.click(screen.getByRole("button", { name: "Log out" }));

    await waitFor(() => {
      expect(logoutMock).toHaveBeenCalledTimes(1);
    });

    expect(clearSessionMock).toHaveBeenCalledTimes(1);
    expect(queryClient.getQueryData(["private-data"])).toBeUndefined();
  });

  it("routes the sidebar logout button through the shared logout mutation and redirects to login", async () => {
    logoutMock.mockResolvedValue(undefined);
    const queryClient = new QueryClient({
      defaultOptions: {
        mutations: { retry: false },
        queries: { retry: false },
      },
    });

    renderWithQueryClient(
      <MemoryRouter initialEntries={["/"]}>
        <SidebarProvider defaultOpen>
          <AppSidebar />
        </SidebarProvider>
      </MemoryRouter>,
      queryClient,
    );

    fireEvent.click(screen.getByTitle("Log out"));

    await waitFor(() => {
      expect(logoutMock).toHaveBeenCalledTimes(1);
    });

    expect(clearSessionMock).toHaveBeenCalledTimes(1);
    expect(navigateMock).toHaveBeenCalledWith("/login", { replace: true });
  });

  it("shows the messages unread badge from messaging server state", () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        mutations: { retry: false },
        queries: { retry: false },
      },
    });

    renderWithQueryClient(
      <MemoryRouter initialEntries={["/"]}>
        <SidebarProvider defaultOpen>
          <AppSidebar />
        </SidebarProvider>
      </MemoryRouter>,
      queryClient,
    );

    expect(screen.getByText("Messages")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Messages3" })).toBeInTheDocument();
  });
});
