import { useState } from "react";
import { motion } from "framer-motion";
import { Check, Clock, XCircle, Eye, EyeOff, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import UserAvatar from "@/components/UserAvatar";
import { QueryState } from "@/components/server-state/QueryState";
import {
  useIncomingAccessRequests,
  useOutgoingAccessRequests,
  useAcceptIncomingAccessRequestMutation,
  useDeclineIncomingAccessRequestMutation,
  useHideIncomingAccessRequestMutation,
  useCancelOutgoingAccessRequestMutation,
} from "@/lib/server-state/hooks/access-requests";
import type { RequestStatusEnumDto } from "@/lib/api/access-requests";

function formatRelativeTime(timestamp: string): string {
  const diff = Date.now() - new Date(timestamp).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return `${Math.floor(days / 7)}w ago`;
}

const STATUS_LABELS: Record<RequestStatusEnumDto, { label: string; icon: React.ReactNode; className: string }> = {
  SENT: { label: "Pending", icon: <Clock className="h-3 w-3" />, className: "text-muted-foreground" },
  ACCEPTED: { label: "Approved", icon: <Check className="h-3 w-3" />, className: "text-primary" },
  REJECTED: { label: "Declined", icon: <XCircle className="h-3 w-3" />, className: "text-destructive" },
  HIDDEN: { label: "Hidden", icon: <EyeOff className="h-3 w-3" />, className: "text-muted-foreground" },
  CANCELLED: { label: "Cancelled", icon: <XCircle className="h-3 w-3" />, className: "text-muted-foreground" },
};

const Requests = () => {
  const [showHidden, setShowHidden] = useState(false);
  const [showApproved, setShowApproved] = useState(false);

  const incoming = useIncomingAccessRequests({
    filters: { status: "SENT" },
    pageable: { page: 0, size: 50 },
  });

  const hiddenIncoming = useIncomingAccessRequests({
    filters: { status: "HIDDEN" },
    pageable: { page: 0, size: 50 },
  });

  const approvedIncoming = useIncomingAccessRequests({
    filters: { status: "ACCEPTED" },
    pageable: { page: 0, size: 50 },
  });

  const outgoing = useOutgoingAccessRequests({
    pageable: { page: 0, size: 50 },
  });

  const accept = useAcceptIncomingAccessRequestMutation();
  const decline = useDeclineIncomingAccessRequestMutation();
  const hide = useHideIncomingAccessRequestMutation();
  const cancel = useCancelOutgoingAccessRequestMutation();

  const incomingItems = incoming.data?.items ?? [];
  const hiddenItems = hiddenIncoming.data?.items ?? [];
  const approvedItems = approvedIncoming.data?.items ?? [];
  const outgoingItems = outgoing.data?.items ?? [];

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-10"
      >
        <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-foreground mb-1">Access Requests</h1>
        <p className="text-sm text-muted-foreground">Manage who can access your lists.</p>
      </motion.div>

      {/* Incoming */}
      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.1 }}
        className="mb-12"
      >
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold tracking-tight text-foreground">
            Incoming
            {(incoming.data?.total ?? 0) > 0 && (
              <span className="ml-2 inline-flex h-5 w-5 items-center justify-center rounded-full bg-accent text-[10px] font-mono font-semibold text-accent-foreground">
                {incoming.data?.total}
              </span>
            )}
          </h2>
          <div className="flex items-center gap-2">
            {approvedItems.length > 0 && (
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-xs text-muted-foreground gap-1.5"
                onClick={() => setShowApproved(!showApproved)}
              >
                {showApproved ? <EyeOff className="h-3 w-3" /> : <Check className="h-3 w-3" />}
                {showApproved ? "Hide access granted" : "Show access granted"} ({approvedItems.length})
              </Button>
            )}
            {hiddenItems.length > 0 && (
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-xs text-muted-foreground gap-1.5"
                onClick={() => setShowHidden(!showHidden)}
              >
                {showHidden ? <EyeOff className="h-3 w-3" /> : <Eye className="h-3 w-3" />}
                {showHidden ? "Hide hidden" : "Show hidden"} ({hiddenItems.length})
              </Button>
            )}
          </div>
        </div>

        <QueryState
          isLoading={incoming.isLoading}
          isError={incoming.isError}
          error={incoming.error}
          isEmpty={incomingItems.length === 0}
          emptyMessage="No pending incoming requests."
          onRetry={incoming.refetch}
        >
          <div className="rounded-2xl border border-border overflow-hidden">
            {incomingItems.map((req) => (
              <div
                key={req.id}
                className="flex items-center gap-4 border-b border-border last:border-0 px-5 py-4"
              >
                <UserAvatar userId={req.requester} name={req.requesterName} size="sm" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-foreground truncate">{req.requesterName}</p>
                  <p className="text-xs text-muted-foreground truncate">
                    Wants access to: <span className="font-medium text-foreground/70">{req.opinionListName}</span>
                  </p>
                </div>
                <span className="text-[10px] text-muted-foreground/60 shrink-0">{formatRelativeTime(req.timestamp)}</span>
                <div className="flex gap-2 shrink-0">
                  <Button
                    variant="default"
                    size="sm"
                    className="h-8 rounded-lg text-xs"
                    disabled={accept.isPending}
                    onClick={() => accept.mutate({ requestId: req.id })}
                  >
                    <Check className="mr-1 h-3 w-3" />
                    Approve
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 rounded-lg text-xs text-muted-foreground"
                    disabled={hide.isPending}
                    onClick={() => hide.mutate({ requestId: req.id })}
                  >
                    <EyeOff className="mr-1 h-3 w-3" />
                    Hide
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 rounded-lg text-xs text-destructive hover:text-destructive"
                    disabled={decline.isPending}
                    onClick={() => decline.mutate({ requestId: req.id })}
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </QueryState>

        {showHidden && hiddenItems.length > 0 && (
          <div className="mt-6">
            <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground/50 mb-2 px-1">Hidden</p>
            <div className="rounded-2xl border border-dashed border-border overflow-hidden opacity-60">
              {hiddenItems.map((req) => (
                <div
                  key={req.id}
                  className="flex items-center gap-4 border-b border-border last:border-0 px-5 py-4"
                >
                  <UserAvatar userId={req.requester} name={req.requesterName} size="sm" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">{req.requesterName}</p>
                    <p className="text-xs text-muted-foreground truncate">
                      Wants access to: <span className="font-medium text-foreground/70">{req.opinionListName}</span>
                    </p>
                  </div>
                  <span className="text-[10px] text-muted-foreground/60 shrink-0">{formatRelativeTime(req.timestamp)}</span>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 rounded-lg text-xs text-muted-foreground"
                    disabled={accept.isPending}
                    onClick={() => accept.mutate({ requestId: req.id })}
                  >
                    <Check className="mr-1 h-3 w-3" />
                    Approve
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 w-8 rounded-lg p-0 text-destructive hover:text-destructive"
                    disabled={decline.isPending}
                    onClick={() => decline.mutate({ requestId: req.id })}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              ))}
            </div>
          </div>
        )}

        {showApproved && approvedItems.length > 0 && (
          <div className="mt-6">
            <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground/50 mb-2 px-1">Access granted</p>
            <div className="rounded-2xl border border-dashed border-border overflow-hidden opacity-70">
              {approvedItems.map((req) => (
                <div
                  key={req.id}
                  className="flex items-center gap-4 border-b border-border last:border-0 px-5 py-4"
                >
                  <UserAvatar userId={req.requester} name={req.requesterName} size="sm" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">{req.requesterName}</p>
                    <p className="text-xs text-muted-foreground truncate">
                      Access granted to: <span className="font-medium text-foreground/70">{req.opinionListName}</span>
                    </p>
                  </div>
                  <span className="text-[10px] text-muted-foreground/60 shrink-0">{formatRelativeTime(req.timestamp)}</span>
                  <span className="inline-flex items-center gap-1 text-xs text-primary shrink-0">
                    <Check className="h-3 w-3" />
                    Approved
                  </span>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="h-8 rounded-lg text-xs text-destructive hover:text-destructive"
                    disabled={decline.isPending}
                    onClick={() => decline.mutate({ requestId: req.id })}
                  >
                    <XCircle className="mr-1 h-3 w-3" />
                    Revoke
                  </Button>
                </div>
              ))}
            </div>
          </div>
        )}
      </motion.section>

      {/* Outgoing */}
      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.15 }}
      >
        <h2 className="mb-4 text-lg font-semibold tracking-tight text-foreground">Outgoing</h2>
        <QueryState
          isLoading={outgoing.isLoading}
          isError={outgoing.isError}
          error={outgoing.error}
          isEmpty={outgoingItems.length === 0}
          emptyMessage="No outgoing requests."
          onRetry={outgoing.refetch}
        >
          <div className="rounded-2xl border border-border overflow-hidden">
            {outgoingItems.map((req) => {
              const statusMeta = STATUS_LABELS[req.status];
              return (
                <div
                  key={req.id}
                  className="flex items-center gap-4 border-b border-border last:border-0 px-5 py-4"
                >
                  <UserAvatar userId={req.owner} name={req.ownerName} size="sm" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground truncate">{req.ownerName}</p>
                    <p className="text-xs text-muted-foreground truncate">{req.opinionListName}</p>
                  </div>
                  <span className="text-[10px] text-muted-foreground/60 shrink-0">{formatRelativeTime(req.timestamp)}</span>
                  <div className="shrink-0 flex items-center gap-2">
                    <span className={`inline-flex items-center gap-1 text-xs ${statusMeta.className}`}>
                      {statusMeta.icon}
                      {statusMeta.label}
                    </span>
                    {req.status === "SENT" && (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 rounded-lg text-xs text-muted-foreground"
                        disabled={cancel.isPending}
                        onClick={() => cancel.mutate({ requestId: req.id })}
                      >
                        Cancel
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </QueryState>
      </motion.section>
    </div>
  );
};

export default Requests;
