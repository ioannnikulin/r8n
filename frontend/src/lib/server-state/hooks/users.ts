import type {
  QueryKey,
  UseMutationOptions,
  UseQueryOptions,
} from "@tanstack/react-query";
import { HttpError } from "@/lib/http-client";
import {
  useApiInvalidation,
  useAuthorizedMutation,
  useAuthorizedQuery,
} from "@/lib/server-state/hooks/authorized";
import { usersApi } from "@/lib/api/users";
import type {
  UpdateMyPublicProfileRequestDto,
  UploadMyAvatarRequestDto,
  UserProfileDto,
  UserSearchResultDto,
  UserWithRolesDto,
} from "@/lib/api/users";
import { usersKeys } from "@/lib/server-state/query-keys";
import type { ApiErrorMeta } from "@/lib/server-state/query-client";
import type { Uuid } from "@/lib/api/shared";

export function useMe() {
  return useAuthorizedQuery({
    queryKey: usersKeys.me(),
    queryFn: () => usersApi.getMe(),
  });
}

export function useUserProfile(
  id: Uuid,
  options?: Omit<
    UseQueryOptions<UserProfileDto, Error, UserProfileDto, QueryKey>,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: usersKeys.detail(id),
    queryFn: () => usersApi.getUser(id),
    enabled: !!id,
    ...options,
  });
}

export function useUserSearch(
  query: string,
  options?: Omit<
    UseQueryOptions<UserSearchResultDto[], Error, UserSearchResultDto[], QueryKey>,
    "queryKey" | "queryFn"
  >,
) {
  const normalizedQuery = query.trim();

  return useAuthorizedQuery({
    queryKey: usersKeys.search(normalizedQuery),
    queryFn: () => usersApi.searchUsers(normalizedQuery),
    enabled: normalizedQuery.length >= 2,
    ...options,
  });
}

export function useUserAvatar(
  id: Uuid,
  options?: Omit<
    UseQueryOptions<Blob | null, Error, Blob | null, QueryKey>,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: usersKeys.avatar(id),
    queryFn: async () => {
      try {
        return (await usersApi.getUserAvatar(id)) ?? null;
      } catch (error) {
        if (error instanceof HttpError && error.status === 404) {
          return null;
        }

        throw error;
      }
    },
    enabled: !!id,
    ...options,
  });
}

export function useUpdateMyPublicProfileMutation(
  options?: UseMutationOptions<
    UserProfileDto,
    Error,
    UpdateMyPublicProfileRequestDto,
    unknown
  >,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => usersApi.updateMyPublicProfile(variables),
    ...options,
    meta: {
      errorTitle: "Profile update failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useUploadMyAvatarMutation(
  options?: UseMutationOptions<void, Error, UploadMyAvatarRequestDto, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (variables) => usersApi.uploadMyAvatar(variables),
    ...options,
    meta: {
      errorTitle: "Avatar upload failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useDeleteMyAvatarMutation(
  options?: UseMutationOptions<void, Error, void, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: () => usersApi.deleteMyAvatar(),
    ...options,
    meta: {
      errorTitle: "Avatar deletion failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.all);
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useUsersWithRoles(
  options?: Omit<
    UseQueryOptions<UserWithRolesDto[], Error, UserWithRolesDto[], QueryKey>,
    "queryKey" | "queryFn"
  >,
) {
  return useAuthorizedQuery({
    queryKey: usersKeys.withRoles(),
    queryFn: () => usersApi.listUsersWithRoles(),
    ...options,
  });
}

export function useAssignModeratorMutation(
  options?: UseMutationOptions<void, Error, Uuid, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (userId) => usersApi.assignModerator(userId),
    ...options,
    meta: {
      errorTitle: "Role assignment failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.withRoles());
      invalidate(usersKeys.me());
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useRevokeModeratorMutation(
  options?: UseMutationOptions<void, Error, Uuid, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (userId) => usersApi.revokeModerator(userId),
    ...options,
    meta: {
      errorTitle: "Role revocation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.withRoles());
      invalidate(usersKeys.me());
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useAssignSupportMutation(
  options?: UseMutationOptions<void, Error, Uuid, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (userId) => usersApi.assignSupport(userId),
    ...options,
    meta: {
      errorTitle: "Role assignment failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.withRoles());
      invalidate(usersKeys.me());
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useRevokeSupportMutation(
  options?: UseMutationOptions<void, Error, Uuid, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (userId) => usersApi.revokeSupport(userId),
    ...options,
    meta: {
      errorTitle: "Role revocation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.withRoles());
      invalidate(usersKeys.me());
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useAssignAdminMutation(
  options?: UseMutationOptions<void, Error, Uuid, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (userId) => usersApi.assignAdmin(userId),
    ...options,
    meta: {
      errorTitle: "Role assignment failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.withRoles());
      invalidate(usersKeys.me());
      options?.onSuccess?.(data, variables, context);
    },
  });
}

export function useRevokeAdminMutation(
  options?: UseMutationOptions<void, Error, Uuid, unknown>,
) {
  const invalidate = useApiInvalidation();

  return useAuthorizedMutation({
    mutationFn: (userId) => usersApi.revokeAdmin(userId),
    ...options,
    meta: {
      errorTitle: "Role revocation failed",
      ...options?.meta,
    } as ApiErrorMeta,
    onSuccess: (data, variables, context) => {
      invalidate(usersKeys.withRoles());
      invalidate(usersKeys.me());
      options?.onSuccess?.(data, variables, context);
    },
  });
}
