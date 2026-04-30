import type { ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { AppSidebar } from "@/components/layout/AppSidebar";
import { SidebarProvider } from "@/components/ui/sidebar";

const { navigateMock } = vi.hoisted(() => ({
  navigateMock: vi.fn(),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom",
  );
  return { ...actual, useNavigate: () => navigateMock };
});

vi.mock("@/lib/api", () => ({
  authApi: { logout: vi.fn() },
}));

vi.mock("@/lib/auth/session", () => ({
  clearSession: vi.fn(),
  configureSessionRefresh: vi.fn(),
  getAccessToken: vi.fn(),
  getSession: vi.fn(),
  hasValidSession: vi.fn(),
  refreshSession: vi.fn(),
  setSession: vi.fn(),
  subscribeSession: vi.fn(() => () => {}),
}));

vi.mock("@/lib/api/users", () => ({
  usersApi: {
    getMe: vi.fn(),
    getUser: vi.fn(),
    getUserAvatar: vi.fn(),
  },
}));

function renderSidebar(queryClient: QueryClient): ReturnType<typeof render> {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/"]}>
        <SidebarProvider defaultOpen>
          <AppSidebar />
        </SidebarProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function makeQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
}

describe("sidebar moderation visibility", () => {
  it("shows the Moderation item for a user with the MODERATOR role", () => {
    const queryClient = makeQueryClient();
    queryClient.setQueryData(["users", "me"], {
      id: "00000000-0000-0000-0000-000000000000",
      name: "Test Testsson",
      roles: ["MODERATOR"],
    });

    renderSidebar(queryClient);

    expect(screen.getByText("Moderation")).toBeInTheDocument();
  });

  it("shows the Moderation item for a user with the SUPPORT role", () => {
    const queryClient = makeQueryClient();
    queryClient.setQueryData(["users", "me"], {
      id: "00000000-0000-0000-0000-000000000001",
      name: "Support User",
      roles: ["SUPPORT"],
    });

    renderSidebar(queryClient);

    expect(screen.getByText("Moderation")).toBeInTheDocument();
  });

  it("hides the Moderation item for a user with only the USER role", () => {
    const queryClient = makeQueryClient();
    queryClient.setQueryData(["users", "me"], {
      id: "20202020-2020-2020-2020-202020202020",
      name: "Anna Müller",
      roles: ["USER"],
    });

    renderSidebar(queryClient);

    expect(screen.queryByText("Moderation")).not.toBeInTheDocument();
  });
});
