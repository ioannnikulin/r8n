package com.r8n.backend.mock.controller

import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.mock.api.MessagingApi
import com.r8n.backend.mock.stub.MiscTestFactory
import com.r8n.backend.security.Authority.IS_USER
import org.springframework.data.domain.PageImpl
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController
class StubMessagingController : MessagingApi {
    @PreAuthorize(IS_USER)
    override fun getSupportThreads() =
        PageImpl(
            listOf(MiscTestFactory.getSupportMessage()),
        ).toResponse()
}