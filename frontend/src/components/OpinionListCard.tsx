import { Lock, List, CheckCircle2 } from "lucide-react";
import { motion } from "framer-motion";
import AccessRequestButton from "@/components/AccessRequestButton";
import UserAvatar from "@/components/UserAvatar";
import type { Uuid } from "@/lib/api/shared";
import { cn } from "@/lib/utils";

export interface OpinionListCardProps {
  id: Uuid;
  title: string;
  description: string;
  reviewCount: number;
  authorId?: Uuid;
  authorName: string;
  hasAccess: boolean;
  accessStatus?: "none" | "pending" | "approved" | "declined";
  showAccessBadge?: boolean;
  className?: string;
  onClick?: () => void;
}

const OpinionListCard = ({
  id,
  title,
  description,
  reviewCount,
  authorId,
  authorName,
  hasAccess,
  accessStatus = "none",
  showAccessBadge = false,
  className,
  onClick,
}: OpinionListCardProps) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className={cn(
        "group relative cursor-pointer rounded-2xl border border-border bg-card p-5 transition-shadow hover:shadow-premium flex flex-col h-full",
        className
      )}
      onClick={onClick}
    >
      <div className="mb-3 flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/8">
          <List className="h-4 w-4 text-primary" />
        </div>
        <div className="flex-1 min-w-0">
          <h4 className="truncate font-medium text-card-foreground font-body text-sm">{title}</h4>
        </div>
        {!hasAccess && <Lock className="h-3.5 w-3.5 text-muted-foreground/40 shrink-0" />}
      </div>

      <p className="mb-3 text-xs text-muted-foreground line-clamp-2 leading-relaxed flex-1">{description}</p>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <UserAvatar userId={authorId} name={authorName} size="sm" />
          <div className="text-xs text-muted-foreground">
            <span className="font-medium text-card-foreground">{authorName}</span>
            <span className="mx-1">·</span>
            <span className="font-mono">{reviewCount}</span> reviews
          </div>
        </div>
      </div>

      {!hasAccess && (
        <div className="mt-3 pt-3 border-t border-border">
          <AccessRequestButton
            listId={id}
            status={accessStatus}
            listTitle={title}
            className="w-full text-xs"
          />
        </div>
      )}

      {hasAccess && showAccessBadge && (
        <div className="mt-3 pt-3 border-t border-border flex items-center justify-center gap-1.5 text-xs text-primary">
          <CheckCircle2 className="h-3.5 w-3.5" />
          <span className="font-medium">Access Approved</span>
        </div>
      )}
    </motion.div>
  );
};

export default OpinionListCard;
