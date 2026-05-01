import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { getApiErrorMessage } from "@/lib/server-state/errors";
import { HttpError } from "@/lib/http-client";

interface QueryStateProps {
  children: ReactNode;
  emptyMessage?: string;
  error?: unknown;
  errorMessage?: string;
  isEmpty?: boolean;
  isError?: boolean;
  isLoading?: boolean;
  loadingMessage?: string;
  onRetry?: () => void;
}

const StateShell = ({
  title,
  description,
  onRetry,
}: {
  title: string;
  description?: string;
  onRetry?: () => void;
}) => (
  <div className="rounded-2xl border border-border bg-card p-6 text-sm text-muted-foreground">
    <p className="font-semibold text-foreground">{title}</p>
    {description && <p className="mt-2 text-sm text-muted-foreground">{description}</p>}
    {onRetry && (
      <Button
        variant="outline"
        size="sm"
        className="mt-4"
        onClick={onRetry}
      >
        Try again
      </Button>
    )}
  </div>
);

export function QueryState({
  children,
  emptyMessage = "Nothing to show yet.",
  error,
  errorMessage,
  isEmpty,
  isError,
  isLoading,
  loadingMessage = "Loading...",
  onRetry,
}: QueryStateProps) {
  if (isLoading) {
    return <StateShell title={loadingMessage} />;
  }

  if (isError) {
    if (error instanceof HttpError && error.status === 403) {
      return <>{children}</>;
    }
    const message = errorMessage ?? getApiErrorMessage(error, "Something went wrong.");
    return <StateShell title="Unable to load" description={message} onRetry={onRetry} />;
  }

  if (isEmpty) {
    return <StateShell title={emptyMessage} />;
  }

  return <>{children}</>;
}
