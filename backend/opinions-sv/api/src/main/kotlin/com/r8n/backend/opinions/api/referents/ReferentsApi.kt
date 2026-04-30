package com.r8n.backend.opinions.api.referents

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.opinions.dto.ReferentDto
import com.r8n.backend.opinions.api.referents.dto.CreateReferentRequestDto
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

interface ReferentsApi {
    companion object {
        private const val ROOT_PATH = "/api/referents"
        const val FIND_PATH = "$ROOT_PATH/find"
        const val CREATE_PATH = ROOT_PATH
    }

    @GetMapping(FIND_PATH)
    fun findReferents(
        @RequestParam(required = false)
        query: String?,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<ReferentDto>

    @PostMapping(CREATE_PATH)
    fun createReferent(
        @Valid
        @RequestBody
        request: CreateReferentRequestDto,
    ): ReferentDto
}
