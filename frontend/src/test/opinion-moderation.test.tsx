import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import OpinionModeration from "@/pages/OpinionModeration";
import type { ModerationDecisionDto, OpinionDto } from "@/lib/api/opinions";
import type { PageResponseDto } from "@/lib/api/shared";
import {
  useApproveOpinionMutation,
  useModerationDecisions,
  useModerationOpinions,
  useRejectOpinionMutation,
} from "@/lib/server-state/hooks/opinions";

vi.mock("@/lib/server-state/hooks/opinions", () => ({
  useApproveOpinionMutation: vi.fn(),
  useModerationDecisions: vi.fn(),
  useModerationOpinions: vi.fn(),
  useRejectOpinionMutation: vi.fn(),
}));

const mockUseModerationOpinions = vi.mocked(useModerationOpinions);
const mockUseModerationDecisions = vi.mocked(useModerationDecisions);
const mockUseApproveOpinionMutation = vi.mocked(useApproveOpinionMutation);
const mockUseRejectOpinionMutation = vi.mocked(useRejectOpinionMutation);

const approveMutateAsync = vi.fn();
const rejectMutateAsync = vi.fn();
const refetch = vi.fn();

const pendingOpinion: OpinionDto = {
  componentMark: null,
  components: [],
  id: "opinion-1",
  mark: 8.5,
  objective: ["Receipt from 2026-04-12", "Paid 3.20 EUR"],
  owner: "user-1",
  ownerName: "Alex Reviewer",
  status: "PENDING_PREMODERATION",
  subject: "subject-1",
  subjective: ["Consistent coffee quality", "Mentions a staff member by name"],
  subjectName: "Espresso Lab Mitte",
  timestamp: new Date(Date.now() - 10 * 60_000).toISOString(),
};

const rejectedDecision: ModerationDecisionDto = {
  action: "REJECTED",
  createdAt: new Date(Date.now() - 5 * 60_000).toISOString(),
  id: "decision-1",
  moderatorId: "moderator-1",
  moderatorName: "Morgan Moderator",
  newStatus: "REJECTED",
  opinionId: "opinion-2",
  ownerName: "Rejected Reviewer",
  previousStatus: "PENDING_PREMODERATION",
  reason: "Needs factual support.",
  subjectName: "Rejected Espresso",
};

function pageResponse<TItem>(items: TItem[]): PageResponseDto<TItem> {
  return {
    items,
    page: 0,
    size: 50,
    total: items.length,
  };
}

function mockModerationQuery(overrides: Record<string, unknown> = {}) {
  mockUseModerationOpinions.mockReturnValue({
    data: pageResponse([pendingOpinion]),
    error: null,
    isError: false,
    isLoading: false,
    refetch,
    ...overrides,
  } as ReturnType<typeof useModerationOpinions>);
}

function mockDecisionQuery(overrides: Record<string, unknown> = {}) {
  mockUseModerationDecisions.mockReturnValue({
    data: pageResponse([rejectedDecision]),
    error: null,
    isError: false,
    isLoading: false,
    refetch,
    ...overrides,
  } as ReturnType<typeof useModerationDecisions>);
}

describe("OpinionModeration", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    mockModerationQuery();
    mockDecisionQuery();
    mockUseApproveOpinionMutation.mockReturnValue({
      isPending: false,
      mutateAsync: approveMutateAsync,
    } as ReturnType<typeof useApproveOpinionMutation>);
    mockUseRejectOpinionMutation.mockReturnValue({
      isPending: false,
      mutateAsync: rejectMutateAsync,
    } as ReturnType<typeof useRejectOpinionMutation>);
  });

  it("loads the pending moderation queue from server state", () => {
    render(<OpinionModeration />);

    expect(mockUseModerationOpinions).toHaveBeenCalledWith({
      pageable: {
        page: 0,
        size: 50,
        sort: [],
      },
    });
    expect(screen.getByText("Espresso Lab Mitte")).toBeInTheDocument();
    expect(screen.getByText(/Submitted by Alex Reviewer/)).toBeInTheDocument();
    expect(screen.getByText(/Consistent coffee quality/)).toBeInTheDocument();
    expect(screen.getByText(/Receipt from 2026-04-12/)).toBeInTheDocument();
    expect(screen.getByText("1 pending")).toBeInTheDocument();
    expect(screen.getByText("Recent decisions")).toBeInTheDocument();
    expect(screen.getByText("Rejected Espresso")).toBeInTheDocument();
    expect(screen.getByText("Needs factual support.")).toBeInTheDocument();
    expect(mockUseModerationDecisions).toHaveBeenCalledWith({
      pageable: {
        page: 0,
        size: 20,
        sort: [],
      },
    });
  });

  it("shows loading, empty, and error states", () => {
    mockModerationQuery({
      data: undefined,
      isLoading: true,
    });
    const { rerender } = render(<OpinionModeration />);
    expect(screen.getByText("Loading moderation queue...")).toBeInTheDocument();

    mockModerationQuery({
      data: pageResponse([]),
    });
    rerender(<OpinionModeration />);
    expect(screen.getByText("No opinions are waiting for review.")).toBeInTheDocument();

    mockModerationQuery({
      data: undefined,
      error: new Error("Forbidden"),
      isError: true,
    });
    rerender(<OpinionModeration />);
    expect(screen.getByText("Unable to load")).toBeInTheDocument();
    expect(screen.getByText("Forbidden")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(refetch).toHaveBeenCalledTimes(1);
  });

  it("approves a pending opinion", async () => {
    approveMutateAsync.mockResolvedValue(pendingOpinion);

    render(<OpinionModeration />);

    fireEvent.click(screen.getByRole("button", { name: "Approve" }));

    await waitFor(() => {
      expect(approveMutateAsync).toHaveBeenCalledWith({
        opinionId: "opinion-1",
      });
    });
  });

  it("requires a rejection reason and submits the trimmed reason", async () => {
    rejectMutateAsync.mockResolvedValue({
      ...pendingOpinion,
      status: "REJECTED",
    });

    render(<OpinionModeration />);

    fireEvent.click(screen.getByRole("button", { name: "Reject" }));
    fireEvent.click(screen.getByRole("button", { name: "Reject with reason" }));

    expect(screen.getByText("Rejection reason is required.")).toBeInTheDocument();
    expect(rejectMutateAsync).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText("Rejection reason"), {
      target: {
        value: "  Needs factual support.  ",
      },
    });
    fireEvent.click(screen.getByRole("button", { name: "Reject with reason" }));

    await waitFor(() => {
      expect(rejectMutateAsync).toHaveBeenCalledWith({
        opinionId: "opinion-1",
        reason: "Needs factual support.",
      });
    });
  });
});
