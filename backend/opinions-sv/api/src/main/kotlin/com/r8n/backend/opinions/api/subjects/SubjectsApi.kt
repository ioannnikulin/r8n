package com.r8n.backend.opinions.api.subjects

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionSubjectDto
import com.r8n.backend.opinions.api.subjects.dto.CreateSubjectRequestDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

@Tag(name = "Subjects", description = "Reviewable subjects that opinions can be written about.")
interface SubjectsApi {
    companion object {
        private const val ROOT_PATH = "/api/subjects"
        const val FIND_PATH = "$ROOT_PATH/find"
        const val CREATE_PATH = ROOT_PATH
        const val SET_PRIMARY_REFERENT_PATH = "$ROOT_PATH/{subjectId}/set-primary-referent"
    }

    @GetMapping(FIND_PATH)
    @Operation(
        summary = "Find subjects",
        description = "Searches reviewable subjects by text and returns paged matches.",
    )
    fun findSubject(
        @RequestParam(required = false)
        query: String?,
        @RequestParam(required = false)
        referentId: UUID?,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionSubjectDto>

    @PostMapping(CREATE_PATH)
    @Operation(
        summary = "Create subject",
        description = "Creates a reviewable subject when it does not already exist.",
    )
    fun createSubject(
        @Valid
        @RequestBody
        request: CreateSubjectRequestDto,
    ): OpinionSubjectDto

    @PatchMapping(SET_PRIMARY_REFERENT_PATH)
    @Operation(
        summary = "Set primary referent",
        description = "Sets the primary referent used to identify or disambiguate a subject.",
    )
    fun setPrimaryReferent(
        @Parameter(description = "Subject identifier.")
        @PathVariable subjectId: UUID,
        @Parameter(description = "Referent identifier to make primary.")
        @RequestParam(required = true) referentId: UUID,
    ): OpinionSubjectDto
}
