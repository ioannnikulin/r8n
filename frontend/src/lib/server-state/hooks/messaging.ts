import type { QueryKey, UseMutationOptions, UseQueryOptions } from "@tanstack/react-query";
import { messagingApi } from "@/lib/api";
import type {
  AddDirectConversationMessageRequestDto,
  AddSupportThreadMessageRequestDto,
  CreateDirectConversationRequestDto,
  CreateSupportThreadRequestDto,
  DirectConversationSummaryDto,
  DirectMessageDto,
  GetDirectConversationMessagesRequestDto,
  GetDirectConversationSummariesRequestDto,
  GetSupportThreadMessagesRequestDto,
  GetSupportThreadSummariesRequestDto,
  SupportMessageDto,
  SupportThreadSummaryDto,
  UnreadMessagesCountDto,
} from "@/lib/api/messaging";
import type { PageResponseDto, Uuid } from "@/lib/api/shared";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";
import { messagingKeys } from "@/lib/server-state/query-keys";
import {
  useApiInvalidation,
  useAuthorizedMutation,
  useAuthorizedQuery,
} from "@/lib/server-state/hooks/authorized";

export function useSupportThreadSummaries(
  request: GetSupportThreadSummariesRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<SupportThreadSummaryDto>,
      Error,
      PageResponseDto<SupportThreadSummaryDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: messagingKeys.supportThreads(request),
    queryFn: () => messagingApi.getSupportThreadSummaries(request),
    ...options,
  });
}

export function useDirectConversationSummaries(
  request: GetDirectConversationSummariesRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<DirectConversationSummaryDto>,
      Error,
      PageResponseDto<DirectConversationSummaryDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: messagingKeys.directConversations(request),
    queryFn: () => messagingApi.getDirectConversationSummaries(request),
    ...options,
  });
}

export function useDirectConversationMessages(
  request: GetDirectConversationMessagesRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<DirectMessageDto>,
      Error,
      PageResponseDto<DirectMessageDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: messagingKeys.directConversationMessages(request),
    queryFn: () => messagingApi.getDirectConversationMessages(request),
    enabled: Boolean(request.conversationId),
    ...options,
  });
}

export function useCreateDirectConversationMutation(
  options?: UseMutationOptions<
    DirectConversationSummaryDto,
    Error,
    CreateDirectConversationRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => messagingApi.createDirectConversation(variables),
    ...options,
    meta: {
      errorTitle: "Direct conversation creation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagingKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useAddDirectConversationMessageMutation(
  options?: UseMutationOptions<
    DirectMessageDto,
    Error,
    AddDirectConversationMessageRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => messagingApi.addDirectConversationMessage(variables),
    ...options,
    meta: {
      errorTitle: "Direct message send failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagingKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useSupportThreadMessages(
  request: GetSupportThreadMessagesRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<SupportMessageDto>,
      Error,
      PageResponseDto<SupportMessageDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: messagingKeys.supportThreadMessages(request),
    queryFn: () => messagingApi.getSupportThreadMessages(request),
    enabled: Boolean(request.threadId),
    ...options,
  });
}

export function useCreateSupportThreadMutation(
  options?: UseMutationOptions<
    SupportThreadSummaryDto,
    Error,
    CreateSupportThreadRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => messagingApi.createSupportThread(variables),
    ...options,
    meta: {
      errorTitle: "Support thread creation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagingKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useUnreadMessagesCount(options?: { refetchInterval?: number }) {
  const query = useAuthorizedQuery<
    UnreadMessagesCountDto,
    Error,
    ReturnType<typeof messagingKeys.unreadCount>
  >({
    queryKey: messagingKeys.unreadCount(),
    queryFn: () => messagingApi.getUnreadMessagesCount(),
    refetchInterval: options?.refetchInterval,
  });

  return query.data?.count ?? 0;
}

export function useMarkDirectConversationAsReadMutation(
  options?: UseMutationOptions<void, Error, Uuid, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (conversationId) => messagingApi.markDirectConversationAsRead(conversationId),
    ...options,
    onSuccess: (data, variables, context) => {
      invalidate(messagingKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useDeleteSupportThreadMutation(
  options?: UseMutationOptions<void, Error, Uuid, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (threadId) => messagingApi.deleteSupportThread(threadId),
    ...options,
    meta: {
      errorTitle: "Support thread deletion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagingKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useAddSupportThreadMessageMutation(
  options?: UseMutationOptions<
    SupportMessageDto,
    Error,
    AddSupportThreadMessageRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => messagingApi.addSupportThreadMessage(variables),
    ...options,
    meta: {
      errorTitle: "Support message send failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(messagingKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}
