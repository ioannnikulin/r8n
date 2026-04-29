package com.r8n.backend.opinions.lists.service

import com.r8n.backend.opinions.access.database.AccessRequestRepository
import com.r8n.backend.opinions.access.domain.OpinionListPermissionEnum
import com.r8n.backend.opinions.access.domain.RequestStatusEnum
import com.r8n.backend.opinions.access.service.AccessService
import com.r8n.backend.opinions.lists.database.OpinionListRepository
import com.r8n.backend.opinions.lists.database.OpinionListSyncRepository
import com.r8n.backend.opinions.lists.database.OpinionsToOpinionListsRepository
import com.r8n.backend.opinions.lists.domain.OpinionList
import com.r8n.backend.opinions.lists.domain.OpinionListInfo
import com.r8n.backend.opinions.lists.domain.OpinionListPrivacyEnum
import com.r8n.backend.opinions.lists.domain.OpinionListSearchFilters
import com.r8n.backend.opinions.lists.domain.OpinionSummary
import com.r8n.backend.opinions.lists.persistence.OpinionListPersistence
import com.r8n.backend.opinions.lists.persistence.OpinionListSyncPersistence
import com.r8n.backend.opinions.lists.persistence.OpinionsToOpinionListsPersistence
import com.r8n.backend.opinions.opinions.database.OpinionRepository
import com.r8n.backend.opinions.opinions.database.OpinionSubjectRepository
import com.r8n.backend.opinions.opinions.database.ReferentRepository
import com.r8n.backend.opinions.opinions.domain.Opinion
import com.r8n.backend.opinions.opinions.service.OpinionService
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class OpinionListService(
    private val opinionListRepository: OpinionListRepository,
    private val opinionService: OpinionService,
    private val opinionsAssignmentRepository: OpinionsToOpinionListsRepository,
    private val accessService: AccessService,
    private val syncRepository: OpinionListSyncRepository,
    private val accessRequestRepository: AccessRequestRepository,
    private val usersApi: UsersInternalApi,
    private val referentRepository: ReferentRepository,
    private val subjectRepository: OpinionSubjectRepository,
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

    fun syncWithOpinionList(
        userId: UUID,
        existingListId: UUID,
        addedListId: UUID,
        weight: Double = 1.0,
    ): OpinionList {
        if (!accessService.ownsOpinionList(userId, existingListId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own the destination list")
        }
        val addedList =
            opinionListRepository.findById(addedListId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND)
            }
        if (addedList.privacy == OpinionListPrivacyEnum.PRIVATE) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        if (!accessService.canAccessOpinionList(userId, addedListId, OpinionListPermissionEnum.VIEW)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to the source list")
        }

        val existingSync = syncRepository.findByDestinationListAndSourceList(existingListId, addedListId)
        if (existingSync != null) {
            if (existingSync.weight != weight) {
                existingSync.weight = weight
                syncRepository.save(existingSync)
            }
        } else {
            syncRepository.save(
                OpinionListSyncPersistence(
                    destinationList = existingListId,
                    sourceList = addedListId,
                    weight = weight,
                ),
            )
        }
        return getList(existingListId, userId)
    }

    @Transactional
    fun unsyncWithOpinionList(
        userId: UUID,
        existingListId: UUID,
        removedListId: UUID,
    ): OpinionList {
        if (!accessService.ownsOpinionList(userId, existingListId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own the destination list")
        }

        syncRepository.deleteByDestinationListAndSourceList(existingListId, removedListId)
        return getList(existingListId, userId)
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
    fun linkOpinion(
        userId: UUID,
        listId: UUID,
        opinionId: UUID,
        weight: Double,
    ): OpinionList {
        if (!accessService.ownsOpinionList(userId, listId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this list")
        }
        // check opinion access
        opinionService.getOpinion(opinionId, userId)

        val existing = opinionsAssignmentRepository.findAllByOpinionList(listId).find { it.opinion == opinionId }
        if (existing != null) {
            existing.weight = weight
            opinionsAssignmentRepository.save(existing)
        } else {
            opinionsAssignmentRepository.save(
                OpinionsToOpinionListsPersistence(
                    opinionList = listId,
                    opinion = opinionId,
                    weight = weight,
                ),
            )
        }
        return getList(listId, userId)
    }

    @Transactional
    fun unlinkOpinion(
        userId: UUID,
        listId: UUID,
        opinionId: UUID,
    ): OpinionList {
        if (!accessService.ownsOpinionList(userId, listId)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own this list")
        }
        val assignments = opinionsAssignmentRepository.findAllByOpinionList(listId)
        val toRemove = assignments.filter { it.opinion == opinionId }
        opinionsAssignmentRepository.deleteAll(toRemove)
        return getList(listId, userId)
    }

    fun getList(
        listId: UUID,
        requesterId: UUID,
        publishedAfter: java.time.Instant? = null,
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

    private fun validateAccess(
        list: OpinionListPersistence,
        requesterId: UUID,
    ) {
        val listId = list.id!!
        if (!accessService.canAccessOpinionList(requesterId, listId, OpinionListPermissionEnum.VIEW)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        if (!accessService.ownsOpinionList(requesterId, listId) && list.privacy == OpinionListPrivacyEnum.PRIVATE) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
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
        publishedAfter: java.time.Instant? = null,
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
            opinionsCount = opinionsAssignmentRepository.countByOpinionList(list.id!!),
            grantedAccessCount = accessService.countAcceptedForList(list.id!!),
        )
    }

    fun getMine(
        ownerId: UUID,
        pageable: Pageable,
    ): Page<OpinionListInfo> =
        opinionListRepository.findByOwner(ownerId, pageable).map { list ->
            OpinionListInfo(
                id = list.id!!,
                name = list.name,
                owner = list.owner,
                privacy = list.privacy,
                opinionsCount = opinionsAssignmentRepository.countByOpinionList(list.id!!),
                grantedAccessCount = accessService.countAcceptedForList(list.id!!),
            )
        }

    fun getListsFull(
        ownerId: UUID,
        pageable: Pageable,
    ): Page<OpinionList> =
        opinionListRepository
            .findByOwner(ownerId, pageable)
            .map { getList(it.id!!, ownerId) }

    fun getApprovedListsWithNamesAndOwners(
        requesterId: UUID,
        pageable: Pageable,
    ): Page<OpinionListInfo> {
        val approvedRequests =
            accessRequestRepository.findAllByFilters(
                listId = null,
                requesterId = requesterId,
                ownerId = null,
                status = RequestStatusEnum.ACCEPTED,
                pageable = pageable,
            )

        val listIds = approvedRequests.map { it.list }.content
        val lists = opinionListRepository.findAllById(listIds).associateBy { it.id }

        val infoList =
            approvedRequests.content.mapNotNull { request ->
                val list = lists[request.list]
                if (list != null && list.privacy == OpinionListPrivacyEnum.SEARCHABLE) {
                    OpinionListInfo(
                        id = list.id!!,
                        name = list.name,
                        owner = list.owner,
                        privacy = list.privacy,
                        opinionsCount = opinionsAssignmentRepository.countByOpinionList(list.id!!),
                        grantedAccessCount = accessService.countAcceptedForList(list.id!!),
                    )
                } else {
                    null
                }
            }
        return PageImpl(infoList, pageable, approvedRequests.totalElements)
    }

    @Transactional(readOnly = true)
    fun search(
        requesterId: UUID,
        filters: OpinionListSearchFilters,
        pageable: Pageable,
    ): Page<OpinionListInfo> {
        if (!hasFilters(filters)) {
            return Page.empty(pageable)
        }

        val authorIdsFromName = getAuthorIdsFromName(filters.authorNameSubstring)

        // Base results from primary filters (name and author)
        val resultIds =
            opinionListRepository
                .searchIds(
                    nameSubstring = filters.nameSubstring?.trim()?.takeIf { it.isNotBlank() },
                    authorId = filters.authorId,
                    authorIds = authorIdsFromName,
                    requesterId = requesterId,
                    searchablePrivacy = OpinionListPrivacyEnum.SEARCHABLE,
                ).toMutableSet()

        if (resultIds.isEmpty()) return Page.empty(pageable)

        // Apply additional filters as intersections
        filterByLocationSubstring(resultIds, filters.locationFilter?.containsLocationSubstring)
        if (resultIds.isEmpty()) return Page.empty(pageable)

        filterBySubjectSubstring(resultIds, filters.containsSubjectSubstring)
        if (resultIds.isEmpty()) return Page.empty(pageable)

        filterByLocationRadius(resultIds, filters.locationFilter)
        if (resultIds.isEmpty()) return Page.empty(pageable)

        filterByYoungerThan(resultIds, filters.someOpinionsYoungerThan)
        if (resultIds.isEmpty()) return Page.empty(pageable)

        filterByTextInAny(resultIds, filters.findThisTextInAnyOfTheAbove, requesterId)
        if (resultIds.isEmpty()) return Page.empty(pageable)

        // Fetch all matching items to allow correct sorting and pagination in-memory
        // since some fields (like opinionsCount) are not available for JPA-level sorting
        val allMatchingInfos = opinionListRepository
            .findAllById(resultIds)
            .map { mapToOpinionListInfo(it) }

        val sortedList = if (pageable.sort.isSorted) {
            val ownerNamesMap = if (pageable.sort.any { it.property == "ownerName" }) {
                val ownerIds = allMatchingInfos.map { it.owner }.toSet()
                ownerIds.associateWith { usersApi.getUserName(it) }
            } else {
                emptyMap()
            }

            var comparator: Comparator<OpinionListInfo>? = null
            for (order in pageable.sort) {
                val propComparator: Comparator<OpinionListInfo>? = when (order.property) {
                    "listName", "name" -> compareBy<OpinionListInfo> { it.name.lowercase() }
                    "ownerName" -> compareBy<OpinionListInfo> { ownerNamesMap[it.owner]?.lowercase() ?: "" }
                    "opinionsCount" -> compareBy<OpinionListInfo> { it.opinionsCount }
                    else -> null
                }
                if (propComparator != null) {
                    val directedComparator = if (order.direction.isDescending) {
                        propComparator.reversed()
                    } else {
                        propComparator
                    }
                    comparator = if (comparator == null) directedComparator else comparator.thenComparing(directedComparator)
                }
            }
            if (comparator != null) allMatchingInfos.sortedWith(comparator) else allMatchingInfos
        } else {
            allMatchingInfos
        }

        val total = sortedList.size.toLong()
        val start = pageable.offset.toInt()
        val end = (start + pageable.pageSize).coerceAtMost(sortedList.size)
        
        val content = if (start < sortedList.size) {
            sortedList.subList(start, end)
        } else {
            emptyList()
        }

        return PageImpl(content, pageable, total)
    }

    private fun hasFilters(filters: OpinionListSearchFilters): Boolean =
        !filters.nameSubstring.isNullOrBlank() ||
            filters.authorId != null ||
            !filters.authorNameSubstring.isNullOrBlank() ||
            !filters.locationFilter?.containsLocationSubstring.isNullOrBlank() ||
            filters.someOpinionsYoungerThan != null ||
            !filters.containsSubjectSubstring.isNullOrBlank() ||
            !filters.findThisTextInAnyOfTheAbove.isNullOrBlank() ||
            (
                filters.locationFilter?.latitude != null &&
                    filters.locationFilter.longitude != null &&
                    filters.locationFilter.radiusInMeters != null
            )

    private fun getAuthorIdsFromName(authorNameSubstring: String?): Set<UUID>? =
        if (!authorNameSubstring.isNullOrBlank()) {
            usersApi
                .findUsersByNameSubstring(authorNameSubstring)
                .map { it.id }
                .toSet()
                .ifEmpty { null }
        } else {
            null
        }

    private fun filterByLocationSubstring(
        resultIds: MutableSet<UUID>,
        loc: String?,
    ) {
        loc?.trim()?.takeIf { it.isNotBlank() }?.let { substring ->
            val referentIds = referentRepository.findIdsByAddressContainingIgnoreCase(substring)
            resultIds.retainAll(findListIdsByReferents(referentIds))
        }
    }

    private fun filterBySubjectSubstring(
        resultIds: MutableSet<UUID>,
        sub: String?,
    ) {
        sub?.trim()?.takeIf { it.isNotBlank() }?.let { substring ->
            val subjectIds = subjectRepository.findIdsByNameContainingIgnoreCase(substring)
            resultIds.retainAll(findListIdsBySubjects(subjectIds))
        }
    }

    private fun filterByLocationRadius(
        resultIds: MutableSet<UUID>,
        filter: com.r8n.backend.opinions.lists.domain.LocationFilter?,
    ) {
        val lat = filter?.latitude
        val lng = filter?.longitude
        val radius = filter?.radiusInMeters
        if (lat != null && lng != null && radius != null) {
            val referentIds = referentRepository.findIdsByLocationRadius(lat, lng, radius)
            resultIds.retainAll(findListIdsByReferents(referentIds))
        }
    }

    private fun filterByYoungerThan(
        resultIds: MutableSet<UUID>,
        youngerThan: java.time.Instant?,
    ) {
        youngerThan?.let { timestamp ->
            val opinionIds = opinionRepository.findIdsByTimestampAfter(timestamp)
            resultIds.retainAll(findListIdsByOpinions(opinionIds))
        }
    }

    private fun filterByTextInAny(
        resultIds: MutableSet<UUID>,
        text: String?,
        requesterId: UUID,
    ) {
        text?.trim()?.takeIf { it.isNotBlank() }?.let { substring ->
            val matchedListIds = mutableSetOf<UUID>()

            // 1. Name match
            matchedListIds.addAll(
                opinionListRepository.searchIds(
                    nameSubstring = substring,
                    authorId = null,
                    authorIds = null,
                    requesterId = requesterId,
                    searchablePrivacy = OpinionListPrivacyEnum.SEARCHABLE,
                ),
            )

            // 2. Subject name match
            val subjectIds = subjectRepository.findIdsByNameContainingIgnoreCase(substring)
            matchedListIds.addAll(findListIdsBySubjects(subjectIds))

            // 3. Location match
            val referentIds = referentRepository.findIdsByAddressContainingIgnoreCase(substring)
            matchedListIds.addAll(findListIdsByReferents(referentIds))

            resultIds.retainAll(matchedListIds)
        }
    }

    private fun findListIdsByReferents(referentIds: Set<UUID>): Set<UUID> =
        if (referentIds.isNotEmpty()) {
            findListIdsBySubjects(subjectRepository.findIdsByReferentIn(referentIds))
        } else {
            emptySet()
        }

    private fun findListIdsBySubjects(subjectIds: Set<UUID>): Set<UUID> =
        if (subjectIds.isNotEmpty()) {
            findListIdsByOpinions(opinionRepository.findIdsBySubjectIn(subjectIds))
        } else {
            emptySet()
        }

    private fun findListIdsByOpinions(opinionIds: Set<UUID>): Set<UUID> =
        if (opinionIds.isNotEmpty()) {
            opinionsAssignmentRepository.findOpinionListIdsByOpinionIn(opinionIds)
        } else {
            emptySet()
        }

    private fun mapToOpinionListInfo(list: OpinionListPersistence): OpinionListInfo =
        OpinionListInfo(
            id = list.id!!,
            name = list.name,
            owner = list.owner,
            privacy = list.privacy,
            opinionsCount = opinionsAssignmentRepository.countByOpinionList(list.id!!),
            grantedAccessCount = accessService.countAcceptedForList(list.id!!),
        )

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
