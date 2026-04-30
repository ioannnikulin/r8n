import type { QueryKey, UseMutationOptions, UseQueryOptions } from "@tanstack/react-query";
import { referentsApi } from "@/lib/api";
import type {
  CreateReferentRequestDto,
  FindReferentsRequestDto,
} from "@/lib/api/referents";
import type { ReferentDto } from "@/lib/api/subjects";
import type { PageResponseDto } from "@/lib/api/shared";
import { referentsKeys } from "@/lib/server-state/query-keys";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";
import { useApiInvalidation, useAuthorizedMutation, useAuthorizedQuery } from "@/lib/server-state/hooks/authorized";

export function useFindReferents(
  request: FindReferentsRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<ReferentDto>,
      Error,
      PageResponseDto<ReferentDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: referentsKeys.find(request),
    queryFn: () => referentsApi.find(request),
    ...options,
  });
}

export function useCreateReferentMutation(
  options?: UseMutationOptions<
    ReferentDto,
    Error,
    CreateReferentRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => referentsApi.create(variables),
    ...options,
    meta: {
      errorTitle: "Location creation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(referentsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

