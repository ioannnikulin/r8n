import type {
  GetIncomingAccessRequestsRequestDto,
  GetOutgoingAccessRequestsRequestDto,
} from "@/lib/api/access-requests";
import type {
  GetMyOpinionListsRequestDto,
  GetOpinionListRequestDto,
  GetOpinionListSummaryRequestDto,
  SearchOpinionListsRequestDto,
} from "@/lib/api/opinion-lists";
import type {
  GetModerationDecisionsRequestDto,
  GetModerationOpinionsRequestDto,
  GetOpinionByIdRequestDto,
  GetOpinionForSubjectRequestDto,
} from "@/lib/api/opinions";
import type {
  GetSelectorsForSubjectRequestDto,
  GetSelectorsForUrlRequestDto,
} from "@/lib/api/selectors";
import type { FindSubjectsRequestDto } from "@/lib/api/subjects";
import type { FindReferentsRequestDto } from "@/lib/api/referents";

export const opinionsKeys = {
  all: ["opinions"] as const,
  detail: (request: GetOpinionByIdRequestDto) => ["opinions", "detail", request.id] as const,
  forSubject: (request: GetOpinionForSubjectRequestDto) => [
    "opinions",
    "for-subject",
    request.subjectId,
  ] as const,
  moderation: (request: GetModerationOpinionsRequestDto) => [
    "opinions",
    "moderation",
    request.pageable,
  ] as const,
  moderationDecisions: (request: GetModerationDecisionsRequestDto) => [
    "opinions",
    "moderation-decisions",
    request.pageable,
  ] as const,
};

export const opinionListsKeys = {
  all: ["opinion-lists"] as const,
  mine: (request: GetMyOpinionListsRequestDto) => ["opinion-lists", "mine", request.pageable] as const,
  summary: (request: GetOpinionListSummaryRequestDto) => [
    "opinion-lists",
    "summary",
    request.listId,
  ] as const,
  detail: (request: GetOpinionListRequestDto) => [
    "opinion-lists",
    "detail",
    request.listId,
    request.publishedAfter ?? null,
  ] as const,
  search: (request: SearchOpinionListsRequestDto) => [
    "opinion-lists",
    "search",
    request.filters ?? null,
    request.pageable,
  ] as const,
};

export const accessRequestsKeys = {
  all: ["access-requests"] as const,
  incoming: (request: GetIncomingAccessRequestsRequestDto) => [
    "access-requests",
    "incoming",
    request.filters ?? null,
    request.pageable,
  ] as const,
  outgoing: (request: GetOutgoingAccessRequestsRequestDto) => [
    "access-requests",
    "outgoing",
    request.filters ?? null,
    request.pageable,
  ] as const,
};

export const selectorsKeys = {
  all: ["selectors"] as const,
  forSubject: (request: GetSelectorsForSubjectRequestDto) => [
    "selectors",
    "for-subject",
    request.subjectId,
    request.pageable,
  ] as const,
  forUrl: (request: GetSelectorsForUrlRequestDto) => [
    "selectors",
    "for-url",
    request.url,
    request.pageable,
  ] as const,
};

export const usersKeys = {
  all: ["users"] as const,
  me: () => ["users", "me"] as const,
  detail: (id: string) => ["users", "detail", id] as const,
  avatar: (id: string) => ["users", "avatar", id] as const,
  withRoles: () => ["users", "with-roles"] as const,
};

export const subjectsKeys = {
  all: ["subjects"] as const,
  find: (request: FindSubjectsRequestDto) => [
    "subjects",
    "find",
    request.query ?? null,
    request.referentId ?? null,
    request.pageable,
  ] as const,
};

export const referentsKeys = {
  all: ["referents"] as const,
  find: (request: FindReferentsRequestDto) => [
    "referents",
    "find",
    request.query,
    request.pageable,
  ] as const,
};

export const queryKeys = {
  opinions: opinionsKeys,
  opinionLists: opinionListsKeys,
  accessRequests: accessRequestsKeys,
  selectors: selectorsKeys,
  referents: referentsKeys,
  subjects: subjectsKeys,
  users: usersKeys,
};
