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

export type RequestStatusEnumDto =
  | "SENT"
  | "ACCEPTED"
  | "REJECTED"
  | "HIDDEN"
  | "CANCELLED";

export interface AccessRequestDto {
  id: Uuid;
  opinionListId: Uuid;
  opinionListName: string;
  owner: Uuid;
  ownerName: string;
  requester: Uuid;
  requesterName: string;
  status: RequestStatusEnumDto;
  timestamp: string;
}

export interface AccessRequestsFiltersDto {
  forListId?: Uuid;
  since?: string;
  status?: RequestStatusEnumDto;
}

export interface GetIncomingAccessRequestsRequestDto
  extends AuthorizedRequestDto {
  filters?: AccessRequestsFiltersDto;
  pageable: PageRequestDto;
}

export interface GetOutgoingAccessRequestsRequestDto
  extends AuthorizedRequestDto {
  filters?: AccessRequestsFiltersDto;
  pageable: PageRequestDto;
}

export interface AccessRequestActionRequestDto extends AuthorizedRequestDto {
  requestId: Uuid;
}

export interface CreateOutgoingAccessRequestRequestDto
  extends AuthorizedRequestDto {
  listId: Uuid;
}

export function createAccessRequestsApi(client: HttpClient = httpClient) {
  return {
    acceptIncoming(
      request: AccessRequestActionRequestDto,
    ): Promise<AccessRequestDto> {
      return client.post<AccessRequestDto>(
        `/access-requests/incoming/${request.requestId}/accept`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
        },
      );
    },

    cancelOutgoing(
      request: AccessRequestActionRequestDto,
    ): Promise<AccessRequestDto> {
      return client.get<AccessRequestDto>(
        `/access-requests/outgoing/cancel/${request.requestId}`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
        },
      );
    },

    createOutgoing(
      request: CreateOutgoingAccessRequestRequestDto,
    ): Promise<AccessRequestDto> {
      return client.get<AccessRequestDto>(
        `/access-requests/outgoing/create/${request.listId}`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
        },
      );
    },

    declineIncoming(
      request: AccessRequestActionRequestDto,
    ): Promise<AccessRequestDto> {
      return client.post<AccessRequestDto>(
        `/access-requests/incoming/${request.requestId}/decline`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
        },
      );
    },

    getIncoming(
      request: GetIncomingAccessRequestsRequestDto,
    ): Promise<PageResponseDto<AccessRequestDto>> {
      const query: HttpQueryParams = {
        ...createPageQuery(request.pageable),
        forListId: request.filters?.forListId,
        since: request.filters?.since,
        status: request.filters?.status,
      };

      return client.get<PageResponseDto<AccessRequestDto>>(
        "/access-requests/incoming",
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query,
        },
      );
    },

    getOutgoing(
      request: GetOutgoingAccessRequestsRequestDto,
    ): Promise<PageResponseDto<AccessRequestDto>> {
      const query: HttpQueryParams = {
        ...createPageQuery(request.pageable),
        forListId: request.filters?.forListId,
        since: request.filters?.since,
        status: request.filters?.status,
      };

      return client.get<PageResponseDto<AccessRequestDto>>(
        "/access-requests/outgoing",
        {
          headers: createAuthorizationHeaders(request.accessToken),
          query,
        },
      );
    },

    hideIncoming(
      request: AccessRequestActionRequestDto,
    ): Promise<AccessRequestDto> {
      return client.post<AccessRequestDto>(
        `/access-requests/incoming/${request.requestId}/hide`,
        {
          headers: createAuthorizationHeaders(request.accessToken),
        },
      );
    },
  };
}

export const accessRequestsApi = createAccessRequestsApi();
