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

export interface SelectorDto {
  id: Uuid;
  referentId: Uuid;
  selector: string;
  urlHumanReadable: string;
  urlRegex: string;
}

export interface SupportThreadDto {
  id: Uuid;
  messages: string[];
}

export interface GetSelectorsForUrlRequestDto extends AuthorizedRequestDto {
  pageable: PageRequestDto;
  url: string;
}

export interface GetSelectorsForSubjectRequestDto extends AuthorizedRequestDto {
  subjectId: Uuid;
  pageable: PageRequestDto;
}

export interface SuggestSelectorRequestDto extends AuthorizedRequestDto {
  selector: string;
  subjectId: Uuid;
}

export interface DisagreeWithSelectorRequestDto extends AuthorizedRequestDto {
  comment: string;
  selectorId: Uuid;
}

export function createSelectorsApi(client: HttpClient = httpClient) {
  return {
    disagree(
      request: DisagreeWithSelectorRequestDto,
    ): Promise<SupportThreadDto> {
      return client.post<SupportThreadDto>(
        `/selectors/${request.selectorId}/disagree`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query: {
            comment: request.comment,
          },
        },
      );
    },

    getForSubject(
      request: GetSelectorsForSubjectRequestDto,
    ): Promise<PageResponseDto<SelectorDto>> {
      const query: HttpQueryParams = createPageQuery(request.pageable);

      return client.get<PageResponseDto<SelectorDto>>(
        `/selectors/for-subject/${request.subjectId}`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
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
        headers: createAuthorizationHeaders(request.accessToken),
        query,
      });
    },

    suggest(request: SuggestSelectorRequestDto): Promise<SelectorDto> {
      return client.post<SelectorDto>(`/selectors/for-subject/${request.subjectId}`, {
        headers: createAuthorizationHeaders(request.accessToken),
        query: {
          selector: request.selector,
        },
      });
    },
  };
}

export const selectorsApi = createSelectorsApi();
