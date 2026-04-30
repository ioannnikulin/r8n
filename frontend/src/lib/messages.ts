export type MessageDirection = "incoming" | "outgoing";

export interface ThreadMessage {
  id: string;
  direction: MessageDirection;
  authorName: string;
  authorRoleLabel?: string;
  body: string;
  sentAt: string;
}

export interface MessageThread {
  id: string;
  subject: string;
  participantUserId?: string;
  participantName: string;
  participantLastSeenAt: string | null;
  participantRole: string;
  context: string;
  supportViewerRole?: "REQUESTER" | "SUPPORT";
  updatedAt: string;
  unreadCount: number;
  messages: ThreadMessage[];
}
