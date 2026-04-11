package com.r8n.backend.mock.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.utils.toResponse
import com.r8n.backend.mock.api.OutgoingAccessRequestApi
import com.r8n.backend.mock.api.dto.access.RequestStatusEnumDto
import com.r8n.backend.mock.stub.AccessRequestsTestDataFactory
import com.r8n.backend.security.Authority.IS_USER
import org.springframework.data.domain.PageImpl
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
class StubOutgoingAccessRequestController : OutgoingAccessRequestApi {
    @PreAuthorize(IS_USER)
    override fun get(
        forListId: UUID?,
        since: Instant?,
        status: RequestStatusEnumDto?,
        pageable: PageRequestDto,
    ) = PageImpl(
        listOf(
            AccessRequestsTestDataFactory.get(status = status ?: RequestStatusEnumDto.SENT),
            AccessRequestsTestDataFactory.get(status = status ?: RequestStatusEnumDto.SENT),
            AccessRequestsTestDataFactory.get(status = status ?: RequestStatusEnumDto.SENT),
        ),
    ).toResponse()

    @PreAuthorize(IS_USER)
    override fun create(listId: UUID) = AccessRequestsTestDataFactory.get(status = RequestStatusEnumDto.SENT)

    @PreAuthorize(IS_USER)
    override fun cancel(requestId: UUID) = AccessRequestsTestDataFactory.get(status = RequestStatusEnumDto.CANCELLED)
}