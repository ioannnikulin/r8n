package com.r8n.backend.opinions.opinions.facade

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.core.utils.toPageable
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.opinions.api.opinions.dto.OpinionSubjectDto
import com.r8n.backend.opinions.api.subjects.dto.CreateSubjectRequestDto
import com.r8n.backend.opinions.opinions.service.SubjectService
import org.springframework.stereotype.Component

@Component
class SubjectFacade(
    private val subjectService: SubjectService,
    private val subjectMapper: SubjectMapper,
) {
    fun findSubject(
        query: String?,
        referentId: java.util.UUID?,
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionSubjectDto> =
        subjectService
            .findSubjects(query, referentId, pageable.toPageable())
            .map { subjectMapper.toDto(it) }
            .toResponse()

    fun createSubject(request: CreateSubjectRequestDto): OpinionSubjectDto =
        subjectMapper.toDto(
            subjectService.createSubject(
                name = request.name,
                primaryReferentId = request.primaryReferentId,
                referentName = request.referentName,
                address = request.address,
                latitude = request.latitude,
                longitude = request.longitude,
            ),
        )

    fun setPrimaryReferent(
        subjectId: java.util.UUID,
        referentId: java.util.UUID,
    ): OpinionSubjectDto = subjectMapper.toDto(subjectService.setPrimaryReferent(subjectId, referentId))
}
