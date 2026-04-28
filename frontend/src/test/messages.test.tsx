import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { MessageDto, ThreadSummaryDto } from "@/lib/api/messages";
import Messages from "@/pages/Messages";

const mocks = vi.hoisted(() => ({
  createDirectThreadMutate: vi.fn(),
  createSupportThreadMutate: vi.fn(),
  markThreadReadMutate: vi.fn(),
  sendThreadMessageMutate: vi.fn(),
  threadsState: {
    data: undefined as { items: ThreadSummaryDto[] } | undefined,
    error: null as Error | null,
    isError: false,
    isLoading: false,
  },
  messagesByThread: new Map<string, MessageDto[]>(),
}));

vi.mock("@/lib/server-state", async () => {
  const actual = await vi.importActual<typeof import("@/lib/server-state")>("@/lib/server-state");

  return {
    ...actual,
    useCreateDirectThreadMutation: () => ({
      isPending: false,
      mutate: mocks.createDirectThreadMutate,
    }),
    useCreateSupportThreadMutation: () => ({
      isPending: false,
      mutate: mocks.createSupportThreadMutate,
    }),
    useMarkThreadReadMutation: () => ({
      isPending: false,
      mutate: mocks.markThreadReadMutate,
    }),
    useMessageThreads: () => ({
      ...mocks.threadsState,
      refetch: vi.fn(),
    }),
    useSendThreadMessageMutation: () => ({
      isPending: false,
      mutate: mocks.sendThreadMessageMutate,
    }),
    useThreadMessages: ({ threadId }: { threadId: string }) => ({
      data: {
        items: mocks.messagesByThread.get(threadId) ?? [],
      },
      error: null,
      isError: false,
      isLoading: false,
      refetch: vi.fn(),
    }),
  };
});

const THREADS: ThreadSummaryDto[] = [
  {
    createdAt: "2026-01-01T09:00:00Z",
    id: "thread-direct",
    lastMessageOwn: false,
    lastMessagePreview: "That helps, thanks.",
    participant: {
      name: "Marta Keller",
      role: "USER",
      userId: "11111111-1111-1111-1111-111111111111",
    },
    type: "DIRECT",
    unreadCount: 2,
    updatedAt: "2026-01-01T10:04:00Z",
  },
  {
    createdAt: "2026-01-01T08:00:00Z",
    id: "thread-support",
    lastMessageOwn: false,
    lastMessagePreview: "Your export is being prepared.",
    participant: {
      name: "R8N Support",
      role: "SUPPORT",
      userId: null,
    },
    type: "SUPPORT",
    unreadCount: 0,
    updatedAt: "2026-01-01T09:00:00Z",
  },
  {
    createdAt: "2026-01-01T07:00:00Z",
    id: "thread-outbox",
    lastMessageOwn: true,
    lastMessagePreview: "Yes, but only for small batches.",
    participant: {
      name: "Elena Rossi",
      role: "USER",
      userId: "22222222-2222-2222-2222-222222222222",
    },
    type: "DIRECT",
    unreadCount: 0,
    updatedAt: "2026-01-01T08:00:00Z",
  },
];

const DIRECT_MESSAGES: MessageDto[] = [
  {
    authorName: "Marta Keller",
    authorRole: "USER",
    authorUserId: "11111111-1111-1111-1111-111111111111",
    createdAt: "2026-01-01T09:42:00Z",
    id: "message-direct-1",
    own: false,
    text: "I saw your note about the grinder noise level.",
    threadId: "thread-direct",
  },
  {
    authorName: "Current User",
    authorRole: "USER",
    authorUserId: "00000000-0000-0000-0000-000000000000",
    createdAt: "2026-01-01T09:51:00Z",
    id: "message-direct-2",
    own: true,
    text: "Daily use. It was fine for short grinding.",
    threadId: "thread-direct",
  },
];

describe("Messages page", () => {
  beforeEach(() => {
    mocks.createDirectThreadMutate.mockReset();
    mocks.createSupportThreadMutate.mockReset();
    mocks.markThreadReadMutate.mockReset();
    mocks.sendThreadMessageMutate.mockReset();
    mocks.threadsState.data = { items: THREADS };
    mocks.threadsState.error = null;
    mocks.threadsState.isError = false;
    mocks.threadsState.isLoading = false;
    mocks.messagesByThread.clear();
    mocks.messagesByThread.set("thread-direct", DIRECT_MESSAGES);
  });

  it("shows the latest message in a collapsed thread and expands on click", () => {
    render(<Messages />);

    expect(screen.getByText("That helps, thanks.")).toBeInTheDocument();
    expect(screen.queryByText("I saw your note about the grinder noise level.")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Expand thread with Marta Keller" }));

    expect(screen.getByText("I saw your note about the grinder noise level.")).toBeInTheDocument();
    expect(mocks.markThreadReadMutate).toHaveBeenCalledWith({ threadId: "thread-direct" });
  });

  it("does not show incoming and outgoing direction labels", () => {
    render(<Messages />);

    expect(screen.queryByText("To you")).not.toBeInTheDocument();
    expect(screen.queryByText("From you")).not.toBeInTheDocument();
  });

  it("filters support conversations", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "Support" }));

    expect(screen.getByText("Support conversation")).toBeInTheDocument();
    expect(screen.queryByText("Conversation with Marta Keller")).not.toBeInTheDocument();
    expect(screen.queryByText("Conversation with Elena Rossi")).not.toBeInTheDocument();
  });

  it("sends a message through the mutation hook", () => {
    mocks.sendThreadMessageMutate.mockImplementation((_variables, options) => {
      options?.onSuccess?.();
    });

    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "Expand thread with Marta Keller" }));
    fireEvent.change(
      screen.getByPlaceholderText("Message Marta Keller..."),
      { target: { value: "Thanks, please send it here once it is ready." } },
    );
    fireEvent.click(screen.getByRole("button", { name: "Send" }));

    expect(mocks.sendThreadMessageMutate).toHaveBeenCalledWith(
      {
        text: "Thanks, please send it here once it is ready.",
        threadId: "thread-direct",
      },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    );
    expect(screen.getByPlaceholderText("Message Marta Keller...")).toHaveValue("");
  });

  it("creates a support thread from the new message dialog", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "New message" }));
    fireEvent.change(screen.getByLabelText("Message"), {
      target: { value: "Hi, I need help with export." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Start thread" }));

    expect(mocks.createSupportThreadMutate).toHaveBeenCalledWith(
      { initialMessage: "Hi, I need help with export." },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    );
  });

  it("creates a direct thread with a recipient user id", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "New message" }));
    fireEvent.click(screen.getByRole("button", { name: "Direct" }));
    fireEvent.change(screen.getByLabelText("Recipient user ID"), {
      target: { value: "33333333-3333-3333-3333-333333333333" },
    });
    fireEvent.change(screen.getByLabelText("Message"), {
      target: { value: "Hi, I wanted to ask about your supplier shortlist." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Start thread" }));

    expect(mocks.createDirectThreadMutate).toHaveBeenCalledWith(
      {
        initialMessage: "Hi, I wanted to ask about your supplier shortlist.",
        recipientUserId: "33333333-3333-3333-3333-333333333333",
      },
      expect.objectContaining({ onSuccess: expect.any(Function) }),
    );
  });

  it("handles loading, error, and empty states", () => {
    mocks.threadsState.isLoading = true;
    const { rerender } = render(<Messages />);
    expect(screen.getByText("Loading...")).toBeInTheDocument();

    mocks.threadsState.isLoading = false;
    mocks.threadsState.isError = true;
    mocks.threadsState.error = new Error("Unable to reach messaging service");
    rerender(<Messages />);
    expect(screen.getByText("Unable to load")).toBeInTheDocument();
    expect(screen.getByText("Unable to reach messaging service")).toBeInTheDocument();

    mocks.threadsState.isError = false;
    mocks.threadsState.error = null;
    mocks.threadsState.data = { items: [] };
    rerender(<Messages />);
    expect(screen.getByText("No conversations yet.")).toBeInTheDocument();
  });
});
