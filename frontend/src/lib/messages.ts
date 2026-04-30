export type MessageDirection = "incoming" | "outgoing";

export interface ThreadMessage {
  id: string;
  direction: MessageDirection;
  authorName: string;
  body: string;
  sentAt: string;
}

export interface MessageThread {
  id: string;
  subject: string;
  participantName: string;
  participantLastSeenAt: string | null;
  participantRole: string;
  context: string;
  updatedAt: string;
  unreadCount: number;
  messages: ThreadMessage[];
}

export const MOCK_MESSAGE_THREADS: MessageThread[] = [
  {
    id: "thread-user-1",
    subject: "Question about your coffee grinder review",
    participantName: "Marta Keller",
    participantLastSeenAt: new Date(Date.now() - 18 * 60_000).toISOString(),
    participantRole: "User",
    context: "Direct message",
    updatedAt: "18m ago",
    unreadCount: 2,
    messages: [
      {
        id: "msg-user-1",
        direction: "incoming",
        authorName: "Marta Keller",
        body: "I saw your note about the grinder noise level. Was that during daily use or only while testing?",
        sentAt: "09:42",
      },
      {
        id: "msg-user-2",
        direction: "outgoing",
        authorName: "You",
        body: "Daily use. It was fine for short grinding, but too loud for the small kitchen during service hours.",
        sentAt: "09:51",
      },
      {
        id: "msg-user-3",
        direction: "incoming",
        authorName: "Marta Keller",
        body: "That helps, thanks. I will compare it with the quieter model before ordering.",
        sentAt: "10:04",
      },
    ],
  },
  {
    id: "thread-support-1",
    subject: "Export archive request",
    participantName: "R8N Support",
    participantLastSeenAt: null,
    participantRole: "Support",
    context: "Support conversation",
    updatedAt: "1h ago",
    unreadCount: 0,
    messages: [
      {
        id: "msg-support-1",
        direction: "outgoing",
        authorName: "You",
        body: "I requested an export of my account data this morning. Can you confirm when it will be ready?",
        sentAt: "Yesterday",
      },
      {
        id: "msg-support-2",
        direction: "incoming",
        authorName: "R8N Support",
        body: "Your export is being prepared. We will notify you here when the archive is ready to download.",
        sentAt: "Today",
      },
    ],
  },
  {
    id: "thread-user-2",
    subject: "Supplier recommendation follow-up",
    participantName: "Elena Rossi",
    participantLastSeenAt: new Date(Date.now() - 26 * 60 * 60_000).toISOString(),
    participantRole: "User",
    context: "Direct message",
    updatedAt: "Yesterday",
    unreadCount: 1,
    messages: [
      {
        id: "msg-user-4",
        direction: "incoming",
        authorName: "Elena Rossi",
        body: "Do you still recommend the packaging supplier from your March list?",
        sentAt: "Yesterday",
      },
      {
        id: "msg-user-5",
        direction: "outgoing",
        authorName: "You",
        body: "Yes, but only for small batches. Their lead time became inconsistent for larger orders.",
        sentAt: "Yesterday",
      },
    ],
  },
];

export function getUnreadMessagesCount(threads: MessageThread[]): number {
  return threads.reduce((sum, thread) => sum + thread.unreadCount, 0);
}
