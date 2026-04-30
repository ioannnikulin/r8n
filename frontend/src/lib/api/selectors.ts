import type { HttpClient, HttpQueryParams } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createPageQuery,
  type PageRequestDto,
  type PageResponseDto,
  type Uuid,
} from "@/lib/api/shared";

export interface SelectorDto {
  id: Uuid;
  referentId: Uuid;
  selector: string;
  urlHumanReadable: string;
  urlRegex: string;
}

export interface GetSelectorsForUrlRequestDto {
  pageable: PageRequestDto;
  url: string;
}

export interface GetSelectorsForSubjectRequestDto {
  subjectId: Uuid;
  pageable: PageRequestDto;
}

export interface SuggestSelectorRequestDto {
  selector: string;
  subjectId: Uuid;
}

export interface DisagreeWithSelectorRequestDto {
  comment: string;
  selectorId: Uuid;
}

export function createSelectorsApi(client: HttpClient = httpClient) {
  return {
    getForSubject(
      request: GetSelectorsForSubjectRequestDto,
    ): Promise<PageResponseDto<SelectorDto>> {
      const query: HttpQueryParams = createPageQuery(request.pageable);

      return client.get<PageResponseDto<SelectorDto>>(
        `/selectors/for-subject/${request.subjectId}`,
        {
          auth: "required",
          query,
        },
      );
    },

    getForUrl(
      request: GetSelectorsForUrlRequestDto,
    ): Promise<PageResponseDto<SelectorDto>> {
      const query: HttpQueryParams = {
        ...createPageQuery(request.pageable),
        url: request.url,
      };

      return client.get<PageResponseDto<SelectorDto>>("/selectors/for-url", {
        auth: "required",
        query,
      });
    },

    suggest(request: SuggestSelectorRequestDto): Promise<SelectorDto> {
      return client.post<SelectorDto>(`/selectors/for-subject/${request.subjectId}`, {
        auth: "required",
        query: {
          selector: request.selector,
        },
      });
    },
  };
}

export const selectorsApi = createSelectorsApi();
