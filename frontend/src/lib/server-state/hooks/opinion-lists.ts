import type { QueryKey, UseMutationOptions, UseQueryOptions } from "@tanstack/react-query";
import { opinionListsApi, opinionsApi } from "@/lib/api";
import type {
  CreateOpinionListRequestDto,
  DeleteOpinionListRequestDto,
  GetMyOpinionListsRequestDto,
  GetOpinionListRequestDto,
  GetOpinionListSummaryRequestDto,
  LinkOpinionToListRequestDto,
  MoveOpinionRequestDto,
  OpinionListDto,
  OpinionListSummaryDto,
  RenameOpinionListRequestDto,
  SearchOpinionListsRequestDto,
  SetOpinionListPrivacyRequestDto,
  SyncOpinionListsRequestDto,
  UnlinkOpinionFromListRequestDto,
  UnsyncOpinionListsRequestDto,
} from "@/lib/api/opinion-lists";
import type { PageResponseDto, Uuid } from "@/lib/api/shared";
import { opinionListsKeys } from "@/lib/server-state/query-keys";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";
import { useApiInvalidation, useAuthorizedMutation, useAuthorizedQuery } from "@/lib/server-state/hooks/authorized";

export function useMyOpinionLists(
  request: GetMyOpinionListsRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<OpinionListSummaryDto>,
      Error,
      PageResponseDto<OpinionListSummaryDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: opinionListsKeys.mine(request),
    queryFn: () => opinionListsApi.getMine(request),
    ...options,
  });
}

export function useOpinionListSummary(
  request: GetOpinionListSummaryRequestDto,
  options?: Omit<
    UseQueryOptions<OpinionListSummaryDto, Error, OpinionListSummaryDto, QueryKey>,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: opinionListsKeys.summary(request),
    queryFn: () => opinionListsApi.getSummary(request),
    ...options,
  });
}

export function useOpinionList(
  request: GetOpinionListRequestDto,
  options?: Omit<
    UseQueryOptions<OpinionListDto, Error, OpinionListDto, QueryKey>,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: opinionListsKeys.detail(request),
    queryFn: () => opinionListsApi.getById(request),
    ...options,
  });
}

export function useSearchOpinionLists(
  request: SearchOpinionListsRequestDto,
  options?: Omit<
    UseQueryOptions<
      PageResponseDto<OpinionListSummaryDto>,
      Error,
      PageResponseDto<OpinionListSummaryDto>,
      QueryKey
    >,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: opinionListsKeys.search(request),
    queryFn: () => opinionListsApi.search(request),
    ...options,
  });
}

export function useCreateOpinionListMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    CreateOpinionListRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.create(variables),
    ...options,
    meta: {
      errorTitle: "List creation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useDeleteOpinionListMutation(
  options?: UseMutationOptions<
    void,
    Error,
    DeleteOpinionListRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.delete(variables),
    ...options,
    meta: {
      errorTitle: "List deletion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useMoveOpinionMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    MoveOpinionRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.moveOpinion(variables),
    ...options,
    meta: {
      errorTitle: "Move opinion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useRenameOpinionListMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    RenameOpinionListRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.rename(variables),
    ...options,
    meta: {
      errorTitle: "Rename failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useSetOpinionListPrivacyMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    SetOpinionListPrivacyRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.setPrivacy(variables),
    ...options,
    meta: {
      errorTitle: "Privacy update failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useLinkOpinionToListMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    LinkOpinionToListRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.linkOpinion(variables),
    ...options,
    meta: {
      errorTitle: "Link opinion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export interface LinkMyOpinionForSubjectToListRequestDto {
  listId: Uuid;
  subjectId: Uuid;
  weight?: number;
}

export function useLinkMyOpinionForSubjectToListMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    LinkMyOpinionForSubjectToListRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: async (variables) => {
      const opinion = await opinionsApi.getForSubject({ subjectId: variables.subjectId });
      return opinionListsApi.linkOpinion({
        listId: variables.listId,
        opinionId: opinion.id,
        weight: variables.weight,
      });
    },
    ...options,
    meta: {
      errorTitle: "Import opinion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useUnlinkOpinionFromListMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    UnlinkOpinionFromListRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.unlinkOpinion(variables),
    ...options,
    meta: {
      errorTitle: "Unlink opinion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useSyncOpinionListsMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    SyncOpinionListsRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.sync(variables),
    ...options,
    meta: {
      errorTitle: "List sync failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useUnsyncOpinionListsMutation(
  options?: UseMutationOptions<
    OpinionListDto,
    Error,
    UnsyncOpinionListsRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => opinionListsApi.unsync(variables),
    ...options,
    meta: {
      errorTitle: "List unsync failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(opinionListsKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}
