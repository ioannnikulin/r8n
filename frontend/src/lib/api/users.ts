import type { HttpClient } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import type { Uuid } from "@/lib/api/shared";

export type RoleDto = "USER" | "MODERATOR" | "SUPPORT" | "ADMIN";

export interface UsernameDto {
  id: Uuid;
  name: string;
  roles: RoleDto[];
}

export type UserStatusEnumDto =
  | "ACTIVE"
  | "SUSPENDED"
  | "DELETION_PENDING"
  | "DELETED";

export interface UserProfileDto {
  id: Uuid;
  name: string;
  status: UserStatusEnumDto;
  lastSeenAt: string | null;
  about: string | null;
  location: string | null;
}

export interface UserSearchResultDto {
  id: Uuid;
  name: string;
  lastSeenAt: string | null;
}

export interface UploadMyAvatarRequestDto {
  file: File;
}

export interface UpdateMyPublicProfileRequestDto {
  about: string | null;
  location: string | null;
  name: string;
}

export interface UserWithRolesDto {
  id: Uuid;
  name: string;
  email: string;
  status: UserStatusEnumDto;
  isModerator: boolean;
  isSupport: boolean;
  isAdmin: boolean;
}

export function createUsersApi(client: HttpClient = httpClient) {
  return {
    getMe(): Promise<UsernameDto> {
      return client.get<UsernameDto>("/users/me", { auth: "required" });
    },

    getUser(id: Uuid): Promise<UserProfileDto> {
      return client.get<UserProfileDto>(`/users/${id}`, { auth: "required" });
    },

    searchUsers(query: string): Promise<UserSearchResultDto[]> {
      return client.get<UserSearchResultDto[]>("/users/search", {
        auth: "required",
        query: { query },
      });
    },

    updateMyPublicProfile(request: UpdateMyPublicProfileRequestDto): Promise<UserProfileDto> {
      return client.patch<UserProfileDto, UpdateMyPublicProfileRequestDto>(
        "/users/me/public-profile",
        {
          auth: "required",
          body: request,
        },
      );
    },

    getUserAvatar(id: Uuid): Promise<Blob | undefined> {
      return client.get<Blob | undefined>(`/users/${id}/avatar`, {
        auth: "required",
        headers: { Accept: "image/*" },
        responseType: "blob",
      });
    },

    uploadMyAvatar(request: UploadMyAvatarRequestDto): Promise<void> {
      const formData = new FormData();
      formData.append("file", request.file);

      return client.post<void, FormData>("/users/me/avatar", {
        auth: "required",
        body: formData,
      });
    },

    deleteMyAvatar(): Promise<void> {
      return client.delete<void>("/users/me/avatar", { auth: "required" });
    },

    listUsersWithRoles(): Promise<UserWithRolesDto[]> {
      return client.get<UserWithRolesDto[]>("/admin/users", { auth: "required" });
    },

    assignModerator(userId: Uuid): Promise<void> {
      return client.post<void, { role: string }>(`/admin/users/${userId}/roles`, {
        auth: "required",
        body: { role: "MODERATOR" },
      });
    },

    revokeModerator(userId: Uuid): Promise<void> {
      return client.delete<void>(`/admin/users/${userId}/roles/MODERATOR`, { auth: "required" });
    },

    assignSupport(userId: Uuid): Promise<void> {
      return client.post<void, { role: string }>(`/admin/users/${userId}/roles`, {
        auth: "required",
        body: { role: "SUPPORT" },
      });
    },

    revokeSupport(userId: Uuid): Promise<void> {
      return client.delete<void>(`/admin/users/${userId}/roles/SUPPORT`, { auth: "required" });
    },

    assignAdmin(userId: Uuid): Promise<void> {
      return client.post<void, { role: string }>(`/admin/users/${userId}/roles`, {
        auth: "required",
        body: { role: "ADMIN" },
      });
    },

    revokeAdmin(userId: Uuid): Promise<void> {
      return client.delete<void>(`/admin/users/${userId}/roles/ADMIN`, { auth: "required" });
    },
  };
}

export const usersApi = createUsersApi();
