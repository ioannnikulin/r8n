import type { HttpClient, HttpQueryParams } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createPageQuery,
  type PageRequestDto,
  type PageResponseDto,
  type Uuid,
} from "@/lib/api/shared";
import type { OpinionSummaryDto } from "@/lib/api/opinions";

export type OpinionListPrivacyEnumDto = "PRIVATE" | "SEARCHABLE";

export interface OpinionListSummaryDto {
  grantedAccessCount: number;
  listId: Uuid;
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
  privacy: OpinionListPrivacyEnumDto;
}

export interface GetOpinionListSummaryRequestDto {
  listId: Uuid;
}

export interface GetOpinionListRequestDto {
  listId: Uuid;
  publishedAfter?: string;
}

export interface RenameOpinionListRequestDto {
  listId: Uuid;
  name: string;
}

export interface SetOpinionListPrivacyRequestDto {
  listId: Uuid;
  privacy: OpinionListPrivacyEnumDto;
}

export interface LinkOpinionToListRequestDto {
  listId: Uuid;
  opinionId: Uuid;
  weight?: number;
}

export interface UnlinkOpinionFromListRequestDto {
  listId: Uuid;
  opinionId: Uuid;
}

export interface LocationFilterDto {
  containsLocationSubstring?: string;
  latitude?: number;
  longitude?: number;
  radiusInMeters?: number;
}

export interface SearchOpinionListsFiltersDto {
  authorId?: Uuid;
  authorNameSubstring?: string;
  nameSubstring?: string;
  someOpinionsYoungerThan?: string;
  containsSubjectSubstring?: string;
  locationFilter?: LocationFilterDto;
  findThisTextInAnyOfTheAbove?: string;
}

export interface SearchOpinionListsRequestDto {
  filters?: SearchOpinionListsFiltersDto;
  pageable: PageRequestDto;
}

export interface SyncOpinionListsRequestDto {
  addedListId: Uuid;
  existingListId: Uuid;
  weight: number;
}

export interface UnsyncOpinionListsRequestDto {
  existingListId: Uuid;
  removedListId: Uuid;
}

export interface GetMyOpinionListsRequestDto {
  pageable: PageRequestDto;
}

export interface CreateOpinionListRequestDto {
  name: string;
  privacy: OpinionListPrivacyEnumDto;
}

export function createOpinionListsApi(client: HttpClient = httpClient) {
  return {
    create(request: CreateOpinionListRequestDto): Promise<OpinionListDto> {
      return client.post<OpinionListDto>("/opinion-lists", {
        auth: "required",
        query: {
          name: request.name,
          privacy: request.privacy,
        },
      });
    },

    getMine(
      request: GetMyOpinionListsRequestDto,
    ): Promise<PageResponseDto<OpinionListSummaryDto>> {
      return client.get<PageResponseDto<OpinionListSummaryDto>>(
        "/opinion-lists/mine",
        {
          auth: "required",
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
          auth: "required",
        },
      );
    },

    getById(request: GetOpinionListRequestDto): Promise<OpinionListDto> {
      return client.get<OpinionListDto>(`/opinion-lists/${request.listId}`, {
        auth: "required",
        query: request.publishedAfter ? { publishedAfter: request.publishedAfter } : undefined,
      });
    },

    linkOpinion(request: LinkOpinionToListRequestDto): Promise<OpinionListDto> {
      return client.post<OpinionListDto>(`/opinion-lists/${request.listId}/link`, {
        auth: "required",
        query: {
          opinionId: request.opinionId,
          weight: request.weight,
        },
      });
    },

    rename(request: RenameOpinionListRequestDto): Promise<OpinionListDto> {
      return client.patch<OpinionListDto>(`/opinion-lists/${request.listId}/rename`, {
        auth: "required",
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
        someOpinionsYoungerThan: request.filters?.someOpinionsYoungerThan,
        containsSubjectSubstring: request.filters?.containsSubjectSubstring,
        findThisTextInAnyOfTheAbove: request.filters?.findThisTextInAnyOfTheAbove,
        "locationFilter.containsLocationSubstring": request.filters?.locationFilter?.containsLocationSubstring,
        "locationFilter.latitude": request.filters?.locationFilter?.latitude,
        "locationFilter.longitude": request.filters?.locationFilter?.longitude,
        "locationFilter.radiusInMeters": request.filters?.locationFilter?.radiusInMeters,
      };

      return client.get<PageResponseDto<OpinionListSummaryDto>>(
        "/opinion-lists/search",
        {
          auth: "required",
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
          auth: "required",
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
          auth: "required",
          query: {
            addedListId: request.addedListId,
            weight: request.weight,
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
          auth: "required",
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
          auth: "required",
          query: {
            removedListId: request.removedListId,
          },
        },
      );
    },
  };
}

export const opinionListsApi = createOpinionListsApi();
