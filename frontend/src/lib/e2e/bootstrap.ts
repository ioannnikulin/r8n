import type { QueryClient } from "@tanstack/react-query";
import type { ModerationDecisionDto, OpinionDto } from "@/lib/api/opinions";
import type { PageResponseDto } from "@/lib/api/shared";
import type { UsernameDto } from "@/lib/api/users";
import { setSession } from "@/lib/auth/session";
import { opinionsKeys, usersKeys } from "@/lib/server-state/query-keys";

const E2E_MODERATION_REQUEST = {
  pageable: {
    page: 0,
    size: 50,
    sort: [],
  },
};

export const E2E_PENDING_OPINION_ID = "e2e-opinion-pending";

const e2eModerator: UsernameDto = {
  id: "e2e-moderator",
  name: "E2E Moderator",
  roles: ["ADMIN"],
};

const e2ePendingOpinion: OpinionDto = {
  componentMark: null,
  components: [],
  id: E2E_PENDING_OPINION_ID,
  mark: 8.5,
  objective: ["Receipt from 2026-04-12", "Paid 3.20 EUR"],
  owner: "e2e-reviewer",
  ownerName: "E2E Reviewer",
  status: "PENDING_PREMODERATION",
  subject: "e2e-subject",
  subjective: ["Consistent coffee quality", "Mentions a staff member by name"],
  subjectName: "E2E Espresso Lab",
  timestamp: new Date(Date.now() - 10 * 60_000).toISOString(),
};

const e2eModerationQueue: PageResponseDto<OpinionDto> = {
  items: [e2ePendingOpinion],
  page: 0,
  size: 50,
  total: 1,
};

const e2eModerationDecisions: PageResponseDto<ModerationDecisionDto> = {
  items: [],
  page: 0,
  size: 20,
  total: 0,
};

export function seedE2eQueryData(queryClient: QueryClient): void {
  setSession({
    accessToken: "e2e-access-token",
    expiresInMilliseconds: 60 * 60_000,
  });

  queryClient.setQueryData(usersKeys.me(), e2eModerator);
  queryClient.setQueryData(
    opinionsKeys.moderation(E2E_MODERATION_REQUEST),
    e2eModerationQueue,
  );
  queryClient.setQueryData(
    opinionsKeys.moderationDecisions({ pageable: { page: 0, size: 20, sort: [] } }),
    e2eModerationDecisions,
  );
}
