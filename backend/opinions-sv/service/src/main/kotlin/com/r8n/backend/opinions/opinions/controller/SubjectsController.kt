package com.r8n.backend.opinions.opinions.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.opinions.api.subjects.SubjectsApi
import com.r8n.backend.opinions.api.subjects.dto.CreateSubjectRequestDto
import com.r8n.backend.opinions.opinions.facade.SubjectFacade
import com.r8n.backend.security.Authority.IS_USER
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SubjectsController(
    private val subjectFacade: SubjectFacade,
) : SubjectsApi {
    @PreAuthorize(IS_USER)
    override fun findSubject(
        query: String?,
        referentId: UUID?,
        pageable: PageRequestDto,
    ) = subjectFacade.findSubject(query, referentId, pageable)

    @PreAuthorize(IS_USER)
    override fun createSubject(request: CreateSubjectRequestDto) = subjectFacade.createSubject(request)

    @PreAuthorize(IS_USER)
    override fun setPrimaryReferent(
        subjectId: UUID,
        referentId: UUID,
    ) = subjectFacade.setPrimaryReferent(subjectId, referentId)
}
