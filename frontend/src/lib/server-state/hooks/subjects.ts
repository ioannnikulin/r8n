import type { QueryKey, UseMutationOptions, UseQueryOptions } from "@tanstack/react-query";
import { subjectsApi } from "@/lib/api";
import type {
  CreateSubjectRequestDto,
  FindSubjectsRequestDto,
  OpinionSubjectDto,
} from "@/lib/api/subjects";
import type { PageResponseDto, Uuid } from "@/lib/api/shared";
import { subjectsKeys } from "@/lib/server-state/query-keys";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";
import { useApiInvalidation, useAuthorizedMutation, useAuthorizedQuery } from "@/lib/server-state/hooks/authorized";

export function useFindSubjects(
  request: FindSubjectsRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<OpinionSubjectDto>,
      Error,
      PageResponseDto<OpinionSubjectDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: subjectsKeys.find(request),
    queryFn: () => subjectsApi.find(request),
    ...options,
  });
}

export function useCreateSubjectMutation(
  options?: UseMutationOptions<
    OpinionSubjectDto,
    Error,
    CreateSubjectRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => subjectsApi.create(variables),
    ...options,
    meta: {
      errorTitle: "Subject creation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(subjectsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export interface SetPrimaryReferentRequestDto {
  referentId: Uuid;
  subjectId: Uuid;
}

export function useSetPrimaryReferentMutation(
  options?: UseMutationOptions<
    OpinionSubjectDto,
    Error,
    SetPrimaryReferentRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => subjectsApi.setPrimaryReferent(variables),
    ...options,
    meta: {
      errorTitle: "Location update failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(subjectsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}
