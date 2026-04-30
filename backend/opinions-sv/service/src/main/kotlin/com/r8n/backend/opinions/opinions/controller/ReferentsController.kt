package com.r8n.backend.opinions.opinions.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.opinions.api.referents.ReferentsApi
import com.r8n.backend.opinions.api.referents.dto.CreateReferentRequestDto
import com.r8n.backend.opinions.opinions.facade.ReferentFacade
import com.r8n.backend.security.Authority.IS_USER
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class ReferentsController(
    private val referentFacade: ReferentFacade,
) : ReferentsApi {
    @PreAuthorize(IS_USER)
    override fun findReferents(
        query: String?,
        pageable: PageRequestDto,
    ) = referentFacade.findReferents(query, pageable)

    @PreAuthorize(IS_USER)
    override fun createReferent(request: CreateReferentRequestDto) = referentFacade.createReferent(request)
}
