package com.r8n.backend.opinions.opinions.facade

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.core.utils.toPageable
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.opinions.api.opinions.dto.ReferentDto
import com.r8n.backend.opinions.api.referents.dto.CreateReferentRequestDto
import com.r8n.backend.opinions.opinions.service.ReferentService
import org.springframework.stereotype.Component

@Component
class ReferentFacade(
    private val referentService: ReferentService,
    private val referentMapper: ReferentMapper,
) {
    fun findReferents(
        query: String?,
        pageable: PageRequestDto,
    ): PageResponseDto<ReferentDto> =
        referentService
            .findReferents(query, pageable.toPageable())
            .map { referentMapper.toDto(it) }
            .toResponse()

    fun createReferent(request: CreateReferentRequestDto): ReferentDto =
        referentMapper.toDto(
            referentService.createReferent(
                name = request.name,
                address = request.address,
                latitude = request.latitude,
                longitude = request.longitude,
            ),
        )
}
