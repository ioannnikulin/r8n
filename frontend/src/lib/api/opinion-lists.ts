import type { HttpClient, HttpQueryParams } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createAuthorizationHeaders,
  createPageQuery,
  type AuthorizedRequestDto,
  type PageRequestDto,
  type PageResponseDto,
  type Uuid,
} from "@/lib/api/shared";
import type { OpinionSummaryDto } from "@/lib/api/opinions";

export type OpinionListPrivacyEnumDto = "PRIVATE" | "SEARCHABLE";

export interface OpinionListSummaryDto {
  grantedAccessCount: number;
  id: Uuid;
  listName: string;
  opinionsCount: number;
  owner: Uuid;
  ownerName: string;
  privacy: OpinionListPrivacyEnumDto;
}

export interface OpinionListDto {
  id: Uuid;
  listName: string;
  opinionSummaries: OpinionSummaryDto[];
  owner: Uuid;
  ownerName: string;
}

export interface GetOpinionListSummaryRequestDto extends AuthorizedRequestDto {
  listId: Uuid;
}

export interface GetOpinionListRequestDto extends AuthorizedRequestDto {
  listId: Uuid;
}

export interface RenameOpinionListRequestDto extends AuthorizedRequestDto {
  listId: Uuid;
  name: string;
}

export interface SetOpinionListPrivacyRequestDto extends AuthorizedRequestDto {
  listId: Uuid;
  privacy: OpinionListPrivacyEnumDto;
}

export interface LinkOpinionToListRequestDto extends AuthorizedRequestDto {
  listId: Uuid;
  opinionId: Uuid;
}

export interface UnlinkOpinionFromListRequestDto extends AuthorizedRequestDto {
  listId: Uuid;
  opinionId: Uuid;
}

export interface SearchOpinionListsFiltersDto {
  authorId?: Uuid;
  authorNameSubstring?: string;
  nameSubstring?: string;
}

export interface SearchOpinionListsRequestDto extends AuthorizedRequestDto {
  filters?: SearchOpinionListsFiltersDto;
  pageable: PageRequestDto;
}

export interface SyncOpinionListsRequestDto extends AuthorizedRequestDto {
  addedListId: Uuid;
  existingListId: Uuid;
}

export interface UnsyncOpinionListsRequestDto extends AuthorizedRequestDto {
  existingListId: Uuid;
  removedListId: Uuid;
}

export interface GetMyOpinionListsRequestDto extends AuthorizedRequestDto {
  pageable: PageRequestDto;
}

export function createOpinionListsApi(client: HttpClient = httpClient) {
  return {
    getMine(
      request: GetMyOpinionListsRequestDto,
    ): Promise<PageResponseDto<OpinionListSummaryDto>> {
      return client.get<PageResponseDto<OpinionListSummaryDto>>(
        "/opinion-lists/mine",
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query: createPageQuery(request.pageable),
        },
      );
    },

    getSummary(
      request: GetOpinionListSummaryRequestDto,
    ): Promise<OpinionListSummaryDto> {
      return client.get<OpinionListSummaryDto>(
        `/opinion-lists/${request.listId}/summary`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
        },
      );
    },

    getById(request: GetOpinionListRequestDto): Promise<OpinionListDto> {
      return client.get<OpinionListDto>(`/opinion-lists/${request.listId}`, {
        headers: createAuthorizationHeaders(request.accessToken),
      });
    },

    linkOpinion(request: LinkOpinionToListRequestDto): Promise<OpinionListDto> {
      return client.post<OpinionListDto>(`/opinion-lists/${request.listId}/link`, {
        headers: createAuthorizationHeaders(request.accessToken),
        query: {
          opinionId: request.opinionId,
        },
      });
    },

    rename(request: RenameOpinionListRequestDto): Promise<OpinionListDto> {
      return client.patch<OpinionListDto>(`/opinion-lists/${request.listId}/rename`, {
        headers: createAuthorizationHeaders(request.accessToken),
        query: {
          name: request.name,
        },
      });
    },

    search(
      request: SearchOpinionListsRequestDto,
    ): Promise<PageResponseDto<OpinionListSummaryDto>> {
      const query: HttpQueryParams = {
        ...createPageQuery(request.pageable),
        authorId: request.filters?.authorId,
        authorNameSubstring: request.filters?.authorNameSubstring,
        nameSubstring: request.filters?.nameSubstring,
      };

      return client.get<PageResponseDto<OpinionListSummaryDto>>(
        "/opinion-lists/search",
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query,
        },
      );
    },

    setPrivacy(
      request: SetOpinionListPrivacyRequestDto,
    ): Promise<OpinionListDto> {
      return client.patch<OpinionListDto>(
        `/opinion-lists/${request.listId}/set-privacy`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query: {
            privacy: request.privacy,
          },
        },
      );
    },

    sync(request: SyncOpinionListsRequestDto): Promise<OpinionListDto> {
      return client.post<OpinionListDto>(
        `/opinion-lists/${request.existingListId}/sync`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query: {
            addedListId: request.addedListId,
          },
        },
      );
    },

    unlinkOpinion(
      request: UnlinkOpinionFromListRequestDto,
    ): Promise<OpinionListDto> {
      return client.patch<OpinionListDto>(
        `/opinion-lists/${request.listId}/unlink`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query: {
            opinionId: request.opinionId,
          },
        },
      );
    },

    unsync(request: UnsyncOpinionListsRequestDto): Promise<OpinionListDto> {
      return client.post<OpinionListDto>(
        `/opinion-lists/${request.existingListId}/unsync`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query: {
            removedListId: request.removedListId,
          },
        },
      );
    },
  };
}

export const opinionListsApi = createOpinionListsApi();
