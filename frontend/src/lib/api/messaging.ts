import type { HttpClient } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createPageQuery,
  type PageRequestDto,
  type PageResponseDto,
  type Uuid,
} from "@/lib/api/shared";

export type SupportParticipantRoleEnumDto = "USER" | "SUPPORT";
export type SupportThreadViewerRoleEnumDto = "REQUESTER" | "SUPPORT";
export type MessageAuthorRoleEnumDto = "USER" | "MODERATOR" | "SUPPORT" | "ADMIN";

export interface DirectConversationSummaryDto {
  id: Uuid;
  participantUserId: Uuid;
  participantDisplayName: string;
  createdAt: string;
  lastMessageAt: string | null;
  lastMessageText: string | null;
  unreadCount: number;
}

export interface DirectMessageDto {
  id: Uuid;
  conversationId: Uuid;
  authorUserId: Uuid;
  authorDisplayName: string;
  authorRole: MessageAuthorRoleEnumDto;
  text: string;
  createdAt: string;
}

export interface CreateDirectConversationRequestDto {
  recipientUserId: Uuid;
  initialMessage: string;
}

export interface CreateDirectMessageRequestDto {
  text: string;
}

export interface GetDirectConversationSummariesRequestDto {
  pageable: PageRequestDto;
}

export interface GetDirectConversationMessagesRequestDto {
  conversationId: Uuid;
  pageable: PageRequestDto;
}

export interface AddDirectConversationMessageRequestDto {
  conversationId: Uuid;
  request: CreateDirectMessageRequestDto;
}

export interface SupportThreadSummaryDto {
  id: Uuid;
  ownerUserId: Uuid;
  viewerRole: SupportThreadViewerRoleEnumDto;
  createdAt: string;
  lastMessageAt: string | null;
  lastMessageText: string | null;
  unreadCount: number;
}

export interface SupportMessageDto {
  id: Uuid;
  threadId: Uuid;
  authorUserId: Uuid;
  authorDisplayName: string;
  authorRole: SupportParticipantRoleEnumDto;
  text: string;
  createdAt: string;
}

export interface CreateSupportThreadRequestDto {
  initialMessage: string;
}

export interface CreateSupportMessageRequestDto {
  text: string;
}

export interface GetSupportThreadSummariesRequestDto {
  pageable: PageRequestDto;
}

export interface GetSupportThreadMessagesRequestDto {
  pageable: PageRequestDto;
  threadId: Uuid;
}

export interface AddSupportThreadMessageRequestDto {
  request: CreateSupportMessageRequestDto;
  threadId: Uuid;
}

export interface UnreadMessagesCountDto {
  count: number;
}

export function createMessagingApi(client: HttpClient = httpClient) {
  return {
    getUnreadMessagesCount(): Promise<UnreadMessagesCountDto> {
      return client.get<UnreadMessagesCountDto>("/messaging/unread-count", {
        auth: "required",
      });
    },

    getDirectConversationSummaries(
      request: GetDirectConversationSummariesRequestDto,
    ): Promise<PageResponseDto<DirectConversationSummaryDto>> {
      return client.get<PageResponseDto<DirectConversationSummaryDto>>(
        "/messaging/direct/conversations",
        {
          auth: "required",
          query: createPageQuery(request.pageable),
        },
      );
    },

    createDirectConversation(
      request: CreateDirectConversationRequestDto,
    ): Promise<DirectConversationSummaryDto> {
      return client.post<DirectConversationSummaryDto>(
        "/messaging/direct/conversations",
        {
          auth: "required",
          body: request,
        },
      );
    },

    getDirectConversationMessages(
      request: GetDirectConversationMessagesRequestDto,
    ): Promise<PageResponseDto<DirectMessageDto>> {
      return client.get<PageResponseDto<DirectMessageDto>>(
        `/messaging/direct/conversations/${request.conversationId}/messages`,
        {
          auth: "required",
          query: createPageQuery(request.pageable),
        },
      );
    },

    addDirectConversationMessage(
      request: AddDirectConversationMessageRequestDto,
    ): Promise<DirectMessageDto> {
      return client.post<DirectMessageDto, CreateDirectMessageRequestDto>(
        `/messaging/direct/conversations/${request.conversationId}/messages`,
        {
          auth: "required",
          body: request.request,
        },
      );
    },

    getSupportThreadSummaries(
      request: GetSupportThreadSummariesRequestDto,
    ): Promise<PageResponseDto<SupportThreadSummaryDto>> {
      return client.get<PageResponseDto<SupportThreadSummaryDto>>(
        "/messaging/support/threads",
        {
          auth: "required",
          query: createPageQuery(request.pageable),
        },
      );
    },

    createSupportThread(
      request: CreateSupportThreadRequestDto,
    ): Promise<SupportThreadSummaryDto> {
      return client.post<SupportThreadSummaryDto>("/messaging/support/threads", {
        auth: "required",
        body: request,
      });
    },

    getSupportThreadMessages(
      request: GetSupportThreadMessagesRequestDto,
    ): Promise<PageResponseDto<SupportMessageDto>> {
      return client.get<PageResponseDto<SupportMessageDto>>(
        `/messaging/support/threads/${request.threadId}/messages`,
        {
          auth: "required",
          query: createPageQuery(request.pageable),
        },
      );
    },

    markDirectConversationAsRead(conversationId: Uuid): Promise<void> {
      return client.post<void>(`/messaging/direct/conversations/${conversationId}/read`, {
        auth: "required",
      });
    },

    deleteSupportThread(threadId: Uuid): Promise<void> {
      return client.delete<void>(`/messaging/support/threads/${threadId}`, {
        auth: "required",
      });
    },

    addSupportThreadMessage(
      request: AddSupportThreadMessageRequestDto,
    ): Promise<SupportMessageDto> {
      return client.post<SupportMessageDto, CreateSupportMessageRequestDto>(
        `/messaging/support/threads/${request.threadId}/messages`,
        {
          auth: "required",
          body: request.request,
        },
      );
    },
  };
}

export const messagingApi = createMessagingApi();
