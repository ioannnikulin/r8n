package com.r8n.backend.mock.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.mock.integration.api.OpinionListInternalApi
import com.r8n.backend.mock.stub.OpinionListTestDataFactory
import com.r8n.backend.security.Authority.IS_USER
import org.springframework.data.domain.PageImpl
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StubOpinionListInternalController : OpinionListInternalApi {
    @PreAuthorize(IS_USER)
    override fun getMineFull(pageable: PageRequestDto) =
        PageImpl(
            listOf(OpinionListTestDataFactory.getList(UUID.fromString("00000000-0000-0000-0000-000000000000"))),
        ).toResponse()
}