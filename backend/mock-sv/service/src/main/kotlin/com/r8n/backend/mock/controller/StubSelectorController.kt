package com.r8n.backend.mock.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.mock.api.SelectorApi
import com.r8n.backend.mock.api.dto.about.SelectorDto
import com.r8n.backend.mock.stub.MiscTestFactory
import com.r8n.backend.mock.stub.SelectorTestDataFactory
import com.r8n.backend.security.Authority.IS_USER
import org.springframework.data.domain.PageImpl
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StubSelectorController : SelectorApi {
    @PreAuthorize(IS_USER)
    override fun getForURL(
        url: String,
        pageable: PageRequestDto,
    ) = PageImpl(listOf(SelectorTestDataFactory.getSelector())).toResponse()

    @PreAuthorize(IS_USER)
    override fun getForSubject(
        subjectId: UUID,
        pageable: PageRequestDto,
    ) = PageImpl(listOf(SelectorTestDataFactory.getSelector())).toResponse()

    @PreAuthorize(IS_USER)
    override fun suggest(
        subjectId: UUID,
        selector: String,
    ): SelectorDto = SelectorTestDataFactory.getSelector()

    @PreAuthorize(IS_USER)
    override fun disagree(
        selectorId: UUID,
        comment: String?,
    ) = MiscTestFactory.getSupportMessage()
}