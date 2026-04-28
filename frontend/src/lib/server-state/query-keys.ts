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
  GetOpinionByIdRequestDto,
  GetOpinionForSubjectRequestDto,
} from "@/lib/api/opinions";
import type {
  GetSelectorsForSubjectRequestDto,
  GetSelectorsForUrlRequestDto,
} from "@/lib/api/selectors";
import type {
  GetMessageThreadsRequestDto,
  GetThreadMessagesRequestDto,
} from "@/lib/api/messages";

export const opinionsKeys = {
  all: ["opinions"] as const,
  detail: (request: GetOpinionByIdRequestDto) => ["opinions", "detail", request.id] as const,
  forSubject: (request: GetOpinionForSubjectRequestDto) => [
    "opinions",
    "for-subject",
    request.subjectId,
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

export const messagesKeys = {
  all: ["messages"] as const,
  threads: (request: GetMessageThreadsRequestDto) => [
    "messages",
    "threads",
    request.pageable,
  ] as const,
  threadMessages: (request: GetThreadMessagesRequestDto) => [
    "messages",
    "thread-messages",
    request.threadId,
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
};

export const queryKeys = {
  opinions: opinionsKeys,
  opinionLists: opinionListsKeys,
  accessRequests: accessRequestsKeys,
  messages: messagesKeys,
  selectors: selectorsKeys,
  users: usersKeys,
};
