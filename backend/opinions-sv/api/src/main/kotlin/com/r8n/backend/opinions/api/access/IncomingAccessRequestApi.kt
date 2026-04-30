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
    name = "Incoming access requests",
    description = "Access requests received for opinion lists owned by the authenticated user.",
)
interface IncomingAccessRequestApi {
    companion object {
        private const val ROOT_PATH = "/api/access-requests/incoming"
        const val GET_PATH = ROOT_PATH
        const val ACCEPT_PATH = "$ROOT_PATH/{requestId}/accept"
        const val DECLINE_PATH = "$ROOT_PATH/{requestId}/decline"
        const val HIDE_PATH = "$ROOT_PATH/{requestId}/hide"
    }

    @GetMapping(GET_PATH)
    @Operation(
        summary = "List incoming access requests",
        description =
            "Returns incoming access requests for the authenticated user's opinion lists, " +
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

    @PostMapping(ACCEPT_PATH)
    @Operation(
        summary = "Accept incoming access request",
        description = "Grants access for an incoming request owned by the authenticated user.",
    )
    fun accept(
        @Parameter(description = "Access request identifier.")
        @PathVariable requestId: UUID,
    ): AccessRequestDto

    @PostMapping(DECLINE_PATH)
    @Operation(
        summary = "Decline incoming access request",
        description = "Declines an incoming access request without granting access.",
    )
    fun decline(
        @Parameter(description = "Access request identifier.")
        @PathVariable requestId: UUID,
    ): AccessRequestDto

    @PostMapping(HIDE_PATH)
    @Operation(
        summary = "Hide incoming access request",
        description =
            "Hides an incoming access request from the authenticated user's active request list. " +
                "It keeps showing up as pending for the requestor. You can unhide, approve, or reject it later.",
    )
    fun hide(
        @Parameter(description = "Access request identifier.")
        @PathVariable requestId: UUID,
    ): AccessRequestDto
}
