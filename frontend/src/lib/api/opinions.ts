import type { HttpClient } from "@/lib/http-client";
import { httpClient } from "@/lib/http-client";
import {
  createPageQuery,
  type PageRequestDto,
  type PageResponseDto,
  type Uuid,
} from "@/lib/api/shared";

export type OpinionStatusEnumDto =
  | "DRAFT"
  | "PENDING_PREMODERATION"
  | "PUBLISHED"
  | "REJECTED";

export type ModerationDecisionActionDto = "APPROVED" | "REJECTED";

export interface WeightedOpinionReferenceDto {
  id: Uuid;
  opinion: Uuid;
  weight: number;
}

export interface OpinionRowDto {
  objective: string[];
  opinionId: Uuid;
  owner: Uuid;
  ownerName: string;
  mark: number | null;
  status: OpinionStatusEnumDto;
  subjective: string[];
  timestamp: string;
  weight: number;
}

export interface OpinionSummaryDto {
  address?: string | null;
  componentMark: number | null;
  latitude?: number | null;
  longitude?: number | null;
  opinions: OpinionRowDto[];
  ownMark: number | null;
  subject: Uuid;
  subjectName: string;
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

export interface ModerationDecisionDto {
  action: ModerationDecisionActionDto;
  createdAt: string;
  id: Uuid;
  moderatorId: Uuid;
  moderatorName: string;
  newStatus: OpinionStatusEnumDto;
  opinionId: Uuid;
  ownerName: string;
  previousStatus: OpinionStatusEnumDto;
  reason: string | null;
  subjectName: string;
}

export interface GetOpinionByIdRequestDto {
  id: Uuid;
}

export interface GetOpinionForSubjectRequestDto {
  subjectId: Uuid;
}

export interface CreateOpinionRequestDto {
  mark?: number | null;
  objective?: readonly string[];
  subjectId: Uuid;
  subjective?: readonly string[];
}

export interface UpdateOpinionRequestDto {
  mark?: number | null;
  objective?: readonly string[];
  opinionId: Uuid;
  subjective?: readonly string[];
}

export interface DeleteOpinionRequestDto {
  opinionId: Uuid;
}

export interface SubmitOpinionForModerationRequestDto {
  opinionId: Uuid;
}

export interface GetModerationOpinionsRequestDto {
  pageable: PageRequestDto;
}

export interface GetModerationDecisionsRequestDto {
  pageable: PageRequestDto;
}

export interface ModerateOpinionRequestDto {
  opinionId: Uuid;
}

export interface RejectOpinionRequestDto {
  opinionId: Uuid;
  reason: string;
}

export interface LinkOpinionComponentRequestDto {
  childOpinionId: Uuid;
  parentOpinionId: Uuid;
  weight: number;
}

export interface UnlinkOpinionComponentRequestDto {
  linkId: Uuid;
}

export interface AdjustOpinionComponentWeightRequestDto {
  linkId: Uuid;
  weight: number;
}

export function createOpinionsApi(client: HttpClient = httpClient) {
  return {
    adjustComponentWeight(
      request: AdjustOpinionComponentWeightRequestDto,
    ): Promise<OpinionDto> {
      return client.patch<OpinionDto>(`/opinions/adjust-weight/${request.linkId}`, {
        auth: "required",
        query: {
          weight: request.weight,
        },
      });
    },

    create(request: CreateOpinionRequestDto): Promise<OpinionDto> {
      return client.post<OpinionDto>("/opinions", {
        auth: "required",
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
        auth: "required",
      });
    },

    submitForModeration(
      request: SubmitOpinionForModerationRequestDto,
    ): Promise<OpinionDto> {
      return client.post<OpinionDto>(
        `/opinions/${request.opinionId}/submit-for-moderation`,
        {
          auth: "required",
        },
      );
    },

    getById(request: GetOpinionByIdRequestDto): Promise<OpinionDto> {
      return client.get<OpinionDto>(`/opinions/${request.id}`, {
        auth: "required",
      });
    },

    getForSubject(
      request: GetOpinionForSubjectRequestDto,
    ): Promise<OpinionDto> {
      return client.get<OpinionDto>(`/opinions/for/${request.subjectId}`, {
        auth: "required",
      });
    },

    getModerationQueue(
      request: GetModerationOpinionsRequestDto,
    ): Promise<PageResponseDto<OpinionDto>> {
      return client.get<PageResponseDto<OpinionDto>>("/opinions/moderation", {
        auth: "required",
        query: createPageQuery(request.pageable),
      });
    },

    getModerationDecisions(
      request: GetModerationDecisionsRequestDto,
    ): Promise<PageResponseDto<ModerationDecisionDto>> {
      return client.get<PageResponseDto<ModerationDecisionDto>>(
        "/opinions/moderation/decisions",
        {
          auth: "required",
          query: createPageQuery(request.pageable),
        },
      );
    },

    approve(request: ModerateOpinionRequestDto): Promise<OpinionDto> {
      return client.post<OpinionDto>(`/opinions/${request.opinionId}/approve`, {
        auth: "required",
      });
    },

    reject(request: RejectOpinionRequestDto): Promise<OpinionDto> {
      return client.post<OpinionDto>(`/opinions/${request.opinionId}/reject`, {
        auth: "required",
        body: {
          reason: request.reason,
        },
      });
    },

    linkComponent(
      request: LinkOpinionComponentRequestDto,
    ): Promise<OpinionDto> {
      return client.post<OpinionDto>("/opinions/link", {
        auth: "required",
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
        auth: "required",
      });
    },

    update(request: UpdateOpinionRequestDto): Promise<OpinionDto> {
      return client.patch<OpinionDto>(`/opinions/${request.opinionId}`, {
        auth: "required",
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
