package com.r8n.backend.opinions.lists.service

import com.r8n.backend.opinions.access.database.AccessRequestRepository
import com.r8n.backend.opinions.access.domain.OpinionListPermissionEnum
import com.r8n.backend.opinions.access.service.AccessService
import com.r8n.backend.opinions.lists.database.OpinionListRepository
import com.r8n.backend.opinions.lists.database.OpinionListSyncRepository
import com.r8n.backend.opinions.lists.database.OpinionsToOpinionListsRepository
import com.r8n.backend.opinions.lists.domain.OpinionList
import com.r8n.backend.opinions.lists.domain.OpinionListInfo
import com.r8n.backend.opinions.lists.domain.OpinionListPrivacyEnum
import com.r8n.backend.opinions.lists.domain.OpinionSummary
import com.r8n.backend.opinions.lists.persistence.OpinionListPersistence
import com.r8n.backend.opinions.lists.persistence.OpinionsToOpinionListsPersistence
import com.r8n.backend.opinions.opinions.database.OpinionRepository
import com.r8n.backend.opinions.opinions.domain.Opinion
import com.r8n.backend.opinions.opinions.service.OpinionService
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class OpinionListService(
    private val opinionListRepository: OpinionListRepository,
    private val opinionService: OpinionService,
    private val opinionsAssignmentRepository: OpinionsToOpinionListsRepository,
    private val accessService: AccessService,
    private val syncRepository: OpinionListSyncRepository,
    private val accessRequestRepository: AccessRequestRepository,
    private val opinionRepository: OpinionRepository,
) {
    @Transactional
    fun createList(
        ownerId: UUID,
        name: String,
        privacy: OpinionListPrivacyEnum,
    ): OpinionList {
        val saved =
            opinionListRepository.save(
                OpinionListPersistence(
                    owner = ownerId,
                    name = name,
                    privacy = privacy,
                ),
            )
        return getList(saved.id!!, ownerId)
    }

    fun getListName(
        listId: UUID,
        userId: UUID,
    ): String {
        val list =
            opinionListRepository
                .findById(listId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (!accessService.ownsOpinionList(userId, listId) &&
            !accessService.canAccessOpinionList(userId, listId, OpinionListPermissionEnum.VIEW) &&
            list.privacy == OpinionListPrivacyEnum.PRIVATE
        ) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return list.name
    }

    @Transactional
    fun deleteList(
        userId: UUID,
        listId: UUID,
    ) {
        if (!accessService.ownsOpinionList(userId, listId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this list")
        }
        if (!opinionListRepository.existsById(listId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        // FK cascades on opinions_to_lists and opinion_lists_syncs handle link / sync cleanup.
        opinionListRepository.deleteById(listId)
    }

    @Transactional
    fun changePrivacy(
        userId: UUID,
        listId: UUID,
        privacy: OpinionListPrivacyEnum,
    ): OpinionList {
        if (!accessService.ownsOpinionList(userId, listId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this list")
        }
        val list =
            opinionListRepository
                .findById(listId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        if (list.privacy != privacy) {
            list.privacy = privacy
            opinionListRepository.save(list)
        }
        return getList(listId, userId)
    }

    @Transactional
    fun renameList(
        userId: UUID,
        listId: UUID,
        name: String,
    ): OpinionList {
        if (!accessService.ownsOpinionList(userId, listId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this list")
        }
        val list =
            opinionListRepository
                .findById(listId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank")
        }
        if (list.name != trimmed) {
            list.name = trimmed
            opinionListRepository.save(list)
        }
        return getList(listId, userId)
    }

    fun getList(
        listId: UUID,
        requesterId: UUID,
        publishedAfter: Instant? = null,
    ): OpinionList {
        val list =
            opinionListRepository
                .findById(listId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }

        validateAccess(list, requesterId)

        val assignments = resolveAllAssignments(listId)
        val opinions = fetchAndEnrichOpinions(assignments, requesterId, publishedAfter)
        val summaries = calculateSummaries(list, opinions, requesterId)

        return toDomain(list, summaries)
    }

    fun getMyVirtualList(ownerId: UUID): OpinionList {
        val opinions =
            opinionService
                .getMyFullOpinions(ownerId, Pageable.unpaged())
                .content
                .map { it.copy(weight = 1.0) }

        val summaries =
            opinions.groupBy { it.subject }.map { (subject, ops) ->
                calculateOwnSummary(subject, ops, ownerId)
            }

        return OpinionList(
            id = null,
            name = "[ALL]",
            owner = ownerId,
            opinionSummaries = summaries,
            privacy = OpinionListPrivacyEnum.PRIVATE,
        )
    }

    fun getMyVirtualListInfo(ownerId: UUID): OpinionListInfo =
        OpinionListInfo(
            id = null,
            name = "[ALL]",
            owner = ownerId,
            privacy = OpinionListPrivacyEnum.PRIVATE,
            opinionsCount = opinionRepository.countByOwner(ownerId),
            grantedAccessCount = 0,
        )

    private fun validateAccess(
        list: OpinionListPersistence,
        requesterId: UUID,
    ) {
        val listId = list.id!!
        if (!accessService.ownsOpinionList(requesterId, listId) && list.privacy == OpinionListPrivacyEnum.PRIVATE) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        if (!accessService.canAccessOpinionList(requesterId, listId, OpinionListPermissionEnum.VIEW)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
    }

    private fun resolveAllAssignments(listId: UUID): List<OpinionsToOpinionListsPersistence> {
        val assignments = opinionsAssignmentRepository.findAllByOpinionList(listId)
        val syncs = syncRepository.findAllByDestinationList(listId)

        val directOpinionWeights = assignments.associate { it.opinion to it.weight }

        val syncedAssignments =
            syncs.flatMap { sync ->
                opinionsAssignmentRepository.findAllByOpinionList(sync.sourceList).mapNotNull { asmt ->
                    if (directOpinionWeights.containsKey(asmt.opinion)) {
                        // ignore synced versions if directly linked
                        null
                    } else {
                        asmt.copy(weight = asmt.weight * sync.weight)
                    }
                }
            }

        // If an opinion is synced from multiple sources, pick the one with max weight
        val dedupedSyncedAssignments =
            syncedAssignments
                .groupBy { it.opinion }
                .map { (_, asmts) -> asmts.maxBy { it.weight } }

        return assignments + dedupedSyncedAssignments
    }

    private fun fetchAndEnrichOpinions(
        assignments: List<OpinionsToOpinionListsPersistence>,
        requesterId: UUID,
        publishedAfter: Instant? = null,
    ): List<Opinion> =
        assignments.mapNotNull { asmt ->
            try {
                val opinion = opinionService.getOpinion(asmt.opinion, requesterId)
                if (publishedAfter != null && opinion.timestamp < publishedAfter) {
                    return@mapNotNull null
                }
                opinion.copy(weight = asmt.weight)
            } catch (e: ResponseStatusException) {
                if (e.statusCode == HttpStatus.FORBIDDEN || e.statusCode == HttpStatus.NOT_FOUND) {
                    null
                } else {
                    throw e
                }
            }
        }

    fun countVisibleOpinions(
        listId: UUID,
        requesterId: UUID,
    ): Long = fetchAndEnrichOpinions(resolveAllAssignments(listId), requesterId).size.toLong()

    private fun calculateSummaries(
        list: OpinionListPersistence,
        opinions: List<Opinion>,
        requesterId: UUID,
    ): List<OpinionSummary> {
        val listId = list.id!!
        val opinionsBySubject = opinions.groupBy { it.subject }

        return if (accessService.ownsOpinionList(requesterId, listId)) {
            opinionsBySubject.map { (subject, ops) ->
                calculateOwnSummary(subject, ops, requesterId)
            }
        } else {
            // for another user I cannot see what his components are, I can see only his own opinion
            opinionsBySubject.mapNotNull { (subject, ops) ->
                val own = ops.firstOrNull { it.owner == list.owner }
                own?.let { createSimpleSummary(subject, it) }
            }
        }
    }

    private fun calculateOwnSummary(
        subject: UUID,
        ops: List<Opinion>,
        requesterId: UUID,
    ): OpinionSummary {
        val own = ops.firstOrNull { it.owner == requesterId }
        val weightedMarks = ops.mapNotNull { it.mark?.let { mark -> it.weight!! * mark } }
        val totalWeight = ops.mapNotNull { if (it.mark != null) it.weight else null }.sum()

        val componentMark =
            if (totalWeight > 0) {
                weightedMarks.sum() / totalWeight
            } else {
                null
            }

        return OpinionSummary(
            subject = subject,
            ownMark = own?.mark,
            componentMark = componentMark,
            opinions = ops,
        )
    }

    private fun createSimpleSummary(
        subject: UUID,
        ownOpinion: Opinion,
    ): OpinionSummary =
        OpinionSummary(
            subject = subject,
            ownMark = ownOpinion.mark,
            componentMark = null,
            opinions = listOf(ownOpinion),
        )

    fun getListInfo(
        listId: UUID,
        requesterId: UUID,
    ): OpinionListInfo {
        val list =
            opinionListRepository
                .findById(listId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        validateAccess(list, requesterId)
        return OpinionListInfo(
            id = list.id!!,
            name = list.name,
            owner = list.owner,
            privacy = list.privacy,
            opinionsCount = countVisibleOpinions(list.id!!, requesterId),
            grantedAccessCount = accessService.countAcceptedForList(list.id!!),
        )
    }

    @Transactional
    fun deleteAllUserDataForUser(userId: UUID) {
        val userLists = opinionListRepository.findAllByOwner(userId)
        val userListIds = userLists.mapNotNull { it.id }

        // Delete all opinions owned by the user
        opinionService.deleteAllOpinionsForUser(userId)

        // Delete all access requests where the user is the requester
        accessRequestRepository.deleteAllByRequester(userId)

        // Delete all access requests targeting lists owned by the user
        if (userListIds.isNotEmpty()) {
            accessRequestRepository.deleteAllByListIn(userListIds)
        }

        // Delete all opinion lists owned by the user
        // This will cascade to delete opinions-to-lists relationships and syncs
        opinionListRepository.deleteAll(userLists)
    }

    private companion object {
        fun toDomain(
            list: OpinionListPersistence,
            summaries: List<OpinionSummary>,
        ) = with(list) {
            OpinionList(
                id = id!!,
                name = name,
                owner = owner,
                opinionSummaries = summaries,
                privacy = privacy,
            )
        }
    }
}
