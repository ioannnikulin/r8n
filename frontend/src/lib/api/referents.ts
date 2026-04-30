import type { HttpClient, HttpQueryParams } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createPageQuery,
  type PageRequestDto,
  type PageResponseDto,
} from "@/lib/api/shared";
import type { ReferentDto } from "@/lib/api/subjects";

export interface FindReferentsRequestDto {
  pageable: PageRequestDto;
  query?: string;
}

export interface CreateReferentRequestDto {
  address?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  name: string;
}

export function createReferentsApi(client: HttpClient = httpClient) {
  return {
    find(request: FindReferentsRequestDto): Promise<PageResponseDto<ReferentDto>> {
      const query: HttpQueryParams = {
        ...createPageQuery(request.pageable),
        ...(request.query ? { query: request.query } : {}),
      };

      return client.get<PageResponseDto<ReferentDto>>("/referents/find", {
        auth: "required",
        query,
      });
    },

    create(request: CreateReferentRequestDto): Promise<ReferentDto> {
      return client.post<ReferentDto>("/referents", {
        auth: "required",
        body: {
          name: request.name,
          address: request.address,
          latitude: request.latitude,
          longitude: request.longitude,
        },
      });
    },
  };
}

export const referentsApi = createReferentsApi();

