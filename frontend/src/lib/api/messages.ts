import type { HttpClient } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createPageQuery,
  type PageRequestDto,
  type PageResponseDto,
  type Uuid,
} from "@/lib/api/shared";

export type ThreadTypeEnumDto = "SUPPORT" | "DIRECT";

export type MessageAuthorRoleEnumDto = "USER" | "SUPPORT";

export interface ThreadParticipantDto {
  name: string;
  role: MessageAuthorRoleEnumDto;
  userId: Uuid | null;
}

export interface ThreadSummaryDto {
  createdAt: string;
  id: Uuid;
  lastMessageOwn: boolean;
  lastMessagePreview: string;
  participant: ThreadParticipantDto;
  type: ThreadTypeEnumDto;
  unreadCount: number;
  updatedAt: string;
}

export interface MessageDto {
  authorName: string;
  authorRole: MessageAuthorRoleEnumDto;
  authorUserId: Uuid;
  createdAt: string;
  id: Uuid;
  own: boolean;
  text: string;
  threadId: Uuid;
}

export interface CreateSupportThreadRequestDto {
  initialMessage: string;
}

export interface CreateDirectThreadRequestDto {
  initialMessage: string;
  recipientUserId: Uuid;
}

export interface CreateThreadMessageRequestDto {
  text: string;
}

export interface GetMessageThreadsRequestDto {
  pageable: PageRequestDto;
}

export interface GetThreadMessagesRequestDto {
  pageable: PageRequestDto;
  threadId: Uuid;
}

export interface AddThreadMessageRequestDto extends CreateThreadMessageRequestDto {
  threadId: Uuid;
}

export interface MarkThreadReadRequestDto {
  threadId: Uuid;
}

export function createMessagesApi(client: HttpClient = httpClient) {
  return {
    addThreadMessage(request: AddThreadMessageRequestDto): Promise<MessageDto> {
      return client.post<MessageDto, CreateThreadMessageRequestDto>(
        `/messaging/threads/${request.threadId}/messages`,
        {
          auth: "required",
          body: {
            text: request.text,
          },
        },
      );
    },

    createDirectThread(
      request: CreateDirectThreadRequestDto,
    ): Promise<ThreadSummaryDto> {
      return client.post<ThreadSummaryDto, CreateDirectThreadRequestDto>(
        "/messaging/direct/threads",
        {
          auth: "required",
          body: request,
        },
      );
    },

    createSupportThread(
      request: CreateSupportThreadRequestDto,
    ): Promise<ThreadSummaryDto> {
      return client.post<ThreadSummaryDto, CreateSupportThreadRequestDto>(
        "/messaging/support/threads",
        {
          auth: "required",
          body: request,
        },
      );
    },

    getThreadMessages(
      request: GetThreadMessagesRequestDto,
    ): Promise<PageResponseDto<MessageDto>> {
      return client.get<PageResponseDto<MessageDto>>(
        `/messaging/threads/${request.threadId}/messages`,
        {
          auth: "required",
          query: createPageQuery(request.pageable),
        },
      );
    },

    getThreads(
      request: GetMessageThreadsRequestDto,
    ): Promise<PageResponseDto<ThreadSummaryDto>> {
      return client.get<PageResponseDto<ThreadSummaryDto>>("/messaging/threads", {
        auth: "required",
        query: createPageQuery(request.pageable),
      });
    },

    markThreadRead(request: MarkThreadReadRequestDto): Promise<ThreadSummaryDto> {
      return client.post<ThreadSummaryDto>(
        `/messaging/threads/${request.threadId}/read`,
        {
          auth: "required",
        },
      );
    },
  };
}

export const messagesApi = createMessagesApi();
