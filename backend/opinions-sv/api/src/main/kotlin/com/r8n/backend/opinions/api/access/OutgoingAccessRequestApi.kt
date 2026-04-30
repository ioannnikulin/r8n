package com.r8n.backend.opinions.api.access

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.access.dto.AccessRequestDto
import com.r8n.backend.opinions.api.access.dto.RequestStatusEnumDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Instant
import java.util.UUID

@Tag(
    name = "Outgoing access requests",
    description = "Access requests created by the authenticated user for other users' opinion lists.",
)
interface OutgoingAccessRequestApi {
    companion object {
        private const val ROOT_PATH = "/api/access-requests/outgoing"
        const val GET_PATH = ROOT_PATH
        const val CREATE_PATH = "$ROOT_PATH/create/{listId}"
        const val CANCEL_PATH = "$ROOT_PATH/cancel/{requestId}"
    }

    @GetMapping(GET_PATH)
    @Operation(
        summary = "List outgoing access requests",
        description =
            "Returns access requests created by the authenticated user, " +
                "optionally filtered by list, time, or status.",
    )
    fun get(
        @Parameter(description = "Optional opinion list identifier to filter by.")
        @RequestParam(required = false)
        forListId: UUID?,
        @Parameter(description = "Only include requests created or updated since this timestamp.")
        @RequestParam(required = false)
        since: Instant?,
        @Parameter(description = "Optional request status to filter by.")
        @RequestParam(required = false)
        status: RequestStatusEnumDto?,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<AccessRequestDto>

    @PostMapping(CREATE_PATH)
    @Operation(
        summary = "Create outgoing access request",
        description = "Requests access to an opinion list for the authenticated user.",
    )
    fun create(
        @Parameter(description = "Opinion list identifier to request access to.")
        @PathVariable listId: UUID,
    ): AccessRequestDto

    @PostMapping(CANCEL_PATH)
    @Operation(
        summary = "Cancel outgoing access request",
        description = "Cancels a pending access request created by the authenticated user.",
    )
    fun cancel(
        @Parameter(description = "Access request identifier.")
        @PathVariable requestId: UUID,
    ): AccessRequestDto
}
