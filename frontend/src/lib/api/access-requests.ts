import type { HttpClient, HttpQueryParams } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createPageQuery,
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

export type AccessRequestIntentDto = "NONE" | "COPY" | "MERGE";

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
  intent?: AccessRequestIntentDto;
  targetListId?: Uuid | null;
}

export interface AccessRequestsFiltersDto {
  forListId?: Uuid;
  since?: string;
  status?: RequestStatusEnumDto;
}

export interface GetIncomingAccessRequestsRequestDto {
  filters?: AccessRequestsFiltersDto;
  pageable: PageRequestDto;
}

export interface GetOutgoingAccessRequestsRequestDto {
  filters?: AccessRequestsFiltersDto;
  pageable: PageRequestDto;
}

export interface AccessRequestActionRequestDto {
  requestId: Uuid;
}

export interface CreateOutgoingAccessRequestRequestDto {
  listId: Uuid;
  intent?: AccessRequestIntentDto;
  targetListId?: Uuid;
}

export function createAccessRequestsApi(client: HttpClient = httpClient) {
  return {
    acceptIncoming(
      request: AccessRequestActionRequestDto,
    ): Promise<AccessRequestDto> {
      return client.post<AccessRequestDto>(
        `/access-requests/incoming/${request.requestId}/accept`,
        {
          auth: "required",
        },
      );
    },

    cancelOutgoing(
      request: AccessRequestActionRequestDto,
    ): Promise<AccessRequestDto> {
      return client.post<AccessRequestDto>(
        `/access-requests/outgoing/cancel/${request.requestId}`,
        {
          auth: "required",
        },
      );
    },

    createOutgoing(
      request: CreateOutgoingAccessRequestRequestDto,
    ): Promise<AccessRequestDto> {
      return client.post<AccessRequestDto>(
        `/access-requests/outgoing/create/${request.listId}`,
        {
          auth: "required",
          query: {
            ...(request.intent ? { intent: request.intent } : {}),
            ...(request.targetListId ? { targetListId: request.targetListId } : {}),
          },
        },
      );
    },

    declineIncoming(
      request: AccessRequestActionRequestDto,
    ): Promise<AccessRequestDto> {
      return client.post<AccessRequestDto>(
        `/access-requests/incoming/${request.requestId}/decline`,
        {
          auth: "required",
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
          auth: "required",
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
          auth: "required",
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
          auth: "required",
        },
      );
    },
  };
}

export const accessRequestsApi = createAccessRequestsApi();
