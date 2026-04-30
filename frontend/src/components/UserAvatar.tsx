import { useState } from "react";
import ReviewerAvatar from "@/components/ReviewerAvatar";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card";
import { useObjectUrl } from "@/hooks/use-object-url";
import type { Uuid } from "@/lib/api/shared";
import { useUserAvatar, useUserProfile } from "@/lib/server-state/hooks/users";

interface UserAvatarProps {
  userId?: Uuid | null;
  name: string;
  lastSeenAt?: string | null;
  size?: "sm" | "md" | "lg";
  className?: string;
  interactive?: boolean;
}

const UserAvatar = ({
  userId,
  name,
  lastSeenAt,
  size = "md",
  className,
  interactive = true,
}: UserAvatarProps) => {
  if (userId) {
    return (
      <ProfileUserAvatar
        userId={userId}
        name={name}
        fallbackLastSeenAt={lastSeenAt}
        size={size}
        className={className}
        interactive={interactive}
      />
    );
  }

  if (interactive && lastSeenAt !== undefined) {
    return (
      <PresenceAvatar
        name={name}
        lastSeenAt={lastSeenAt}
        size={size}
        className={className}
      />
    );
  }

  return <ReviewerAvatar name={name} size={size} className={className} />;
};

interface ProfileUserAvatarProps {
  userId: Uuid;
  name: string;
  fallbackLastSeenAt?: string | null;
  size: "sm" | "md" | "lg";
  className?: string;
  interactive: boolean;
}

function ProfileUserAvatar({
  userId,
  name,
  fallbackLastSeenAt,
  size,
  className,
  interactive,
}: ProfileUserAvatarProps) {
  const { data: avatar } = useUserAvatar(userId ?? "", {
    enabled: true,
    retry: false,
  });
  const [isPresenceOpen, setIsPresenceOpen] = useState(false);
  const { data: profile, isError, isLoading } = useUserProfile(userId ?? "", {
    enabled: isPresenceOpen,
    retry: false,
    staleTime: 60_000,
  });
  const avatarUrl = useObjectUrl(avatar);

  if (!interactive) {
    return (
      <ReviewerAvatar
        name={name}
        image={avatarUrl}
        size={size}
        className={className}
      />
    );
  }

  return (
    <PresenceAvatar
      name={name}
      displayName={profile?.name}
      image={avatarUrl}
      lastSeenAt={profile?.lastSeenAt ?? fallbackLastSeenAt}
      isError={isError}
      isLoading={isLoading}
      isOpen={isPresenceOpen}
      onOpenChange={setIsPresenceOpen}
      size={size}
      className={className}
    />
  );
}

interface PresenceAvatarProps {
  name: string;
  displayName?: string;
  image?: string;
  lastSeenAt?: string | null;
  isLoading?: boolean;
  isError?: boolean;
  isOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  size: "sm" | "md" | "lg";
  className?: string;
}

function PresenceAvatar({
  name,
  displayName,
  image,
  lastSeenAt,
  isLoading = false,
  isError = false,
  isOpen,
  onOpenChange,
  size,
  className,
}: PresenceAvatarProps) {
  const [uncontrolledOpen, setUncontrolledOpen] = useState(false);
  const open = isOpen ?? uncontrolledOpen;
  const setOpen = onOpenChange ?? setUncontrolledOpen;

  const avatarElement = (
    <ReviewerAvatar
      name={name}
      image={image}
      size={size}
      className={className}
    />
  );

  return (
    <HoverCard open={open} onOpenChange={setOpen} openDelay={150}>
      <HoverCardTrigger asChild>
        <button
          type="button"
          className="inline-flex rounded-full border-0 bg-transparent p-0 outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          aria-label={`${name} presence`}
        >
          {avatarElement}
        </button>
      </HoverCardTrigger>
      <HoverCardContent side="top" align="center" className="w-52 p-3">
        <div className="space-y-1">
          <p className="truncate text-sm font-medium text-foreground">{displayName ?? name}</p>
          <p className="text-xs text-muted-foreground">
            {getPresenceText(lastSeenAt, isLoading, isError)}
          </p>
        </div>
      </HoverCardContent>
    </HoverCard>
  );
}

function getPresenceText(
  lastSeenAt: string | null | undefined,
  isLoading: boolean,
  isError: boolean,
): string {
  if (isLoading) {
    return "Loading last seen...";
  }

  if (isError || !lastSeenAt) {
    return "Last seen unavailable";
  }

  const seenAt = new Date(lastSeenAt).getTime();
  if (Number.isNaN(seenAt)) {
    return "Last seen unavailable";
  }

  const minutes = Math.max(0, Math.floor((Date.now() - seenAt) / 60_000));
  if (minutes < 5) {
    return "Last seen recently";
  }
  if (minutes < 60) {
    return `Last seen ${formatTimeAgo(minutes, "minute")} ago`;
  }

  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `Last seen ${formatTimeAgo(hours, "hour")} ago`;
  }

  const days = Math.floor(hours / 24);
  if (days === 1) {
    return "Last seen yesterday";
  }
  if (days < 30) {
    return `Last seen ${formatTimeAgo(days, "day")} ago`;
  }

  return `Last seen on ${new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
  }).format(new Date(seenAt))}`;
}

function formatTimeAgo(value: number, unit: "minute" | "hour" | "day"): string {
  return `${value} ${unit}${value === 1 ? "" : "s"}`;
}

export default UserAvatar;
