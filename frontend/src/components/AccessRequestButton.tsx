import { Lock, Clock, Check, XCircle, Copy as CopyIcon, FolderInput } from "lucide-react";
import { motion } from "framer-motion";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useCreateOutgoingAccessRequestMutation } from "@/lib/server-state/hooks/access-requests";
import { useMyOpinionLists } from "@/lib/server-state/hooks/opinion-lists";
import type { AccessRequestIntentDto } from "@/lib/api/access-requests";
import type { Uuid } from "@/lib/api/shared";
import { cn } from "@/lib/utils";

interface AccessRequestButtonProps {
  listId: Uuid;
  status?: "none" | "pending" | "approved" | "declined";
  listTitle?: string;
  className?: string;
}

const AccessRequestButton = ({
  listId,
  status = "none",
  listTitle = "this list",
  className,
}: AccessRequestButtonProps) => {
  const [currentStatus, setCurrentStatus] = useState(status);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [intent, setIntent] = useState<AccessRequestIntentDto>("COPY");
  const [targetListId, setTargetListId] = useState<string>("");
  const { data: myListsData } = useMyOpinionLists(
    { pageable: { page: 0, size: 50 } },
    { enabled: dialogOpen && intent === "MERGE" },
  );
  const myLists = (myListsData?.items ?? []).filter((l) => l.listId !== listId);
  const createRequest = useCreateOutgoingAccessRequestMutation({
    onSuccess: () => {
      setCurrentStatus("pending");
      setDialogOpen(false);
      setIntent("COPY");
      setTargetListId("");
    },
  });

  const canSubmit =
    !createRequest.isPending && (intent !== "MERGE" || targetListId.length > 0);

  if (currentStatus === "approved") {
    return (
      <Button variant="ghost" size="sm" className={className} disabled>
        <Check className="mr-1.5 h-3 w-3" />
        Access Granted
      </Button>
    );
  }

  if (currentStatus === "declined") {
    return (
      <Button variant="ghost" size="sm" className={className} disabled>
        <XCircle className="mr-1.5 h-3 w-3 text-destructive" />
        <span className="text-destructive">Declined</span>
      </Button>
    );
  }

  if (currentStatus === "pending") {
    return (
      <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }}>
        <Button variant="secondary" size="sm" className={className} disabled>
          <Clock className="mr-1.5 h-3 w-3" />
          Request Sent
        </Button>
      </motion.div>
    );
  }

  return (
    <>
      <motion.div whileTap={{ scale: 0.98 }}>
        <Button
          variant="access"
          size="sm"
          className={className}
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setDialogOpen(true);
          }}
        >
          <Lock className="mr-1.5 h-3 w-3" />
          Request Access
        </Button>
      </motion.div>
      <Dialog open={dialogOpen} onOpenChange={(v) => { if (!v) setDialogOpen(false); }}>
        <DialogContent
          className="sm:max-w-md"
          onClick={(e) => e.stopPropagation()}
        >
          <DialogHeader>
            <DialogTitle>Request access</DialogTitle>
            <DialogDescription>
              Once <span className="font-medium text-foreground">{listTitle}</span>'s owner accepts, what should happen?
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2 py-2">
            <IntentChoice
              icon={<CopyIcon className="h-4 w-4 text-primary" />}
              title="Copy as a new list"
              description="A new private list will be created and synced with this one. Their opinions appear with their weights."
              selected={intent === "COPY"}
              onClick={() => setIntent("COPY")}
            />
            <IntentChoice
              icon={<FolderInput className="h-4 w-4 text-primary" />}
              title="Merge into one of mine"
              description="Sync this list into one of your existing lists."
              selected={intent === "MERGE"}
              onClick={() => setIntent("MERGE")}
            />
            {intent === "MERGE" && (
              <div className="pl-1">
                <label className="text-xs font-medium text-muted-foreground mb-1 block">Pick a list</label>
                {myLists.length === 0 ? (
                  <p className="text-xs text-muted-foreground">You don't have any other lists yet.</p>
                ) : (
                  <select
                    value={targetListId}
                    onChange={(e) => setTargetListId(e.target.value)}
                    className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
                  >
                    <option value="">Pick a list…</option>
                    {myLists.map((l) => (
                      <option key={l.listId} value={l.listId}>{l.listName}</option>
                    ))}
                  </select>
                )}
              </div>
            )}
          </div>
          <DialogFooter>
            <Button
              variant="ghost"
              size="sm"
              onClick={(e) => { e.stopPropagation(); setDialogOpen(false); }}
              disabled={createRequest.isPending}
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={(e) => {
                e.stopPropagation();
                createRequest.mutate({
                  listId,
                  intent,
                  targetListId: intent === "MERGE" ? (targetListId as Uuid) : undefined,
                });
              }}
              disabled={!canSubmit}
            >
              Send request
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

const IntentChoice = ({
  icon,
  title,
  description,
  selected,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  selected: boolean;
  onClick: () => void;
}) => (
  <button
    type="button"
    onClick={onClick}
    className={cn(
      "flex w-full items-start gap-3 rounded-xl border p-3 text-left transition-colors",
      selected
        ? "border-primary bg-primary/5"
        : "border-border hover:bg-secondary/50",
    )}
  >
    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary/8">
      {icon}
    </div>
    <div className="flex-1 min-w-0">
      <p className="text-sm font-medium text-foreground">{title}</p>
      <p className="text-[11px] text-muted-foreground mt-0.5">{description}</p>
    </div>
  </button>
);

export default AccessRequestButton;
