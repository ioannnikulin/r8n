package com.r8n.backend.opinions.integration.api

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping

interface OpinionListsInternalApi {
    companion object {
        const val MINE_FULL_PATH = "/api/internal/opinion-lists/mine/full"
    }

    @GetMapping(MINE_FULL_PATH)
    fun getMineFull(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListDto>
}
