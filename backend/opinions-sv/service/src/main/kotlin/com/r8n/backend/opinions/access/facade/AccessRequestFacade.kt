package com.r8n.backend.opinions.access.facade

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.core.utils.toPageable
import com.r8n.backend.opinions.access.service.AccessRequestService
import com.r8n.backend.opinions.api.access.dto.AccessRequestDto
import com.r8n.backend.opinions.api.access.dto.RequestStatusEnumDto
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class AccessRequestFacade(
    private val service: AccessRequestService,
    private val accessRequestMapper: AccessRequestMapper,
) {
    fun getIncoming(
        forListId: UUID?,
        since: Instant?,
        status: RequestStatusEnumDto?,
        pageable: PageRequestDto,
        ownerId: UUID,
    ): PageResponseDto<AccessRequestDto> {
        val page =
            service.getRequests(
                forListId,
                null,
                ownerId,
                since,
                accessRequestMapper.toDomain(status),
                pageable.toPageable(),
            )
        return PageResponseDto(
            items = page.content.map { accessRequestMapper.toDto(it, ownerId) },
            total = page.totalElements,
            page = pageable.page,
            size = pageable.size,
        )
    }

    fun getOutgoing(
        forListId: UUID?,
        since: Instant?,
        status: RequestStatusEnumDto?,
        pageable: PageRequestDto,
        requesterId: UUID,
    ): PageResponseDto<AccessRequestDto> {
        val page =
            service.getRequests(
                forListId,
                requesterId,
                null,
                since,
                accessRequestMapper.toDomain(status),
                pageable.toPageable(),
            )
        return PageResponseDto(
            items = page.content.map { accessRequestMapper.toDto(it, requesterId) },
            total = page.totalElements,
            page = pageable.page,
            size = pageable.size,
        )
    }

    fun createRequest(
        listId: UUID,
        requesterId: UUID,
    ): AccessRequestDto = accessRequestMapper.toDto(service.createRequest(listId, requesterId), requesterId)

    fun cancelRequest(
        requestId: UUID,
        requesterId: UUID,
    ): AccessRequestDto = accessRequestMapper.toDto(service.cancelRequest(requestId, requesterId), requesterId)

    fun acceptRequest(
        requestId: UUID,
        ownerId: UUID,
    ): AccessRequestDto = accessRequestMapper.toDto(service.acceptRequest(requestId, ownerId), ownerId)

    fun declineRequest(
        requestId: UUID,
        ownerId: UUID,
    ): AccessRequestDto = accessRequestMapper.toDto(service.declineRequest(requestId, ownerId), ownerId)

    fun hideRequest(
        requestId: UUID,
        ownerId: UUID,
    ): AccessRequestDto = accessRequestMapper.toDto(service.hideRequest(requestId, ownerId), ownerId)
}
