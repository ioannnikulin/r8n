import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import {
  ChevronDown,
  Clock,
  Plus,
  SendHorizontal,
} from "lucide-react";
import UserAvatar from "@/components/UserAvatar";
import { Button } from "@/components/ui/button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { MOCK_MESSAGE_THREADS, type MessageDirection, type MessageThread, type ThreadMessage } from "@/lib/messages";
import { cn } from "@/lib/utils";

type MessageFilter = "all" | "inbox" | "outbox" | "support";

const FILTERS: Array<{ id: MessageFilter; label: string }> = [
  { id: "all", label: "All" },
  { id: "inbox", label: "Inbox" },
  { id: "outbox", label: "Outbox" },
  { id: "support", label: "Support" },
];

function getDirectionMeta(direction: MessageDirection) {
  return direction === "outgoing"
    ? {
        bubbleClassName: "border-primary/20 bg-primary/5",
        layoutClassName: "justify-end",
      }
    : {
        bubbleClassName: "border-border bg-background",
        layoutClassName: "justify-start",
      };
}

const Messages = () => {
  const [threads, setThreads] = useState<MessageThread[]>(MOCK_MESSAGE_THREADS);
  const [openThreads, setOpenThreads] = useState<string[]>([]);
  const [activeFilter, setActiveFilter] = useState<MessageFilter>("all");
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [isNewMessageDialogOpen, setIsNewMessageDialogOpen] = useState(false);
  const [newRecipient, setNewRecipient] = useState("");
  const [newMessage, setNewMessage] = useState("");

  const filteredThreads = useMemo(
    () =>
      threads.filter((thread) => {
        const lastDirection = thread.messages[thread.messages.length - 1]?.direction;

        if (activeFilter === "inbox") {
          return lastDirection === "incoming";
        }

        if (activeFilter === "outbox") {
          return lastDirection === "outgoing";
        }

        if (activeFilter === "support") {
          return thread.participantRole === "Support";
        }

        return true;
      }),
    [activeFilter, threads],
  );

  const toggleThread = (threadId: string) => {
    setOpenThreads((current) =>
      current.includes(threadId)
        ? current.filter((id) => id !== threadId)
        : [...current, threadId],
    );
  };

  const updateDraft = (threadId: string, value: string) => {
    setDrafts((current) => ({
      ...current,
      [threadId]: value,
    }));
  };

  const sendMessage = (threadId: string) => {
    const draft = drafts[threadId]?.trim();

    if (!draft) {
      return;
    }

    setThreads((current) => {
      const thread = current.find((item) => item.id === threadId);
      if (!thread) {
        return current;
      }

      const nextMessage: ThreadMessage = {
        id: `msg-${threadId}-${Date.now()}`,
        direction: "outgoing",
        authorName: "You",
        body: draft,
        sentAt: "Just now",
      };

      const updatedThread: MessageThread = {
        ...thread,
        updatedAt: "Just now",
        unreadCount: 0,
        messages: [...thread.messages, nextMessage],
      };

      return [
        updatedThread,
        ...current.filter((item) => item.id !== threadId),
      ];
    });

    setDrafts((current) => ({
      ...current,
      [threadId]: "",
    }));
  };

  const createThread = () => {
    const recipientName = newRecipient.trim();
    const messageBody = newMessage.trim();

    if (!recipientName || !messageBody) {
      return;
    }

    const normalizedRecipient = recipientName.toLowerCase();
    const isSupportThread = normalizedRecipient.includes("support");
    const threadId = `thread-new-${Date.now()}`;
    const createdThread: MessageThread = {
      id: threadId,
      subject: isSupportThread
        ? "New support conversation"
        : `Conversation with ${recipientName}`,
      participantName: recipientName,
      participantLastSeenAt: null,
      participantRole: isSupportThread ? "Support" : "User",
      context: isSupportThread ? "Support conversation" : "Direct message",
      updatedAt: "Just now",
      unreadCount: 0,
      messages: [
        {
          id: `msg-${threadId}-1`,
          direction: "outgoing",
          authorName: "You",
          body: messageBody,
          sentAt: "Just now",
        },
      ],
    };

    setThreads((current) => [createdThread, ...current]);
    setOpenThreads((current) => [threadId, ...current]);
    setActiveFilter("all");
    setDrafts((current) => ({
      ...current,
      [threadId]: "",
    }));
    setNewRecipient("");
    setNewMessage("");
    setIsNewMessageDialogOpen(false);
  };

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-10 flex items-start justify-between gap-4"
      >
        <div>
          <h1 className="mb-1 text-2xl font-semibold tracking-tight text-foreground md:text-3xl">
            Messages
          </h1>
          <p className="text-sm text-muted-foreground">
            Private conversations with other users and R8N support.
          </p>
        </div>
        <Button
          type="button"
          variant="outline"
          className="rounded-xl"
          onClick={() => setIsNewMessageDialogOpen(true)}
        >
          <Plus className="h-4 w-4" />
          New message
        </Button>
      </motion.div>

      <div className="mb-6 flex flex-wrap gap-2" aria-label="Message filters">
        {FILTERS.map((filter) => (
          <button
            key={filter.id}
            type="button"
            onClick={() => setActiveFilter(filter.id)}
            className={cn(
              "rounded-xl border px-4 py-2 text-sm font-medium transition-colors",
              activeFilter === filter.id
                ? "border-primary/20 bg-primary/5 text-foreground"
                : "border-border bg-card text-muted-foreground hover:bg-muted/50 hover:text-foreground",
            )}
          >
            {filter.label}
          </button>
        ))}
      </div>

      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.1 }}
        className="space-y-4"
      >
        {filteredThreads.map((thread) => {
          const isOpen = openThreads.includes(thread.id);
          const lastMessage = thread.messages[thread.messages.length - 1];
          const previewMeta = getDirectionMeta(lastMessage.direction);

          return (
            <Collapsible
              key={thread.id}
              open={isOpen}
              onOpenChange={() => toggleThread(thread.id)}
              className="overflow-hidden rounded-2xl border border-border bg-card shadow-card"
            >
              <div className="border-b border-border/70 px-5 py-4">
                <div className="flex items-start gap-4">
                  <UserAvatar
                    name={thread.participantName}
                    lastSeenAt={thread.participantLastSeenAt}
                    size="md"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="mb-1 flex flex-wrap items-center gap-2">
                      <h2 className="truncate text-sm font-semibold text-foreground">
                        {thread.subject}
                      </h2>
                      {thread.unreadCount > 0 && (
                        <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-accent px-1.5 text-[10px] font-mono font-semibold text-accent-foreground">
                          {thread.unreadCount}
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {thread.participantName} · {thread.participantRole} · {thread.context}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-1.5 text-[10px] text-muted-foreground/70">
                    <Clock className="h-3 w-3" />
                    {thread.updatedAt}
                  </div>
                </div>
              </div>

              <CollapsibleTrigger asChild>
                <button
                  type="button"
                  className="w-full px-5 py-4 text-left transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  aria-label={`${isOpen ? "Collapse" : "Expand"} thread with ${thread.participantName}`}
                >
                  <div
                    className={cn(
                      "flex items-start gap-3",
                      previewMeta.layoutClassName,
                    )}
                  >
                    <div
                      className={cn(
                        "max-w-[82%] rounded-2xl border px-4 py-3",
                        previewMeta.bubbleClassName,
                      )}
                    >
                      <div className="mb-1.5 flex flex-wrap items-center gap-2">
                        <span className="text-sm font-medium text-foreground">
                          {lastMessage.authorName}
                        </span>
                        <span className="text-[10px] text-muted-foreground/70">
                          {lastMessage.sentAt}
                        </span>
                      </div>
                      {!isOpen && (
                        <p className="line-clamp-2 text-sm leading-6 text-foreground/85">
                          {lastMessage.body}
                        </p>
                      )}
                    </div>
                    <ChevronDown
                      className={cn(
                        "mt-1 h-4 w-4 shrink-0 text-muted-foreground transition-transform",
                        isOpen && "rotate-180",
                      )}
                    />
                  </div>
                </button>
              </CollapsibleTrigger>

              <CollapsibleContent>
                <div className="border-t border-border/70 px-5 py-4">
                  <div className="space-y-4">
                    {thread.messages.map((message) => {
                      const messageMeta = getDirectionMeta(message.direction);

                      return (
                        <article
                          key={message.id}
                          className={cn(
                            "flex gap-3",
                            message.direction === "outgoing" && "flex-row-reverse",
                          )}
                        >
                          <div
                            className={cn(
                              "max-w-[82%] rounded-2xl border px-4 py-3",
                              messageMeta.bubbleClassName,
                            )}
                          >
                            <div className="mb-1.5 flex flex-wrap items-center gap-2">
                              <span className="text-sm font-medium text-foreground">
                                {message.authorName}
                              </span>
                              <span className="text-[10px] text-muted-foreground/70">
                                {message.sentAt}
                              </span>
                            </div>
                            <p className="text-sm leading-6 text-foreground/85">
                              {message.body}
                            </p>
                          </div>
                        </article>
                      );
                    })}
                  </div>
                  <div className="mt-5 border-t border-border/70 pt-4">
                    <div className="rounded-2xl border border-border bg-background p-3">
                      <Textarea
                        value={drafts[thread.id] ?? ""}
                        onChange={(event) => updateDraft(thread.id, event.target.value)}
                        placeholder={`Message ${thread.participantName}...`}
                        className="min-h-[96px] resize-none border-0 px-0 py-0 shadow-none focus-visible:ring-0"
                      />
                      <div className="mt-3 flex justify-end">
                        <Button
                          type="button"
                          size="sm"
                          className="rounded-xl"
                          disabled={!drafts[thread.id]?.trim()}
                          onClick={() => sendMessage(thread.id)}
                        >
                          <SendHorizontal className="h-4 w-4" />
                          Send
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              </CollapsibleContent>
            </Collapsible>
          );
        })}
      </motion.section>

      <Dialog open={isNewMessageDialogOpen} onOpenChange={setIsNewMessageDialogOpen}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle>New message</DialogTitle>
            <DialogDescription>
              Start a private conversation with another user or R8N support.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="new-message-recipient" className="text-sm font-medium text-foreground">
                Recipient
              </label>
              <Input
                id="new-message-recipient"
                value={newRecipient}
                onChange={(event) => setNewRecipient(event.target.value)}
                placeholder="Enter a name or R8N Support"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="new-message-body" className="text-sm font-medium text-foreground">
                Message
              </label>
              <Textarea
                id="new-message-body"
                value={newMessage}
                onChange={(event) => setNewMessage(event.target.value)}
                placeholder="Write the first message..."
                className="min-h-[120px] resize-none"
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              className="rounded-xl"
              onClick={() => setIsNewMessageDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="button"
              className="rounded-xl"
              disabled={!newRecipient.trim() || !newMessage.trim()}
              onClick={createThread}
            >
              <SendHorizontal className="h-4 w-4" />
              Start thread
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Messages;
