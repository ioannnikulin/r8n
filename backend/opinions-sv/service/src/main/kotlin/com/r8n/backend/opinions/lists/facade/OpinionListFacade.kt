package com.r8n.backend.opinions.lists.facade

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.core.utils.toPageable
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListPrivacyEnumDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import com.r8n.backend.opinions.lists.domain.OpinionListPrivacyEnum
import com.r8n.backend.opinions.lists.service.OpinionListService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OpinionListFacade(
    private val opinionListService: OpinionListService,
    private val opinionListMapper: OpinionListMapper,
) {
    fun getListSummary(
        listId: UUID,
        requesterId: UUID,
    ): OpinionListSummaryDto = opinionListMapper.toSummaryDto(opinionListService.getListInfo(listId, requesterId))

    fun getList(
        listId: UUID,
        requesterId: UUID,
        publishedAfter: java.time.Instant? = null,
    ): OpinionListDto = opinionListMapper.toDto(opinionListService.getList(listId, requesterId, publishedAfter))

    fun createList(
        ownerId: UUID,
        name: String,
        privacy: OpinionListPrivacyEnumDto,
    ): OpinionListDto =
        opinionListMapper.toDto(
            opinionListService.createList(
                ownerId = ownerId,
                name = name,
                privacy = privacy.toDomain(),
            ),
        )

    fun getMine(
        ownerId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto> =
        opinionListService
            .getMine(ownerId, pageable.toPageable())
            .map { opinionListMapper.toSummaryDto(it) }
            .toResponse()

    fun getListsFull(
        ownerId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListDto> =
        opinionListService
            .getListsFull(ownerId, pageable.toPageable())
            .map { opinionListMapper.toDto(it) }
            .toResponse()

    fun searchOpinionListsByName(
        nameSubstring: String,
        requesterId: UUID,
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto> =
        opinionListService
            .searchOpinionListsByName(
                nameSubstring = nameSubstring,
                requesterId = requesterId,
                pageable = pageable.toPageable(),
            ).map { opinionListMapper.toSummaryDto(it) }
            .toResponse()

    fun syncWithOpinionList(
        userId: UUID,
        existingListId: UUID,
        addedListId: UUID,
        weight: Double,
    ): OpinionListDto =
        opinionListMapper.toDto(opinionListService.syncWithOpinionList(userId, existingListId, addedListId, weight))

    fun unsyncWithOpinionList(
        userId: UUID,
        existingListId: UUID,
        removedListId: UUID,
    ): OpinionListDto =
        opinionListMapper.toDto(opinionListService.unsyncWithOpinionList(userId, existingListId, removedListId))

    fun linkOpinion(
        userId: UUID,
        listId: UUID,
        opinionId: UUID,
        weight: Double,
    ): OpinionListDto = opinionListMapper.toDto(opinionListService.linkOpinion(userId, listId, opinionId, weight))

    fun unlinkOpinion(
        userId: UUID,
        listId: UUID,
        opinionId: UUID,
    ): OpinionListDto = opinionListMapper.toDto(opinionListService.unlinkOpinion(userId, listId, opinionId))

    private companion object {
        fun OpinionListPrivacyEnumDto.toDomain() =
            when (this) {
                OpinionListPrivacyEnumDto.PRIVATE -> OpinionListPrivacyEnum.PRIVATE
                OpinionListPrivacyEnumDto.SEARCHABLE -> OpinionListPrivacyEnum.SEARCHABLE
            }
    }
}
