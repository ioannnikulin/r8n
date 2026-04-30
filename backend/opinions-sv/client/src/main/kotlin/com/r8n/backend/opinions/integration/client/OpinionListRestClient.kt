package com.r8n.backend.opinions.integration.client

import com.r8n.backend.opinions.api.lists.OpinionListsApi
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.CREATE_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.DELETE_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.GET_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.LINK_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.MOVE_OPINION_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.RENAME_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.SET_PRIVACY_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.SUMMARY_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.SYNC_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.UNLINK_PATH
import com.r8n.backend.opinions.api.lists.OpinionListsApi.Companion.UNSYNC_PATH
import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListPrivacyEnumDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.util.Optional
import java.util.UUID

class OpinionListRestClient(
    private val restClient: RestClient,
) : OpinionListsApi {
    override fun getListSummary(listId: UUID): OpinionListSummaryDto =
        restClient
            .get()
            .uri(SUMMARY_PATH, listId)
            .retrieve()
            .body<OpinionListSummaryDto>()!!

    override fun getList(
        listId: UUID,
        publishedAfter: java.time.Instant?,
    ): OpinionListDto =
        restClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path(GET_PATH)
                    .queryParamIfPresent("publishedAfter", Optional.ofNullable(publishedAfter))
                    .build(listId)
            }.retrieve()
            .body<OpinionListDto>()!!

    override fun createList(
        name: String,
        privacy: OpinionListPrivacyEnumDto,
    ): OpinionListDto =
        restClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path(CREATE_PATH)
                    .queryParam("name", name)
                    .queryParam("privacy", privacy)
                    .build()
            }.retrieve()
            .body<OpinionListDto>()!!

    override fun renameList(
        listId: UUID,
        name: String,
    ): OpinionListDto =
        restClient
            .patch()
            .uri { uriBuilder ->
                uriBuilder.path(RENAME_PATH).queryParam("name", name).build(listId)
            }.retrieve()
            .body<OpinionListDto>()!!

    override fun changePrivacy(
        listId: UUID,
        privacy: OpinionListPrivacyEnumDto,
    ): OpinionListDto =
        restClient
            .patch()
            .uri { uriBuilder ->
                uriBuilder.path(SET_PRIVACY_PATH).queryParam("privacy", privacy).build(listId)
            }.retrieve()
            .body<OpinionListDto>()!!

    override fun deleteList(listId: UUID) {
        restClient
            .delete()
            .uri(DELETE_PATH, listId)
            .retrieve()
            .toBodilessEntity()
    }

    override fun moveOpinion(
        fromListId: UUID,
        toListId: UUID,
        opinionId: UUID,
        weight: Double,
    ): OpinionListDto =
        restClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path(MOVE_OPINION_PATH)
                    .queryParam("toListId", toListId)
                    .queryParam("opinionId", opinionId)
                    .queryParam("weight", weight)
                    .build(fromListId)
            }.retrieve()
            .body<OpinionListDto>()!!

    override fun linkOpinion(
        listId: UUID,
        opinionId: UUID,
        weight: Double,
    ): OpinionListDto =
        restClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path(LINK_PATH)
                    .queryParam("opinionId", opinionId)
                    .queryParam("weight", weight)
                    .build(listId)
            }.retrieve()
            .body<OpinionListDto>()!!

    override fun unlinkOpinion(
        listId: UUID,
        opinionId: UUID,
    ): OpinionListDto =
        restClient
            .patch()
            .uri { uriBuilder ->
                uriBuilder.path(UNLINK_PATH).queryParam("opinionId", opinionId).build(listId)
            }.retrieve()
            .body<OpinionListDto>()!!

    override fun syncWithOpinionList(
        existingListId: UUID,
        addedListId: UUID,
        weight: Double,
    ): OpinionListDto =
        restClient
            .post()
            .uri { uriBuilder ->
                uriBuilder
                    .path(
                        SYNC_PATH,
                    ).queryParam("addedListId", addedListId)
                    .queryParam("weight", weight)
                    .build(existingListId)
            }.retrieve()
            .body<OpinionListDto>()!!

    override fun unsyncWithOpinionList(
        existingListId: UUID,
        removedListId: UUID,
    ): OpinionListDto =
        restClient
            .post()
            .uri { uriBuilder ->
                uriBuilder.path(UNSYNC_PATH).queryParam("removedListId", removedListId).build(existingListId)
            }.retrieve()
            .body<OpinionListDto>()!!
}
