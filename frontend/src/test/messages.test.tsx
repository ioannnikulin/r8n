import { fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Messages from "@/pages/Messages";

const mocks = vi.hoisted(() => ({
  addDirectConversationMessage: vi.fn(),
  addSupportThreadMessage: vi.fn(),
  createDirectConversation: vi.fn(),
  createSupportThread: vi.fn(),
  markDirectConversationAsRead: vi.fn(),
}));

vi.mock("@/lib/server-state/hooks/users", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/server-state/hooks/users")>();
  return {
    ...actual,
    useUserAvatar: () => ({ data: null }),
    useUserProfile: () => ({ data: null }),
  };
});

vi.mock("@/lib/server-state", () => ({
  useAddDirectConversationMessageMutation: (options?: { onSuccess?: () => void }) => ({
    isPending: false,
    mutate: mocks.addDirectConversationMessage.mockImplementation(() => options?.onSuccess?.()),
  }),
  useAddSupportThreadMessageMutation: (options?: { onSuccess?: () => void }) => ({
    isPending: false,
    mutate: mocks.addSupportThreadMessage.mockImplementation(() => options?.onSuccess?.()),
  }),
  useCreateDirectConversationMutation: (options?: { onSuccess?: (conv: { id: string }) => void }) => ({
    isPending: false,
    mutate: mocks.createDirectConversation.mockImplementation(() =>
      options?.onSuccess?.({
        createdAt: "2026-04-29T09:30:00Z",
        id: "direct-conv-lina",
        lastMessageAt: "2026-04-29T09:30:00Z",
        lastMessageText: "Hi, I wanted to ask about your supplier shortlist.",
        participantDisplayName: "Lina Hartmann",
        participantUserId: "lina-id",
        unreadCount: 0,
      }),
    ),
  }),
  useCreateSupportThreadMutation: (options?: { onSuccess?: (thread: { id: string }) => void }) => ({
    isPending: false,
    mutate: mocks.createSupportThread.mockImplementation(() =>
      options?.onSuccess?.({ id: "support-thread-new" }),
    ),
  }),
  useDeleteSupportThreadMutation: (options?: { onSuccess?: () => void }) => ({
    isPending: false,
    mutate: vi.fn().mockImplementation(() => options?.onSuccess?.()),
  }),
  useDirectConversationMessages: (request: { conversationId: string }) => {
    if (request.conversationId === "direct-conv-lina") {
      return {
        data: {
          items: [
            {
              authorDisplayName: "You",
              authorRole: "USER",
              authorUserId: "current-user",
              conversationId: "direct-conv-lina",
              createdAt: "2026-04-29T09:30:00Z",
              id: "direct-msg-lina-1",
              text: "Hi, I wanted to ask about your supplier shortlist.",
            },
          ],
          page: 0,
          size: 100,
          total: 1,
        },
        isError: false,
        isLoading: false,
      };
    }
    return { data: { items: [], page: 0, size: 100, total: 0 }, isError: false, isLoading: false };
  },
  useDirectConversationSummaries: () => ({
    data: {
      items: [
        {
          createdAt: "2026-04-29T08:00:00Z",
          id: "direct-conv-marta",
          lastMessageAt: "2026-04-29T08:10:00Z",
          lastMessageText: "Sure, let me check the availability.",
          participantDisplayName: "Marta Keller",
          participantUserId: "marta-id",
          unreadCount: 0,
        },
        {
          createdAt: "2026-04-29T08:00:00Z",
          id: "direct-conv-elena",
          lastMessageAt: "2026-04-29T08:00:00Z",
          lastMessageText: "Sounds great!",
          participantDisplayName: "Elena Rossi",
          participantUserId: "elena-id",
          unreadCount: 0,
        },
        {
          createdAt: "2026-04-29T09:30:00Z",
          id: "direct-conv-lina",
          lastMessageAt: "2026-04-29T09:30:00Z",
          lastMessageText: "Hi, I wanted to ask about your supplier shortlist.",
          participantDisplayName: "Lina Hartmann",
          participantUserId: "lina-id",
          unreadCount: 0,
        },
      ],
      page: 0,
      size: 50,
      total: 3,
    },
    isError: false,
    isLoading: false,
  }),
  useMarkDirectConversationAsReadMutation: () => ({
    mutate: mocks.markDirectConversationAsRead,
  }),
  useMe: () => ({
    data: { id: "current-user", name: "You", roles: ["USER"] },
  }),
  useSupportThreadMessages: () => ({
    data: {
      items: [
        {
          authorDisplayName: "You",
          authorRole: "USER",
          authorUserId: "current-user",
          createdAt: "2026-04-29T09:30:00Z",
          id: "support-message-1",
          text: "I requested an export of my account data this morning. Can you confirm when it will be ready?",
          threadId: "support-thread-1",
        },
        {
          authorDisplayName: "R8N Support",
          authorRole: "SUPPORT",
          authorUserId: "support-user",
          createdAt: "2026-04-29T09:35:00Z",
          id: "support-message-2",
          text: "Your export is being prepared. We will notify you here when the archive is ready to download.",
          threadId: "support-thread-1",
        },
      ],
      page: 0,
      size: 100,
      total: 2,
    },
    isError: false,
    isLoading: false,
  }),
  useSupportThreadSummaries: () => ({
    data: {
      items: [
        {
          createdAt: "2026-04-29T09:30:00Z",
          id: "support-thread-1",
          lastMessageAt: "2026-04-29T09:35:00Z",
          lastMessageText: "Your export is being prepared. We will notify you here when the archive is ready to download.",
          ownerUserId: "current-user",
          unreadCount: 0,
          viewerRole: "REQUESTER",
        },
      ],
      page: 0,
      size: 50,
      total: 1,
    },
    isError: false,
    isLoading: false,
  }),
  useUserSearch: () => ({
    data: [{ id: "lina-id", lastSeenAt: null, name: "Lina Hartmann" }],
    isError: false,
    isLoading: false,
  }),
}));

describe("Messages page", () => {
  beforeEach(() => {
    mocks.addDirectConversationMessage.mockClear();
    mocks.addSupportThreadMessage.mockClear();
    mocks.createDirectConversation.mockClear();
    mocks.createSupportThread.mockClear();
    mocks.markDirectConversationAsRead.mockClear();
  });

  it("shows the latest message in the thread list and opens the selected chat", () => {
    render(<Messages />);

    expect(
      screen.getByText("Your export is being prepared. We will notify you here when the archive is ready to download."),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("I requested an export of my account data this morning. Can you confirm when it will be ready?"),
    ).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Select thread with R8N Support" }));

    expect(
      screen.getByText("I requested an export of my account data this morning. Can you confirm when it will be ready?"),
    ).toBeInTheDocument();
  });

  it("does not show incoming and outgoing direction labels", () => {
    render(<Messages />);

    expect(screen.queryByText("To you")).not.toBeInTheDocument();
    expect(screen.queryByText("From you")).not.toBeInTheDocument();
  });

  it("shows a presence avatar for direct message thread participants", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "Select thread with Marta Keller" }));

    expect(screen.getAllByRole("button", { name: "Marta Keller presence" }).length).toBeGreaterThan(0);
  });

  it("does not show inbox outbox and support filter buttons", () => {
    render(<Messages />);

    expect(screen.queryByRole("button", { name: "Inbox" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Outbox" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Support" })).not.toBeInTheDocument();
    expect(screen.getByText("Marta Keller")).toBeInTheDocument();
    expect(screen.getByText("Elena Rossi")).toBeInTheDocument();
  });

  it("sends a new message in an expanded thread", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "Select thread with R8N Support" }));
    fireEvent.change(
      screen.getByPlaceholderText("Message R8N Support..."),
      { target: { value: "Thanks, please send it here once it is ready." } },
    );
    fireEvent.click(screen.getByRole("button", { name: "Send" }));

    expect(mocks.addSupportThreadMessage).toHaveBeenCalledWith({
      request: { text: "Thanks, please send it here once it is ready." },
      threadId: "support-thread-1",
    });
    expect(screen.getByPlaceholderText("Message R8N Support...")).toHaveValue("");
  });

  it("creates a support thread through the backend mutation", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "New message" }));
    fireEvent.change(screen.getByLabelText("Recipient"), {
      target: { value: "R8N Support" },
    });
    fireEvent.change(screen.getByLabelText("Message"), {
      target: { value: "Need help with a review." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Start thread" }));

    expect(mocks.createSupportThread).toHaveBeenCalledWith({
      initialMessage: "Need help with a review.",
    });
  });

  it("creates a new thread from the new message dialog", () => {
    render(<Messages />);

    fireEvent.click(screen.getByRole("button", { name: "New message" }));

    const dialog = screen.getByRole("dialog");

    fireEvent.change(within(dialog).getByLabelText("Recipient"), {
      target: { value: "Lina Hartmann" },
    });
    fireEvent.click(within(dialog).getByText("Lina Hartmann"));
    fireEvent.change(within(dialog).getByLabelText("Message"), {
      target: { value: "Hi, I wanted to ask about your supplier shortlist." },
    });
    fireEvent.click(within(dialog).getByRole("button", { name: "Start thread" }));

    expect(mocks.createDirectConversation).toHaveBeenCalledWith({
      initialMessage: "Hi, I wanted to ask about your supplier shortlist.",
      recipientUserId: "lina-id",
    });
    expect(screen.getAllByText("Lina Hartmann")).toHaveLength(2);
    expect(
      screen.getAllByText("Hi, I wanted to ask about your supplier shortlist."),
    ).toHaveLength(2);
    expect(screen.getByPlaceholderText("Message Lina Hartmann...")).toBeInTheDocument();
  });
});
