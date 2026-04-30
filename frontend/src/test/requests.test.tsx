import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Requests from "@/pages/Requests";

const {
  acceptMutate,
  cancelMutate,
  declineMutate,
  hideMutate,
} = vi.hoisted(() => ({
  acceptMutate: vi.fn(),
  cancelMutate: vi.fn(),
  declineMutate: vi.fn(),
  hideMutate: vi.fn(),
}));

vi.mock("@/components/UserAvatar", () => ({
  default: ({ name }: { name: string }) => <span>{name}</span>,
}));

vi.mock("@/lib/server-state/hooks/access-requests", () => ({
  useIncomingAccessRequests: vi.fn((request) => {
    if (request.filters?.status === "ACCEPTED") {
      return {
        data: {
          items: [
            {
              id: "11111111-1111-1111-1111-111111111111",
              opinionListId: "22222222-2222-2222-2222-222222222222",
              opinionListName: "Private Cafe List",
              owner: "33333333-3333-3333-3333-333333333333",
              ownerName: "Owner User",
              requester: "44444444-4444-4444-4444-444444444444",
              requesterName: "Requester User",
              status: "ACCEPTED",
              timestamp: "2026-04-29T08:00:00.000Z",
            },
          ],
          total: 1,
        },
        error: null,
        isError: false,
        isLoading: false,
        refetch: vi.fn(),
      };
    }

    return {
      data: { items: [], total: 0 },
      error: null,
      isError: false,
      isLoading: false,
      refetch: vi.fn(),
    };
  }),
  useOutgoingAccessRequests: vi.fn(() => ({
    data: { items: [], total: 0 },
    error: null,
    isError: false,
    isLoading: false,
    refetch: vi.fn(),
  })),
  useAcceptIncomingAccessRequestMutation: vi.fn(() => ({
    isPending: false,
    mutate: acceptMutate,
  })),
  useDeclineIncomingAccessRequestMutation: vi.fn(() => ({
    isPending: false,
    mutate: declineMutate,
  })),
  useHideIncomingAccessRequestMutation: vi.fn(() => ({
    isPending: false,
    mutate: hideMutate,
  })),
  useCancelOutgoingAccessRequestMutation: vi.fn(() => ({
    isPending: false,
    mutate: cancelMutate,
  })),
}));

describe("Requests page", () => {
  it("lets an owner revoke an approved incoming access request", () => {
    render(<Requests />);

    fireEvent.click(screen.getByRole("button", { name: /show access granted/i }));
    fireEvent.click(screen.getByRole("button", { name: /revoke/i }));

    expect(declineMutate).toHaveBeenCalledWith({
      requestId: "11111111-1111-1111-1111-111111111111",
    });
  });
});
