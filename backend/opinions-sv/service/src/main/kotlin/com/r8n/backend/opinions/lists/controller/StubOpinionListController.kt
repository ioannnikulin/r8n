package com.r8n.backend.opinions.lists.controller

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.opinions.api.lists.OpinionListsApi
import com.r8n.backend.opinions.api.lists.dto.OpinionListPrivacyEnumDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import com.r8n.backend.opinions.lists.facade.OpinionListFacade
import com.r8n.backend.opinions.stub.OpinionListTestDataFactory
import com.r8n.backend.security.Authority.IS_USER
import com.r8n.backend.security.CurrentUserIdentifier.getCurrentUserId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class StubOpinionListController(
    private val opinionListFacade: OpinionListFacade,
) : OpinionListsApi {
    @PreAuthorize(IS_USER)
    override fun getListSummary(listId: UUID): OpinionListSummaryDto =
        opinionListFacade.getListSummary(listId, getCurrentUserId())

    @PreAuthorize(IS_USER)
    override fun getList(
        listId: UUID,
        publishedAfter: java.time.Instant?,
    ) = opinionListFacade.getList(listId, getCurrentUserId(), publishedAfter)

    @PreAuthorize(IS_USER)
    override fun createList(
        name: String,
        privacy: OpinionListPrivacyEnumDto,
    ) = opinionListFacade.createList(getCurrentUserId(), name, privacy)

    @PreAuthorize(IS_USER)
    override fun renameList(
        listId: UUID,
        name: String,
    ) = OpinionListTestDataFactory.getList()

    @PreAuthorize(IS_USER)
    override fun changePrivacy(
        listId: UUID,
        privacy: OpinionListPrivacyEnumDto,
    ) = OpinionListTestDataFactory.getList(listId)

    @PreAuthorize(IS_USER)
    override fun linkOpinion(
        listId: UUID,
        opinionId: UUID,
        weight: Double,
    ) = opinionListFacade.linkOpinion(getCurrentUserId(), listId, opinionId, weight)

    @PreAuthorize(IS_USER)
    override fun unlinkOpinion(
        listId: UUID,
        opinionId: UUID,
    ) = opinionListFacade.unlinkOpinion(getCurrentUserId(), listId, opinionId)

    @PreAuthorize(IS_USER)
    override fun search(
        nameSubstring: String?,
        authorId: UUID?,
        authorNameSubstring: String?,
        pageable: PageRequestDto,
    ) = opinionListFacade.searchOpinionListsByName(
        nameSubstring = nameSubstring ?: "",
        requesterId = getCurrentUserId(),
        pageable = pageable,
    )

    @PreAuthorize(IS_USER)
    override fun syncWithOpinionList(
        existingListId: UUID,
        addedListId: UUID,
        weight: Double,
    ) = opinionListFacade.syncWithOpinionList(getCurrentUserId(), existingListId, addedListId, weight)

    @PreAuthorize(IS_USER)
    override fun unsyncWithOpinionList(
        existingListId: UUID,
        removedListId: UUID,
    ) = opinionListFacade.unsyncWithOpinionList(getCurrentUserId(), existingListId, removedListId)

    @PreAuthorize(IS_USER)
    override fun getMine(pageable: PageRequestDto) = opinionListFacade.getMine(getCurrentUserId(), pageable)
}
