import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { Check, X } from "lucide-react";
import RatingBadge from "@/components/RatingBadge";
import { QueryState } from "@/components/server-state/QueryState";
import UserAvatar from "@/components/UserAvatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import type {
  GetModerationDecisionsRequestDto,
  GetModerationOpinionsRequestDto,
  ModerationDecisionDto,
  OpinionDto,
} from "@/lib/api/opinions";
import {
  useApproveOpinionMutation,
  useModerationDecisions,
  useModerationOpinions,
  useRejectOpinionMutation,
} from "@/lib/server-state/hooks/opinions";
import { cn } from "@/lib/utils";

const MODERATION_QUEUE_REQUEST = {
  pageable: {
    page: 0,
    size: 50,
    sort: [],
  },
} satisfies GetModerationOpinionsRequestDto;

const MODERATION_DECISIONS_REQUEST = {
  pageable: {
    page: 0,
    size: 20,
    sort: [],
  },
} satisfies GetModerationDecisionsRequestDto;

const EMPTY_OPINIONS: OpinionDto[] = [];
const EMPTY_DECISIONS: ModerationDecisionDto[] = [];

function formatRelativeTime(timestamp: string): string {
  const createdAt = new Date(timestamp).getTime();

  if (Number.isNaN(createdAt)) {
    return "Submitted recently";
  }

  const diffMs = Math.max(0, Date.now() - createdAt);
  const minutes = Math.floor(diffMs / 60_000);

  if (minutes < 1) {
    return "Just now";
  }

  if (minutes < 60) {
    return `${minutes}m ago`;
  }

  const hours = Math.floor(minutes / 60);

  if (hours < 24) {
    return `${hours}h ago`;
  }

  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function formatOpinionText(items: readonly string[], fallback: string): string {
  const values = items.map((item) => item.trim()).filter(Boolean);
  return values.length > 0 ? values.join(", ") : fallback;
}

const OpinionRating = ({ opinion }: { opinion: OpinionDto }) => {
  if (opinion.mark === null) {
    return (
      <Badge variant="outline" className="border-border text-muted-foreground">
        No rating
      </Badge>
    );
  }

  return <RatingBadge value={opinion.mark} />;
};

const OpinionModeration = () => {
  const moderation = useModerationOpinions(MODERATION_QUEUE_REQUEST);
  const decisions = useModerationDecisions(MODERATION_DECISIONS_REQUEST);
  const approveOpinion = useApproveOpinionMutation();
  const rejectOpinion = useRejectOpinionMutation();
  const [rejectingOpinionId, setRejectingOpinionId] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState("");
  const [rejectionError, setRejectionError] = useState<string | null>(null);

  const pendingOpinions = moderation.data?.items ?? EMPTY_OPINIONS;
  const recentDecisions = decisions.data?.items ?? EMPTY_DECISIONS;
  const isMutating = approveOpinion.isPending || rejectOpinion.isPending;
  const rejectingOpinion = useMemo(
    () => pendingOpinions.find((opinion) => opinion.id === rejectingOpinionId) ?? null,
    [pendingOpinions, rejectingOpinionId],
  );

  const closeRejectDialog = () => {
    if (rejectOpinion.isPending) {
      return;
    }

    setRejectingOpinionId(null);
    setRejectionReason("");
    setRejectionError(null);
  };

  const handleApprove = async (opinionId: string) => {
    try {
      await approveOpinion.mutateAsync({ opinionId });
    } catch {
      // Mutation errors are surfaced by the shared API error toast.
    }
  };

  const handleReject = async () => {
    if (!rejectingOpinion) {
      return;
    }

    const trimmedReason = rejectionReason.trim();
    if (!trimmedReason) {
      setRejectionError("Rejection reason is required.");
      return;
    }

    try {
      await rejectOpinion.mutateAsync({
        opinionId: rejectingOpinion.id,
        reason: trimmedReason,
      });
      closeRejectDialog();
    } catch {
      // Mutation errors are surfaced by the shared API error toast.
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-10"
      >
        <h1 className="mb-2 text-3xl font-semibold tracking-tight text-foreground md:text-4xl">
          Opinion Moderation
        </h1>
        <p className="max-w-3xl text-sm text-muted-foreground">
          Review submitted opinions before publication. Reject actions always require a reason so
          the author can revise the text safely.
        </p>
      </motion.div>

      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.1 }}
      >
        <div className="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold tracking-tight text-foreground">Queue</h2>
            <p className="text-sm text-muted-foreground">
              Opinions that were submitted for manual review.
            </p>
          </div>
          <Badge variant="outline" className="rounded-full border-border px-3 py-1 text-xs font-medium">
            {moderation.data?.total ?? pendingOpinions.length} pending
          </Badge>
        </div>

        <QueryState
          isLoading={moderation.isLoading}
          isError={moderation.isError}
          error={moderation.error}
          onRetry={() => void moderation.refetch()}
          isEmpty={pendingOpinions.length === 0}
          loadingMessage="Loading moderation queue..."
          emptyMessage="No opinions are waiting for review."
        >
          <div className="space-y-4">
            {pendingOpinions.map((opinion) => (
              <Card key={opinion.id} className="overflow-hidden rounded-2xl border-border">
                <CardContent className="p-0">
                  <div className="border-b border-border px-5 py-4 md:px-6">
                    <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                      <div className="flex items-start gap-3">
                        <UserAvatar name={opinion.ownerName} size="md" />
                        <div className="min-w-0">
                          <div className="mb-2 flex flex-wrap items-center gap-2">
                            <h3 className="text-base font-semibold text-foreground">{opinion.subjectName}</h3>
                            <Badge variant="outline" className="border-border text-muted-foreground">
                              Pending review
                            </Badge>
                          </div>
                          <p className="text-sm text-muted-foreground">
                            Submitted by {opinion.ownerName} · {formatRelativeTime(opinion.timestamp)}
                          </p>
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center gap-2 md:justify-end">
                        <OpinionRating opinion={opinion} />
                      </div>
                    </div>
                  </div>

                  <div className="px-5 py-5 md:px-6">
                    <div className="space-y-4">
                      <div>
                        <p className="mb-1 text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">
                          Subjective opinion
                        </p>
                        <p className="text-sm leading-relaxed text-foreground/85">
                          {formatOpinionText(opinion.subjective, "No subjective opinion provided.")}
                        </p>
                      </div>
                      <div>
                        <p className="mb-1 text-xs font-semibold uppercase tracking-[0.14em] text-muted-foreground">
                          Objective facts
                        </p>
                        <p className="text-sm leading-relaxed text-muted-foreground">
                          {formatOpinionText(opinion.objective, "No objective facts provided.")}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col gap-2 border-t border-border bg-muted/20 px-5 py-4 md:flex-row md:justify-end md:px-6">
                    <Button
                      variant="outline"
                      className="border-destructive/20 text-destructive hover:bg-destructive/5 hover:text-destructive"
                      disabled={isMutating}
                      onClick={() => {
                        setRejectingOpinionId(opinion.id);
                        setRejectionError(null);
                      }}
                    >
                      <X className="h-4 w-4" />
                      Reject
                    </Button>
                    <Button
                      disabled={isMutating}
                      onClick={() => void handleApprove(opinion.id)}
                    >
                      <Check className="h-4 w-4" />
                      Approve
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </QueryState>
      </motion.section>

      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.15 }}
        className="mt-12"
      >
        <div className="mb-4 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold tracking-tight text-foreground">Recent decisions</h2>
            <p className="text-sm text-muted-foreground">
              Stored moderation actions from the current backend database.
            </p>
          </div>
          <Badge variant="outline" className="rounded-full border-border px-3 py-1 text-xs font-medium">
            {decisions.data?.total ?? recentDecisions.length} logged
          </Badge>
        </div>

        <QueryState
          isLoading={decisions.isLoading}
          isError={decisions.isError}
          error={decisions.error}
          onRetry={() => void decisions.refetch()}
          isEmpty={recentDecisions.length === 0}
          loadingMessage="Loading moderation decisions..."
          emptyMessage="No moderation decisions have been recorded yet."
        >
          <div className="space-y-3">
            {recentDecisions.map((decision) => (
              <Card key={decision.id} className="rounded-2xl border-border">
                <CardContent className="flex flex-col gap-4 px-5 py-4 md:flex-row md:items-start md:justify-between md:px-6">
                  <div className="min-w-0">
                    <div className="mb-1 flex flex-wrap items-center gap-2">
                      <p className="text-sm font-semibold text-foreground">{decision.subjectName}</p>
                      <Badge
                        className={cn(
                          decision.action === "APPROVED"
                            ? "bg-primary/10 text-primary hover:bg-primary/10"
                            : "bg-destructive/10 text-destructive hover:bg-destructive/10",
                        )}
                      >
                        {decision.action === "APPROVED" ? "Approved" : "Rejected"}
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {decision.ownerName} · moderated by {decision.moderatorName} ·{" "}
                      {formatRelativeTime(decision.createdAt)}
                    </p>
                    {decision.reason && (
                      <p className="mt-3 text-sm leading-relaxed text-foreground/85">
                        <span className="font-medium text-foreground">Reason:</span> {decision.reason}
                      </p>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </QueryState>
      </motion.section>

      <Dialog open={Boolean(rejectingOpinion)} onOpenChange={(open) => !open && closeRejectDialog()}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Reject opinion</DialogTitle>
            <DialogDescription>
              Add a clear reason so the author can revise{" "}
              <span className="font-medium text-foreground">{rejectingOpinion?.subjectName}</span> safely.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-2">
            <label htmlFor="rejection-reason" className="text-sm font-medium text-foreground">
              Rejection reason
            </label>
            <Textarea
              id="rejection-reason"
              value={rejectionReason}
              disabled={rejectOpinion.isPending}
              onChange={(event) => {
                setRejectionReason(event.target.value);
                if (rejectionError) {
                  setRejectionError(null);
                }
              }}
              placeholder="Explain what must be changed before this opinion can be approved."
              className={cn(rejectionError && "border-destructive focus-visible:ring-destructive")}
            />
            {rejectionError && <p className="text-sm text-destructive">{rejectionError}</p>}
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              disabled={rejectOpinion.isPending}
              onClick={closeRejectDialog}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={rejectOpinion.isPending}
              onClick={() => void handleReject()}
            >
              Reject with reason
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default OpinionModeration;
