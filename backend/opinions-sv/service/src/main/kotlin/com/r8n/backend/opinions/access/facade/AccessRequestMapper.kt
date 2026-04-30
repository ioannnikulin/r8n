package com.r8n.backend.opinions.access.facade

import com.r8n.backend.opinions.access.domain.AccessRequest
import com.r8n.backend.opinions.access.domain.RequestStatusEnum
import com.r8n.backend.opinions.api.access.dto.AccessRequestDto
import com.r8n.backend.opinions.api.access.dto.RequestStatusEnumDto
import com.r8n.backend.opinions.lists.service.OpinionListService
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AccessRequestMapper(
    private val userClient: UsersInternalApi,
    private val opinionListService: OpinionListService,
) {
    fun toDomain(status: RequestStatusEnumDto?) =
        when (status) {
            null -> null
            RequestStatusEnumDto.SENT -> RequestStatusEnum.SENT
            RequestStatusEnumDto.ACCEPTED -> RequestStatusEnum.ACCEPTED
            RequestStatusEnumDto.REJECTED -> RequestStatusEnum.REJECTED
            RequestStatusEnumDto.HIDDEN -> RequestStatusEnum.HIDDEN
            RequestStatusEnumDto.CANCELLED -> RequestStatusEnum.CANCELLED
        }

    fun RequestStatusEnum.toDto() =
        when (this) {
            RequestStatusEnum.SENT -> RequestStatusEnumDto.SENT
            RequestStatusEnum.ACCEPTED -> RequestStatusEnumDto.ACCEPTED
            RequestStatusEnum.REJECTED -> RequestStatusEnumDto.REJECTED
            RequestStatusEnum.HIDDEN -> RequestStatusEnumDto.HIDDEN
            RequestStatusEnum.CANCELLED -> RequestStatusEnumDto.CANCELLED
        }

    fun toDto(
        request: AccessRequest,
        viewerId: UUID,
    ): AccessRequestDto =
        with(request) {
            AccessRequestDto(
                id = id!!,
                opinionListId = listId,
                opinionListName =
                    try {
                        opinionListService.getListName(listId, viewerId)
                    } catch (_: Exception) {
                        "UNKNOWN"
                    },
                owner = ownerId,
                ownerName =
                    try {
                        userClient.getUserName(ownerId)
                    } catch (_: Exception) {
                        "UNKNOWN"
                    },
                requester = requesterId,
                requesterName =
                    try {
                        userClient.getUserName(requesterId)
                    } catch (_: Exception) {
                        "UNKNOWN"
                    },
                timestamp = updatedAt,
                status = status.toDto(),
            )
        }
}
