package com.r8n.backend.opinions.integration.client

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.opinions.api.opinions.OpinionsApi
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.ADJUST_WEIGHT_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.APPROVE_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.CREATE_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.DELETE_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.GET_BY_ID_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.GET_FOR_SUBJECT_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.LINK_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.MODERATION_DECISIONS_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.MODERATION_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.REJECT_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.SUBMIT_FOR_MODERATION_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.UNLINK_PATH
import com.r8n.backend.opinions.api.opinions.OpinionsApi.Companion.UPDATE_PATH
import com.r8n.backend.opinions.api.opinions.dto.ModerationDecisionDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionDto
import com.r8n.backend.opinions.api.opinions.dto.RejectOpinionRequestDto
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.UUID

class OpinionsRestClient(
    private val restClient: RestClient,
) : OpinionsApi {
    override fun getOpinionById(id: UUID): OpinionDto =
        restClient
            .get()
            .uri(GET_BY_ID_PATH, id)
            .retrieve()
            .body<OpinionDto>()!!

    override fun getOpinionFor(subjectId: UUID): OpinionDto =
        restClient
            .get()
            .uri(GET_FOR_SUBJECT_PATH, subjectId)
            .retrieve()
            .body<OpinionDto>()!!

    override fun createOpinion(
        subjectId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
    ): OpinionDto =
        restClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path(CREATE_PATH)
                    .queryParam("subjectId", subjectId)
                    .apply {
                        if (subjective.isNotEmpty()) queryParam("subjective", *subjective.toTypedArray())
                        if (objective.isNotEmpty()) queryParam("objective", *objective.toTypedArray())
                        mark?.let { queryParam("mark", it) }
                    }.build()
            }.retrieve()
            .body<OpinionDto>()!!

    override fun updateOpinion(
        opinionId: UUID,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
    ): OpinionDto =
        restClient
            .patch()
            .uri { uriBuilder ->
                uriBuilder
                    .path(UPDATE_PATH)
                    .apply {
                        if (subjective.isNotEmpty()) queryParam("subjective", *subjective.toTypedArray())
                        if (objective.isNotEmpty()) queryParam("objective", *objective.toTypedArray())
                        mark?.let { queryParam("mark", it) }
                    }.build(opinionId)
            }.retrieve()
            .body<OpinionDto>()!!

    override fun deleteOpinion(opinionId: UUID) {
        restClient
            .delete()
            .uri(DELETE_PATH, opinionId)
            .retrieve()
            .toBodilessEntity()
    }

    override fun submitOpinionForModeration(opinionId: UUID): OpinionDto =
        restClient
            .post()
            .uri(SUBMIT_FOR_MODERATION_PATH, opinionId)
            .retrieve()
            .body<OpinionDto>()!!

    override fun getModerationOpinions(pageable: PageRequestDto): PageResponseDto<OpinionDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(MODERATION_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<OpinionDto>>()!!

    override fun getModerationDecisions(pageable: PageRequestDto): PageResponseDto<ModerationDecisionDto> =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(MODERATION_DECISIONS_PATH)
                    .queryParam("page", pageable.page)
                    .queryParam("size", pageable.size)
                    .apply {
                        pageable.sort.forEach {
                            queryParam("sort", "${it.property},${it.direction}")
                        }
                    }.build()
            }.retrieve()
            .body<PageResponseDto<ModerationDecisionDto>>()!!

    override fun approveOpinion(opinionId: UUID): OpinionDto =
        restClient
            .post()
            .uri(APPROVE_PATH, opinionId)
            .retrieve()
            .body<OpinionDto>()!!

    override fun rejectOpinion(
        opinionId: UUID,
        request: RejectOpinionRequestDto,
    ): OpinionDto =
        restClient
            .post()
            .uri(REJECT_PATH, opinionId)
            .body(request)
            .retrieve()
            .body<OpinionDto>()!!

    override fun linkComponent(
        parentOpinionId: UUID,
        childOpinionId: UUID,
        weight: Double,
    ): OpinionDto =
        restClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path(LINK_PATH)
                    .queryParam("parentOpinionId", parentOpinionId)
                    .queryParam("childOpinionId", childOpinionId)
                    .queryParam("weight", weight)
                    .build()
            }.retrieve()
            .body<OpinionDto>()!!

    override fun unlinkComponent(linkId: UUID): OpinionDto =
        restClient
            .delete()
            .uri(UNLINK_PATH, linkId)
            .retrieve()
            .body<OpinionDto>()!!

    override fun adjustComponentWeight(
        linkId: UUID,
        weight: Double,
    ): OpinionDto =
        restClient
            .patch()
            .uri { uriBuilder ->
                uriBuilder
                    .path(ADJUST_WEIGHT_PATH)
                    .queryParam("weight", weight)
                    .build(linkId)
            }.retrieve()
            .body<OpinionDto>()!!
}
