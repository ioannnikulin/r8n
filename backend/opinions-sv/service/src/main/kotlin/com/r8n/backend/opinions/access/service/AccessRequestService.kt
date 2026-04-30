package com.r8n.backend.opinions.access.service

import com.r8n.backend.opinions.access.database.AccessRequestRepository
import com.r8n.backend.opinions.access.domain.AccessRequest
import com.r8n.backend.opinions.access.domain.RequestStatusEnum
import com.r8n.backend.opinions.access.persistence.AccessRequestPersistence
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
        )

    @Transactional
    fun createRequest(
        listId: UUID,
        requesterId: UUID,
    ): AccessRequest {
        if (accessService.getListOwner(listId) == requesterId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot request access to their own list")
        }

        val existing =
            repository
                .findByRequesterAndListAndStatusIn(requesterId, listId, ACTIVE_REQUEST_STATUSES)
                .firstOrNull()
                ?.toDomain()
        if (existing != null) {
            return existing
        }

        val now = Instant.now()
        val request =
            AccessRequestPersistence(
                list = listId,
                requester = requesterId,
                status = RequestStatusEnum.SENT,
                createdAt = now,
                updatedAt = now,
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
        return repository.save(request).toDomain()
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
