import type { QueryKey, UseMutationOptions, UseQueryOptions } from "@tanstack/react-query";
import { opinionsApi } from "@/lib/api";
import type {
  AdjustOpinionComponentWeightRequestDto,
  CreateOpinionRequestDto,
  DeleteOpinionRequestDto,
  GetModerationDecisionsRequestDto,
  GetModerationOpinionsRequestDto,
  GetOpinionByIdRequestDto,
  GetOpinionForSubjectRequestDto,
  LinkOpinionComponentRequestDto,
  ModerateOpinionRequestDto,
  ModerationDecisionDto,
  OpinionDto,
  RejectOpinionRequestDto,
  SubmitOpinionForModerationRequestDto,
  UnlinkOpinionComponentRequestDto,
  UpdateOpinionRequestDto,
} from "@/lib/api/opinions";
import type { PageResponseDto } from "@/lib/api/shared";
import { opinionListsKeys, opinionsKeys } from "@/lib/server-state/query-keys";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";
import { useApiInvalidation, useAuthorizedMutation, useAuthorizedQuery } from "@/lib/server-state/hooks/authorized";

export function useOpinion(
  request: GetOpinionByIdRequestDto,
  options?: Omit<
    UseQueryOptions<OpinionDto, Error, OpinionDto, QueryKey>,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: opinionsKeys.detail(request),
    queryFn: () => opinionsApi.getById(request),
    ...options,
  });
}

export function useOpinionForSubject(
  request: GetOpinionForSubjectRequestDto,
  options?: Omit<
    UseQueryOptions<OpinionDto, Error, OpinionDto, QueryKey>,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: opinionsKeys.forSubject(request),
    queryFn: () => opinionsApi.getForSubject(request),
    ...options,
  });
}

export function useModerationOpinions(
  request: GetModerationOpinionsRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<OpinionDto>,
      Error,
      PageResponseDto<OpinionDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: opinionsKeys.moderation(request),
    queryFn: () => opinionsApi.getModerationQueue(request),
    ...options,
  });
}

export function useModerationDecisions(
  request: GetModerationDecisionsRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<ModerationDecisionDto>,
      Error,
      PageResponseDto<ModerationDecisionDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: opinionsKeys.moderationDecisions(request),
    queryFn: () => opinionsApi.getModerationDecisions(request),
    ...options,
  });
}

export function useCreateOpinionMutation(
  options?: UseMutationOptions<
    OpinionDto,
    Error,
    CreateOpinionRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.create(variables),
    ...options,
    meta: {
      errorTitle: "Opinion creation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useUpdateOpinionMutation(
  options?: UseMutationOptions<
    OpinionDto,
    Error,
    UpdateOpinionRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.update(variables),
    ...options,
    meta: {
      errorTitle: "Opinion update failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useDeleteOpinionMutation(
  options?: UseMutationOptions<
    void,
    Error,
    DeleteOpinionRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.delete(variables),
    ...options,
    meta: {
      errorTitle: "Opinion deletion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useSubmitOpinionForModerationMutation(
  options?: UseMutationOptions<
    OpinionDto,
    Error,
    SubmitOpinionForModerationRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.submitForModeration(variables),
    ...options,
    meta: {
      errorTitle: "Opinion submission failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useApproveOpinionMutation(
  options?: UseMutationOptions<
    OpinionDto,
    Error,
    ModerateOpinionRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.approve(variables),
    ...options,
    meta: {
      errorTitle: "Opinion approval failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useRejectOpinionMutation(
  options?: UseMutationOptions<
    OpinionDto,
    Error,
    RejectOpinionRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.reject(variables),
    ...options,
    meta: {
      errorTitle: "Opinion rejection failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useLinkOpinionComponentMutation(
  options?: UseMutationOptions<
    OpinionDto,
    Error,
    LinkOpinionComponentRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.linkComponent(variables),
    ...options,
    meta: {
      errorTitle: "Opinion component link failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useUnlinkOpinionComponentMutation(
  options?: UseMutationOptions<
    OpinionDto,
    Error,
    UnlinkOpinionComponentRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.unlinkComponent(variables),
    ...options,
    meta: {
      errorTitle: "Opinion component unlink failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useAdjustOpinionComponentWeightMutation(
  options?: UseMutationOptions<
    OpinionDto,
    Error,
    AdjustOpinionComponentWeightRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionsApi.adjustComponentWeight(variables),
    ...options,
    meta: {
      errorTitle: "Opinion component update failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionsKeys.all);
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}
