import type { QueryKey, UseMutationOptions, UseQueryOptions } from "@tanstack/react-query";
import { messagingApi, selectorsApi } from "@/lib/api";
import type {
  DisagreeWithSelectorRequestDto,
  GetSelectorsForSubjectRequestDto,
  GetSelectorsForUrlRequestDto,
  SelectorDto,
  SuggestSelectorRequestDto,
} from "@/lib/api/selectors";
import type { SupportThreadSummaryDto } from "@/lib/api/messaging";
import type { PageResponseDto } from "@/lib/api/shared";
import { selectorsKeys } from "@/lib/server-state/query-keys";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";
import { useApiInvalidation, useAuthorizedMutation, useAuthorizedQuery } from "@/lib/server-state/hooks/authorized";

export function useSelectorsForUrl(
  request: GetSelectorsForUrlRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<SelectorDto>,
      Error,
      PageResponseDto<SelectorDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: selectorsKeys.forUrl(request),
    queryFn: () => selectorsApi.getForUrl(request),
    ...options,
  });
}

export function useSelectorsForSubject(
  request: GetSelectorsForSubjectRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<SelectorDto>,
      Error,
      PageResponseDto<SelectorDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: selectorsKeys.forSubject(request),
    queryFn: () => selectorsApi.getForSubject(request),
    ...options,
  });
}

export function useSuggestSelectorMutation(
  options?: UseMutationOptions<
    SelectorDto,
    Error,
    SuggestSelectorRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => selectorsApi.suggest(variables),
    ...options,
    meta: {
      errorTitle: "Selector suggestion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(selectorsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useDisagreeWithSelectorMutation(
  options?: UseMutationOptions<
    SupportThreadSummaryDto,
    Error,
    DisagreeWithSelectorRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) =>
      messagingApi.createSupportThread({
        initialMessage: createSelectorFeedbackMessage(variables),
      }),
    ...options,
    meta: {
      errorTitle: "Selector feedback failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(selectorsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

function createSelectorFeedbackMessage(
  request: DisagreeWithSelectorRequestDto,
): string {
  return [
    `Selector feedback for ${request.selectorId}`,
    request.comment,
  ].join("\n\n");
}
