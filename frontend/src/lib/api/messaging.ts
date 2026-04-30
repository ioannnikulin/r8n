import type { HttpClient } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import type { Uuid } from "@/lib/api/shared";

export interface SupportThreadSummaryDto {
  id: Uuid;
  ownerUserId: Uuid;
  createdAt: string;
  lastMessageAt: string | null;
}

export interface CreateSupportThreadRequestDto {
  initialMessage: string;
}

export function createMessagingApi(client: HttpClient = httpClient) {
  return {
    createSupportThread(
      request: CreateSupportThreadRequestDto,
    ): Promise<SupportThreadSummaryDto> {
      return client.post<SupportThreadSummaryDto>("/messaging/support/threads", {
        auth: "required",
        body: request,
      });
    },
  };
}

export const messagingApi = createMessagingApi();
