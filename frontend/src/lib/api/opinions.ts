import type { HttpClient } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createAuthorizationHeaders,
  type AuthorizedRequestDto,
  type Uuid,
} from "@/lib/api/shared";

export type OpinionStatusEnumDto =
  | "DRAFT"
  | "PENDING_PREMODERATION"
  | "PUBLISHED"
  | "REJECTED";

export interface WeightedOpinionReferenceDto {
  id: Uuid;
  opinion: Uuid;
  weight: number;
}

export interface OpinionSummaryDto {
  componentMark: number | null;
  id: Uuid;
  opinions: WeightedOpinionReferenceDto[];
  ownMark: number | null;
  subject: Uuid;
  subjectName: string;
  synchronizedMark: number;
}

export interface OpinionDto {
  componentMark: number | null;
  components: WeightedOpinionReferenceDto[];
  id: Uuid;
  mark: number | null;
  objective: string[];
  owner: Uuid;
  ownerName: string;
  status: OpinionStatusEnumDto;
  subject: Uuid;
  subjective: string[];
  subjectName: string;
  timestamp: string;
}

export interface GetOpinionByIdRequestDto extends AuthorizedRequestDto {
  id: Uuid;
}

export interface GetOpinionForSubjectRequestDto extends AuthorizedRequestDto {
  subjectId: Uuid;
}

export interface CreateOpinionRequestDto extends AuthorizedRequestDto {
  mark?: number | null;
  objective?: readonly string[];
  subjectId: Uuid;
  subjective?: readonly string[];
}

export interface UpdateOpinionRequestDto extends AuthorizedRequestDto {
  mark?: number | null;
  objective?: readonly string[];
  opinionId: Uuid;
  subjective?: readonly string[];
}

export interface DeleteOpinionRequestDto extends AuthorizedRequestDto {
  opinionId: Uuid;
}

export interface LinkOpinionComponentRequestDto extends AuthorizedRequestDto {
  childOpinionId: Uuid;
  parentOpinionId: Uuid;
  weight: number;
}

export interface UnlinkOpinionComponentRequestDto extends AuthorizedRequestDto {
  linkId: Uuid;
}

export interface AdjustOpinionComponentWeightRequestDto
  extends AuthorizedRequestDto {
  linkId: Uuid;
  weight: number;
}

export function createOpinionsApi(client: HttpClient = httpClient) {
  return {
    adjustComponentWeight(
      request: AdjustOpinionComponentWeightRequestDto,
    ): Promise<OpinionDto> {
      return client.patch<OpinionDto>(`/opinions/adjustWeight/${request.linkId}`, {
        headers: createAuthorizationHeaders(request.accessToken),
        query: {
          weight: request.weight,
        },
      });
    },

    create(request: CreateOpinionRequestDto): Promise<OpinionDto> {
      return client.post<OpinionDto>("/opinions", {
        headers: createAuthorizationHeaders(request.accessToken),
        query: {
          mark: request.mark,
          objective: request.objective,
          subjectId: request.subjectId,
          subjective: request.subjective,
        },
      });
    },

    delete(request: DeleteOpinionRequestDto): Promise<void> {
      return client.delete<void>(`/opinions/${request.opinionId}`, {
        headers: createAuthorizationHeaders(request.accessToken),
      });
    },

    getById(request: GetOpinionByIdRequestDto): Promise<OpinionDto> {
      return client.get<OpinionDto>(`/opinions/${request.id}`, {
        headers: createAuthorizationHeaders(request.accessToken),
      });
    },

    getForSubject(
      request: GetOpinionForSubjectRequestDto,
    ): Promise<OpinionDto> {
      return client.get<OpinionDto>(`/opinions/for/${request.subjectId}`, {
        headers: createAuthorizationHeaders(request.accessToken),
      });
    },

    linkComponent(
      request: LinkOpinionComponentRequestDto,
    ): Promise<OpinionDto> {
      return client.post<OpinionDto>("/opinions/link", {
        headers: createAuthorizationHeaders(request.accessToken),
        query: {
          childOpinionId: request.childOpinionId,
          parentOpinionId: request.parentOpinionId,
          weight: request.weight,
        },
      });
    },

    unlinkComponent(
      request: UnlinkOpinionComponentRequestDto,
    ): Promise<OpinionDto> {
      return client.delete<OpinionDto>(`/opinions/unlink/${request.linkId}`, {
        headers: createAuthorizationHeaders(request.accessToken),
      });
    },

    update(request: UpdateOpinionRequestDto): Promise<OpinionDto> {
      return client.patch<OpinionDto>(`/opinions/${request.opinionId}`, {
        headers: createAuthorizationHeaders(request.accessToken),
        query: {
          mark: request.mark,
          objective: request.objective,
          subjective: request.subjective,
        },
      });
    },
  };
}

export const opinionsApi = createOpinionsApi();
