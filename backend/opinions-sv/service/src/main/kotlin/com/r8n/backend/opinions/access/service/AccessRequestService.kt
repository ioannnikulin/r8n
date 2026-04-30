package com.r8n.backend.opinions.access.service

import com.r8n.backend.opinions.access.database.AccessRequestRepository
import com.r8n.backend.opinions.access.domain.AccessRequest
import com.r8n.backend.opinions.access.domain.AccessRequestIntent
import com.r8n.backend.opinions.access.domain.RequestStatusEnum
import com.r8n.backend.opinions.access.persistence.AccessRequestPersistence
import com.r8n.backend.opinions.lists.domain.OpinionListPrivacyEnum
import com.r8n.backend.opinions.lists.service.OpinionListService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class AccessRequestService(
    private val repository: AccessRequestRepository,
    private val accessService: AccessService,
    private val opinionListService: OpinionListService,
) {
    private companion object {
        val ACTIVE_REQUEST_STATUSES =
            listOf(
                RequestStatusEnum.SENT,
                RequestStatusEnum.HIDDEN,
                RequestStatusEnum.ACCEPTED,
            )
    }

    fun getRequests(
        listId: UUID?,
        requesterId: UUID?,
        ownerId: UUID?,
        since: Instant?,
        status: RequestStatusEnum?,
        pageable: Pageable,
    ): Page<AccessRequest> =
        (
            since
                ?.let { repository.findAllByFiltersUpdatedSince(listId, requesterId, ownerId, it, status, pageable) }
                ?: repository.findAllByFilters(listId, requesterId, ownerId, status, pageable)
        ).map {
            it.toDomain().apply {
                // If the owner has hidden the request, the requester shouldn't know.
                if (requesterId != null && ownerId != requesterId && this.status == RequestStatusEnum.HIDDEN) {
                    this.status = RequestStatusEnum.SENT
                }
            }
        }

    private fun AccessRequestPersistence.toDomain(): AccessRequest =
        AccessRequest(
            id = id,
            listId = list,
            requesterId = requester,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            ownerId = accessService.getListOwner(list),
            intent = intent,
            targetListId = targetListId,
        )

    @Transactional
    fun createRequest(
        listId: UUID,
        requesterId: UUID,
        intent: AccessRequestIntent = AccessRequestIntent.NONE,
        targetListId: UUID? = null,
    ): AccessRequest {
        if (accessService.getListOwner(listId) == requesterId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot request access to their own list")
        }

        if (intent == AccessRequestIntent.MERGE) {
            if (targetListId == null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "MERGE intent requires targetListId")
            }
            if (!accessService.ownsOpinionList(requesterId, targetListId)) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You don't own the target list")
            }
        } else if (targetListId != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "targetListId is only valid for MERGE intent")
        }

        // Idempotent for the pending row, but keep the requester's latest intent.
        // Terminal states (CANCELLED, REJECTED) and ACCEPTED do not block creating a new SENT row.
        val existingSent =
            repository.findFirstByRequesterAndListAndStatus(requesterId, listId, RequestStatusEnum.SENT)
        if (existingSent != null) {
            if (existingSent.intent != intent || existingSent.targetListId != targetListId) {
                existingSent.intent = intent
                existingSent.targetListId = targetListId
                existingSent.updatedAt = Instant.now()
            }
            return repository.save(existingSent).toDomain()
        }

        val now = Instant.now()
        val request =
            AccessRequestPersistence(
                list = listId,
                requester = requesterId,
                status = RequestStatusEnum.SENT,
                createdAt = now,
                updatedAt = now,
                intent = intent,
                targetListId = targetListId,
            )
        return repository.save(request).toDomain()
    }

    @Transactional
    fun cancelRequest(
        requestId: UUID,
        requesterId: UUID,
    ): AccessRequest {
        val request =
            repository
                .findById(requestId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found") }

        if (request.requester != requesterId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot cancel someone else's request")
        }

        if (request.status == RequestStatusEnum.CANCELLED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Request already cancelled")
        }

        request.status = RequestStatusEnum.CANCELLED
        request.updatedAt = Instant.now()
        return repository.save(request).toDomain()
    }

    @Transactional
    fun acceptRequest(
        requestId: UUID,
        ownerId: UUID,
    ): AccessRequest {
        val request =
            repository
                .findById(requestId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found") }

        if (accessService.getListOwner(request.list) != ownerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can accept a request")
        }

        if (request.status != RequestStatusEnum.SENT && request.status != RequestStatusEnum.HIDDEN) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only accept a pending or hidden request")
        }

        request.status = RequestStatusEnum.ACCEPTED
        request.updatedAt = Instant.now()
        val acceptedRequest = repository.save(request)
        executeAcceptedIntent(acceptedRequest)
        return acceptedRequest.toDomain()
    }

    private fun executeAcceptedIntent(request: AccessRequestPersistence) {
        when (request.intent) {
            AccessRequestIntent.NONE -> Unit
            AccessRequestIntent.COPY -> {
                val sourceName = opinionListService.getListName(request.list, request.requester)
                val copiedList =
                    opinionListService.createList(
                        ownerId = request.requester,
                        name = "Copy of $sourceName",
                        privacy = OpinionListPrivacyEnum.PRIVATE,
                    )
                opinionListService.syncWithOpinionList(
                    userId = request.requester,
                    existingListId = copiedList.id,
                    addedListId = request.list,
                    weight = 1.0,
                )
            }
            AccessRequestIntent.MERGE -> {
                val targetListId =
                    request.targetListId
                        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "MERGE intent requires targetListId")
                opinionListService.syncWithOpinionList(
                    userId = request.requester,
                    existingListId = targetListId,
                    addedListId = request.list,
                    weight = 1.0,
                )
            }
        }
    }

    @Transactional
    fun declineRequest(
        requestId: UUID,
        ownerId: UUID,
    ): AccessRequest {
        val request =
            repository
                .findById(requestId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found") }

        if (accessService.getListOwner(request.list) != ownerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can decline a request")
        }

        if (request.status == RequestStatusEnum.REJECTED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Request already rejected")
        }

        if (request.status == RequestStatusEnum.CANCELLED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot decline a cancelled request")
        }

        request.status = RequestStatusEnum.REJECTED
        request.updatedAt = Instant.now()
        return repository.save(request).toDomain()
    }

    @Transactional
    fun hideRequest(
        requestId: UUID,
        ownerId: UUID,
    ): AccessRequest {
        val request =
            repository
                .findById(requestId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found") }

        if (accessService.getListOwner(request.list) != ownerId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can hide a request")
        }

        if (request.status != RequestStatusEnum.SENT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only hide a pending request")
        }

        request.status = RequestStatusEnum.HIDDEN
        request.updatedAt = Instant.now()
        return repository.save(request).toDomain()
    }
}
