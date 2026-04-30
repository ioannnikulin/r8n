import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Requests from "@/pages/Requests";
import type { AccessRequestDto } from "@/lib/api/access-requests";

const REQUESTER_ID = "44444444-4444-4444-4444-444444444444";
const OWNER_ID = "33333333-3333-3333-3333-333333333333";
const LIST_ID = "22222222-2222-2222-2222-222222222222";

const {
  acceptMutate,
  cancelMutate,
  declineMutate,
  hideMutate,
  outgoingItemsRef,
  approvedIncomingItemsRef,
} = vi.hoisted(() => ({
  acceptMutate: vi.fn(),
  cancelMutate: vi.fn(),
  declineMutate: vi.fn(),
  hideMutate: vi.fn(),
  outgoingItemsRef: { current: [] as AccessRequestDto[] },
  approvedIncomingItemsRef: { current: [] as AccessRequestDto[] },
}));

vi.mock("@/components/UserAvatar", () => ({
  default: ({ name }: { name: string }) => <span>{name}</span>,
}));

vi.mock("@/lib/server-state/hooks/access-requests", () => ({
  useIncomingAccessRequests: vi.fn((request) => {
    if (request.filters?.status === "ACCEPTED") {
      return {
        data: { items: approvedIncomingItemsRef.current, total: approvedIncomingItemsRef.current.length },
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
    data: { items: outgoingItemsRef.current, total: outgoingItemsRef.current.length },
    error: null,
    isError: false,
    isLoading: false,
    refetch: vi.fn(),
  })),
  useAcceptIncomingAccessRequestMutation: vi.fn(() => ({ isPending: false, mutate: acceptMutate })),
  useDeclineIncomingAccessRequestMutation: vi.fn(() => ({ isPending: false, mutate: declineMutate })),
  useHideIncomingAccessRequestMutation: vi.fn(() => ({ isPending: false, mutate: hideMutate })),
  useCancelOutgoingAccessRequestMutation: vi.fn(() => ({ isPending: false, mutate: cancelMutate })),
}));

const buildRow = (overrides: Partial<AccessRequestDto> = {}): AccessRequestDto => ({
  id: "11111111-1111-1111-1111-111111111111",
  opinionListId: LIST_ID,
  opinionListName: "Private Cafe List",
  owner: OWNER_ID,
  ownerName: "Owner User",
  requester: REQUESTER_ID,
  requesterName: "Requester User",
  status: "ACCEPTED",
  timestamp: "2026-04-29T08:00:00.000Z",
  ...overrides,
});

beforeEach(() => {
  outgoingItemsRef.current = [];
  approvedIncomingItemsRef.current = [];
  acceptMutate.mockReset();
  cancelMutate.mockReset();
  declineMutate.mockReset();
  hideMutate.mockReset();
});

describe("Requests page", () => {
  it("lets an owner revoke an approved incoming access request", () => {
    approvedIncomingItemsRef.current = [
      buildRow({
        id: "55555555-5555-5555-5555-555555555555",
        status: "ACCEPTED",
      }),
    ];

    render(<Requests />);

    fireEvent.click(screen.getByRole("button", { name: /show access granted/i }));
    fireEvent.click(screen.getByRole("button", { name: /revoke/i }));

    expect(declineMutate).toHaveBeenCalledWith({
      requestId: "55555555-5555-5555-5555-555555555555",
    });
  });

  it("shows an accepted outgoing COPY request without executing client-side copy", () => {
    const row = buildRow({ id: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", intent: "COPY" });
    outgoingItemsRef.current = [row];

    render(<Requests />);

    expect(screen.getByText("Approved")).toBeInTheDocument();
    expect(screen.getByText("Private Cafe List")).toBeInTheDocument();
  });

  it("shows an accepted outgoing MERGE request without executing client-side sync", () => {
    const targetList = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    const row = buildRow({
      id: "cccccccc-cccc-cccc-cccc-cccccccccccc",
      intent: "MERGE",
      targetListId: targetList,
    });
    outgoingItemsRef.current = [row];

    render(<Requests />);

    expect(screen.getByText("Approved")).toBeInTheDocument();
    expect(screen.getByText("Private Cafe List")).toBeInTheDocument();
  });
});
