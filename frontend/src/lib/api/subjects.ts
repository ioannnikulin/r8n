import type { HttpClient, HttpQueryParams } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createPageQuery,
  type PageRequestDto,
  type PageResponseDto,
  type Uuid,
} from "@/lib/api/shared";

export interface ReferentDto {
  address: string | null;
  id: Uuid;
  latitude: number | null;
  longitude: number | null;
  name: string;
}

export interface OpinionSubjectDto {
  alternativeReferents: ReferentDto[];
  id: Uuid;
  name: string;
  primaryReferent: ReferentDto | null;
}

export interface FindSubjectsRequestDto {
  pageable: PageRequestDto;
  query?: string;
  referentId?: Uuid;
}

export interface CreateSubjectRequestDto {
  address?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  name: string;
  primaryReferentId?: Uuid | null;
  referentName?: string | null;
}

export function createSubjectsApi(client: HttpClient = httpClient) {
  return {
    find(
      request: FindSubjectsRequestDto,
    ): Promise<PageResponseDto<OpinionSubjectDto>> {
      const query: HttpQueryParams = {
        ...createPageQuery(request.pageable),
        ...(request.query ? { query: request.query } : {}),
        ...(request.referentId ? { referentId: request.referentId } : {}),
      };

      return client.get<PageResponseDto<OpinionSubjectDto>>("/subjects/find", {
        auth: "required",
        query,
      });
    },

    create(request: CreateSubjectRequestDto): Promise<OpinionSubjectDto> {
      return client.post<OpinionSubjectDto>("/subjects", {
        auth: "required",
        body: {
          name: request.name,
          primaryReferentId: request.primaryReferentId,
          referentName: request.referentName,
          address: request.address,
          latitude: request.latitude,
          longitude: request.longitude,
        },
      });
    },

    setPrimaryReferent(request: { subjectId: Uuid; referentId: Uuid }): Promise<OpinionSubjectDto> {
      return client.patch<OpinionSubjectDto>(`/subjects/${request.subjectId}/set-primary-referent`, {
        auth: "required",
        query: {
          referentId: request.referentId,
        },
      });
    },
  };
}

export const subjectsApi = createSubjectsApi();
