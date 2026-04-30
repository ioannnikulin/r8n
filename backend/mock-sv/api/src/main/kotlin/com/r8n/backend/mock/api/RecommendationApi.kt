package com.r8n.backend.mock.api

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionSubjectDto
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

interface RecommendationApi {
    companion object {
        private const val ROOT_PATH = "/api/recommendations"
        const val SUBJECTS_PATH = "$ROOT_PATH/subjects/{lookingAtSubjectId}"
        const val OPINION_LISTS_PATH = "$ROOT_PATH/opinion-lists/{lookingAtListId}"
        const val HIDE_SUBJECT_PATH = "$ROOT_PATH/subjects/{id}/hide"
        const val HIDE_LIST_PATH = "$ROOT_PATH/opinion-lists/{id}/hide"
        const val HIDDEN_SUBJECTS_PATH = "$ROOT_PATH/subjects/hidden"
        const val HIDDEN_LISTS_PATH = "$ROOT_PATH/opinion-lists/hidden"
        const val UNHIDE_SUBJECT_PATH = "$ROOT_PATH/subjects/{id}/unhide"
        const val UNHIDE_LIST_PATH = "$ROOT_PATH/opinion-lists/{id}/unhide"
    }

    @GetMapping(SUBJECTS_PATH)
    fun getRecommendedSubjects(
        @PathVariable lookingAtSubjectId: UUID,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionSubjectDto>

    @GetMapping(OPINION_LISTS_PATH)
    fun getRecommendedOpinionLists(
        @PathVariable lookingAtListId: UUID,
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto>

    @PatchMapping(HIDE_SUBJECT_PATH)
    fun hideRecommendedSubject(
        @PathVariable id: UUID,
    )

    @PatchMapping(HIDE_LIST_PATH)
    fun hideRecommendedOpinionList(
        @PathVariable id: UUID,
    )

    @GetMapping(HIDDEN_SUBJECTS_PATH)
    fun getHiddenSubjects(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionSubjectDto>

    @GetMapping(HIDDEN_LISTS_PATH)
    fun getHiddenOpinionLists(
        @Valid
        pageable: PageRequestDto,
    ): PageResponseDto<OpinionListSummaryDto>

    @PatchMapping(UNHIDE_SUBJECT_PATH)
    fun unhideSubject(
        @PathVariable id: UUID,
    ): OpinionSubjectDto

    @PatchMapping(UNHIDE_LIST_PATH)
    fun unhideOpinionList(
        @PathVariable id: UUID,
    ): OpinionListSummaryDto
}
