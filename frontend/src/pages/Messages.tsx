import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock,
  Plus,
  SendHorizontal,
} from "lucide-react";
import UserAvatar from "@/components/UserAvatar";
import { QueryState } from "@/components/server-state/QueryState";
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
import type { MessageDto, ThreadSummaryDto } from "@/lib/api/messages";
import {
  useCreateDirectThreadMutation,
  useCreateSupportThreadMutation,
  useMarkThreadReadMutation,
  useMessageThreads,
  useSendThreadMessageMutation,
  useThreadMessages,
} from "@/lib/server-state";
import { cn } from "@/lib/utils";

type MessageFilter = "all" | "inbox" | "outbox" | "support";
type NewThreadType = "support" | "direct";

const FILTERS: Array<{ id: MessageFilter; label: string }> = [
  { id: "all", label: "All" },
  { id: "inbox", label: "Inbox" },
  { id: "outbox", label: "Outbox" },
  { id: "support", label: "Support" },
];

const THREADS_PAGE_SIZE = 2;

const MESSAGES_PAGE_SIZE = 5;

function getDirectionMeta(own: boolean) {
  return own
    ? {
        bubbleClassName: "border-primary/20 bg-primary/5",
        layoutClassName: "justify-end",
      }
    : {
        bubbleClassName: "border-border bg-background",
        layoutClassName: "justify-start",
      };
}

function formatTimestamp(timestamp: string) {
  const date = new Date(timestamp);

  if (Number.isNaN(date.getTime())) {
    return timestamp;
  }

  return new Intl.DateTimeFormat(undefined, {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "short",
  }).format(date);
}

function getThreadSubject(thread: ThreadSummaryDto) {
  return thread.type === "SUPPORT"
    ? "Support conversation"
    : `Conversation with ${thread.participant.name}`;
}

function getThreadContext(thread: ThreadSummaryDto) {
  return thread.type === "SUPPORT" ? "Support conversation" : "Direct message";
}

const Messages = () => {
  const [openThreads, setOpenThreads] = useState<string[]>([]);
  const [activeFilter, setActiveFilter] = useState<MessageFilter>("all");
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [isNewMessageDialogOpen, setIsNewMessageDialogOpen] = useState(false);
  const [threadPage, setThreadPage] = useState(0);
  const [messagePages, setMessagePages] = useState<Record<string, number>>({});
  const [newThreadType, setNewThreadType] = useState<NewThreadType>("support");
  const [newRecipientUserId, setNewRecipientUserId] = useState("");
  const [newMessage, setNewMessage] = useState("");

  const threadsQuery = useMessageThreads({
    pageable: {
      page: threadPage,
      size: THREADS_PAGE_SIZE,
    },
  });
  const createSupportThread = useCreateSupportThreadMutation();
  const createDirectThread = useCreateDirectThreadMutation();

  const threads = useMemo(
    () => threadsQuery.data?.items ?? [],
    [threadsQuery.data?.items],
  );
  const filteredThreads = useMemo(
    () =>
      threads.filter((thread) => {
        if (activeFilter === "inbox") {
          return !thread.lastMessageOwn;
        }

        if (activeFilter === "outbox") {
          return thread.lastMessageOwn;
        }

        if (activeFilter === "support") {
          return thread.type === "SUPPORT";
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

  const updateMessagePage = (threadId: string, page: number) => {
    setMessagePages((current) => ({
      ...current,
      [threadId]: Math.max(0, page),
    }));
  };

  const updateDraft = (threadId: string, value: string) => {
    setDrafts((current) => ({
      ...current,
      [threadId]: value,
    }));
  };

  const resetNewThreadForm = () => {
    setNewThreadType("support");
    setNewRecipientUserId("");
    setNewMessage("");
  };

  const createThread = () => {
    const messageBody = newMessage.trim();
    const recipientUserId = newRecipientUserId.trim();

    if (!messageBody || (newThreadType === "direct" && !recipientUserId)) {
      return;
    }

    const mutation =
      newThreadType === "support"
        ? createSupportThread.mutate
        : createDirectThread.mutate;

    mutation(
      newThreadType === "support"
        ? { initialMessage: messageBody }
        : { initialMessage: messageBody, recipientUserId },
      {
        onSuccess: (thread) => {
          setOpenThreads((current) => [thread.id, ...current.filter((id) => id !== thread.id)]);
          setActiveFilter("all");
          resetNewThreadForm();
          setIsNewMessageDialogOpen(false);
        },
      },
    );
  };

  const isCreatingThread = createSupportThread.isPending || createDirectThread.isPending;
  const canCreateThread =
    newMessage.trim() !== "" &&
    (newThreadType === "support" || newRecipientUserId.trim() !== "");

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
            onClick={() => {
              setActiveFilter(filter.id);
              setThreadPage(0);
            }}
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
        <QueryState
          isLoading={threadsQuery.isLoading}
          isError={threadsQuery.isError}
          error={threadsQuery.error}
          isEmpty={filteredThreads.length === 0}
          emptyMessage={
            activeFilter === "all"
              ? "No conversations yet."
              : "No conversations match this filter."
          }
          onRetry={() => threadsQuery.refetch()}
        >
          {filteredThreads.map((thread) => (
            <MessageThreadItem
              key={thread.id}
              thread={thread}
              isOpen={openThreads.includes(thread.id)}
              draft={drafts[thread.id] ?? ""}
              onDraftChange={(value) => updateDraft(thread.id, value)}
              onDraftSent={() => updateDraft(thread.id, "")}
              messagePage={messagePages[thread.id] ?? 0}
              onMessagePageChange={(page) => updateMessagePage(thread.id, page)}
              onToggle={() => toggleThread(thread.id)}
            />
          ))}
        </QueryState>
        <PageControls
          label="threads"
          page={threadPage}
          size={THREADS_PAGE_SIZE}
          total={threadsQuery.data?.total ?? 0}
          onPageChange={setThreadPage}
        />
      </motion.section>

      <Dialog
        open={isNewMessageDialogOpen}
        onOpenChange={(open) => {
          setIsNewMessageDialogOpen(open);
          if (!open) {
            resetNewThreadForm();
          }
        }}
      >
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle>New message</DialogTitle>
            <DialogDescription>
              Start a support conversation or a direct message with another user.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-2">
              <Button
                type="button"
                variant={newThreadType === "support" ? "default" : "outline"}
                className="rounded-xl"
                onClick={() => setNewThreadType("support")}
              >
                Support
              </Button>
              <Button
                type="button"
                variant={newThreadType === "direct" ? "default" : "outline"}
                className="rounded-xl"
                onClick={() => setNewThreadType("direct")}
              >
                Direct
              </Button>
            </div>
            {newThreadType === "direct" && (
              <div className="space-y-2">
                <label htmlFor="new-message-recipient" className="text-sm font-medium text-foreground">
                  Recipient user ID
                </label>
                <Input
                  id="new-message-recipient"
                  value={newRecipientUserId}
                  onChange={(event) => setNewRecipientUserId(event.target.value)}
                  placeholder="00000000-0000-0000-0000-000000000000"
                />
              </div>
            )}
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
              disabled={!canCreateThread || isCreatingThread}
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

function MessageThreadItem({
  draft,
  isOpen,
  onDraftChange,
  onDraftSent,
  onMessagePageChange,
  onToggle,
  thread,
  messagePage,
}: {
  draft: string;
  isOpen: boolean;
  messagePage: number;
  onDraftChange: (value: string) => void;
  onDraftSent: () => void;
  onMessagePageChange: (page: number) => void;
  onToggle: () => void;
  thread: ThreadSummaryDto;
}) {
  const messagesQuery = useThreadMessages(
    {
      pageable: {
        page: messagePage,
        size: MESSAGES_PAGE_SIZE,
      },
      threadId: thread.id,
    },
    {
      enabled: isOpen,
    },
  );
  const sendMessage = useSendThreadMessageMutation();
  const markRead = useMarkThreadReadMutation();
  const previewMeta = getDirectionMeta(thread.lastMessageOwn);
  const participantRole = thread.participant.role === "SUPPORT" ? "Support" : "User";

  const handleOpenChange = () => {
    if (!isOpen && thread.unreadCount > 0) {
      markRead.mutate({ threadId: thread.id });
    }
    onToggle();
  };

  const handleSend = () => {
    const text = draft.trim();

    if (!text) {
      return;
    }

    sendMessage.mutate(
      {
        text,
        threadId: thread.id,
      },
      {
        onSuccess: onDraftSent,
      },
    );
  };

  return (
    <Collapsible
      open={isOpen}
      onOpenChange={handleOpenChange}
      className="overflow-hidden rounded-2xl border border-border bg-card shadow-card"
    >
      <div className="border-b border-border/70 px-5 py-4">
        <div className="flex items-start gap-4">
          <UserAvatar
            name={thread.participant.name}
            lastSeenAt={null}
            size="md"
          />
          <div className="min-w-0 flex-1">
            <div className="mb-1 flex flex-wrap items-center gap-2">
              <h2 className="truncate text-sm font-semibold text-foreground">
                {getThreadSubject(thread)}
              </h2>
              {thread.unreadCount > 0 && (
                <span className="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-accent px-1.5 text-[10px] font-mono font-semibold text-accent-foreground">
                  {thread.unreadCount}
                </span>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              {thread.participant.name} · {participantRole} · {getThreadContext(thread)}
            </p>
          </div>
          <div className="flex shrink-0 items-center gap-1.5 text-[10px] text-muted-foreground/70">
            <Clock className="h-3 w-3" />
            {formatTimestamp(thread.updatedAt)}
          </div>
        </div>
      </div>

      <CollapsibleTrigger asChild>
        <button
          type="button"
          className="w-full px-5 py-4 text-left transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          aria-label={`${isOpen ? "Collapse" : "Expand"} thread with ${thread.participant.name}`}
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
                  {thread.lastMessageOwn ? "You" : thread.participant.name}
                </span>
                <span className="text-[10px] text-muted-foreground/70">
                  {formatTimestamp(thread.updatedAt)}
                </span>
              </div>
              {!isOpen && (
                <p className="line-clamp-2 text-sm leading-6 text-foreground/85">
                  {thread.lastMessagePreview}
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
          <QueryState
            isLoading={messagesQuery.isLoading}
            isError={messagesQuery.isError}
            error={messagesQuery.error}
            isEmpty={(messagesQuery.data?.items.length ?? 0) === 0}
            emptyMessage="No messages in this conversation yet."
            onRetry={() => messagesQuery.refetch()}
          >
            <div className="space-y-4">
              {(messagesQuery.data?.items ?? []).map((message) => (
                <MessageBubble key={message.id} message={message} />
              ))}
            </div>
          </QueryState>
          <PageControls
            label={`messages for ${thread.participant.name}`}
            page={messagePage}
            size={MESSAGES_PAGE_SIZE}
            total={messagesQuery.data?.total ?? 0}
            onPageChange={onMessagePageChange}
          />
          <div className="mt-5 border-t border-border/70 pt-4">
            <div className="rounded-2xl border border-border bg-background p-3">
              <Textarea
                value={draft}
                onChange={(event) => onDraftChange(event.target.value)}
                placeholder={`Message ${thread.participant.name}...`}
                className="min-h-[96px] resize-none border-0 px-0 py-0 shadow-none focus-visible:ring-0"
              />
              <div className="mt-3 flex justify-end">
                <Button
                  type="button"
                  size="sm"
                  className="rounded-xl"
                  disabled={!draft.trim() || sendMessage.isPending}
                  onClick={handleSend}
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
}

function PageControls({
  label,
  onPageChange,
  page,
  size,
  total,
}: {
  label: string;
  onPageChange: (page: number) => void;
  page: number;
  size: number;
  total: number;
}) {
  const totalPages = Math.max(1, Math.ceil(total / size));

  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3 text-sm text-muted-foreground">
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="rounded-xl"
        disabled={page <= 0}
        aria-label={`Previous ${label} page`}
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft className="h-4 w-4" />
        Previous
      </Button>
      <span className="font-medium text-foreground">
        {label.charAt(0).toUpperCase() + label.slice(1)} page {page + 1} of {totalPages}
      </span>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="rounded-xl"
        disabled={page >= totalPages - 1}
        aria-label={`Next ${label} page`}
        onClick={() => onPageChange(page + 1)}
      >
        Next
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  );
}

function MessageBubble({ message }: { message: MessageDto }) {
  const messageMeta = getDirectionMeta(message.own);

  return (
    <article
      className={cn(
        "flex gap-3",
        message.own && "flex-row-reverse",
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
            {message.own ? "You" : message.authorName}
          </span>
          <span className="text-[10px] text-muted-foreground/70">
            {formatTimestamp(message.createdAt)}
          </span>
        </div>
        <p className="text-sm leading-6 text-foreground/85">
          {message.text}
        </p>
      </div>
    </article>
  );
}

export default Messages;
