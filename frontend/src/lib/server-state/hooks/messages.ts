import type { QueryKey, UseMutationOptions, UseQueryOptions } from "@tanstack/react-query";
import { messagesApi } from "@/lib/api";
import type {
  AddThreadMessageRequestDto,
  CreateDirectThreadRequestDto,
  CreateSupportThreadRequestDto,
  GetMessageThreadsRequestDto,
  GetThreadMessagesRequestDto,
  MarkThreadReadRequestDto,
  MessageDto,
  ThreadSummaryDto,
} from "@/lib/api/messages";
import type { PageResponseDto } from "@/lib/api/shared";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";
import { messagesKeys } from "@/lib/server-state/query-keys";
import { useApiInvalidation, useAuthorizedMutation, useAuthorizedQuery } from "@/lib/server-state/hooks/authorized";

export function useMessageThreads(
  request: GetMessageThreadsRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<ThreadSummaryDto>,
      Error,
      PageResponseDto<ThreadSummaryDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: messagesKeys.threads(request),
    queryFn: () => messagesApi.getThreads(request),
    ...options,
  });
}

export function useThreadMessages(
  request: GetThreadMessagesRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<MessageDto>,
      Error,
      PageResponseDto<MessageDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: messagesKeys.threadMessages(request),
    queryFn: () => messagesApi.getThreadMessages(request),
    ...options,
  });
}

export function useCreateSupportThreadMutation(
  options?: UseMutationOptions<
    ThreadSummaryDto,
    Error,
    CreateSupportThreadRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => messagesApi.createSupportThread(variables),
    ...options,
    meta: {
      errorTitle: "Thread creation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagesKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useCreateDirectThreadMutation(
  options?: UseMutationOptions<
    ThreadSummaryDto,
    Error,
    CreateDirectThreadRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => messagesApi.createDirectThread(variables),
    ...options,
    meta: {
      errorTitle: "Thread creation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagesKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useSendThreadMessageMutation(
  options?: UseMutationOptions<
    MessageDto,
    Error,
    AddThreadMessageRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => messagesApi.addThreadMessage(variables),
    ...options,
    meta: {
      errorTitle: "Message send failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagesKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useMarkThreadReadMutation(
  options?: UseMutationOptions<
    ThreadSummaryDto,
    Error,
    MarkThreadReadRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => messagesApi.markThreadRead(variables),
    ...options,
    meta: {
      errorTitle: "Read marker update failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagesKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}
