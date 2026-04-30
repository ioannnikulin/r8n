package com.r8n.backend.opinions.access.service

import com.r8n.backend.opinions.access.database.AccessRequestRepository
import com.r8n.backend.opinions.access.domain.AccessRequest
import com.r8n.backend.opinions.access.domain.OpinionListPermissionEnum
import com.r8n.backend.opinions.access.domain.OpinionPermissionEnum
import com.r8n.backend.opinions.access.domain.RequestStatusEnum
import com.r8n.backend.opinions.access.persistence.AccessRequestPersistence
import com.r8n.backend.opinions.lists.database.OpinionListRepository
import com.r8n.backend.opinions.lists.database.OpinionsToOpinionListsRepository
import com.r8n.backend.opinions.opinions.database.OpinionRepository
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class AccessService(
    private val repository: AccessRequestRepository,
    private val usersClient: UsersInternalApi,
    private val opinionRepository: OpinionRepository,
    private val opinionsToOpinionListsRepository: OpinionsToOpinionListsRepository,
    private val opinionListRepository: OpinionListRepository,
) {
    fun getListOwner(listId: UUID): UUID =
        opinionListRepository
            .findById(listId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
            .owner

    private fun AccessRequestPersistence.toDomain(): AccessRequest =
        AccessRequest(
            id = id,
            listId = list,
            requesterId = requester,
            ownerId = getListOwner(list),
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    fun ownsOpinion(
        userId: UUID,
        opinionId: UUID,
    ): Boolean = opinionRepository.existsByIdAndOwner(opinionId, userId)

    fun ownsOpinionList(
        userId: UUID,
        opinionListId: UUID,
    ): Boolean = opinionListRepository.existsByIdAndOwner(opinionListId, userId)

    private fun roleBasedForOpinion(
        requesterId: UUID,
        ownerId: UUID,
        permission: OpinionPermissionEnum,
    ): Boolean? {
        if (usersClient.isAnyModerator(requesterId) &&
            (permission == OpinionPermissionEnum.READ || permission == OpinionPermissionEnum.REJECT)
        ) {
            return true
        }
        if (usersClient.isHumanModerator(requesterId) && permission == OpinionPermissionEnum.APPROVE) {
            return true
        }
        if (ownerId == requesterId &&
            (
                permission == OpinionPermissionEnum.READ ||
                    permission == OpinionPermissionEnum.EDIT ||
                    permission == OpinionPermissionEnum.DELETE
            )
        ) {
            return true
        }
        if (permission != OpinionPermissionEnum.READ) {
            return false
        }
        return null
    }

    fun canAccessOpinion(
        requesterId: UUID,
        opinionId: UUID,
        permission: OpinionPermissionEnum,
    ): Boolean {
        val ownerId =
            opinionRepository
                .findById(
                    opinionId,
                ).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
                .owner
        val roleBased = roleBasedForOpinion(requesterId, ownerId, permission)
        if (roleBased != null) return roleBased

        // READ-only: fetch all owner's lists that contain this opinion and check if the requester has access to any of them

        val listsContainingOpinion = opinionsToOpinionListsRepository.findAllByOpinion(opinionId)

        return listsContainingOpinion.any { listAssignment ->
            repository.existsByRequesterAndListAndStatus(
                requesterId,
                listAssignment.opinionList,
                RequestStatusEnum.ACCEPTED,
            )
        }
    }

    private fun roleBasedForList(
        userId: UUID,
        ownerId: UUID,
        permission: OpinionListPermissionEnum,
    ): Boolean? {
        if (usersClient.isAnyModerator(userId) &&
            (permission == OpinionListPermissionEnum.VIEW || permission == OpinionListPermissionEnum.HIDE)
        ) {
            return true
        }
        if (ownerId == userId &&
            (
                permission == OpinionListPermissionEnum.VIEW ||
                    permission == OpinionListPermissionEnum.ADD_TO ||
                    permission == OpinionListPermissionEnum.REMOVE_FROM ||
                    permission == OpinionListPermissionEnum.HIDE ||
                    permission == OpinionListPermissionEnum.PUBLISH ||
                    permission == OpinionListPermissionEnum.DELETE ||
                    permission == OpinionListPermissionEnum.RENAME
            )
        ) {
            return true
        }
        if (permission != OpinionListPermissionEnum.VIEW) {
            return false
        }
        return null
    }

    fun countAcceptedForList(listId: UUID): Int =
        repository.countByListAndStatus(listId, RequestStatusEnum.ACCEPTED).toInt()

    fun canAccessOpinionList(
        userId: UUID,
        listId: UUID,
        permission: OpinionListPermissionEnum,
    ): Boolean {
        val ownerId =
            opinionListRepository
                .findById(
                    listId,
                ).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
                .owner
        val roleBased = roleBasedForList(userId, ownerId, permission)
        if (roleBased != null) return roleBased

        // READ-only: has an access request to this list been approved?

        return repository.existsByRequesterAndListAndStatus(userId, listId, RequestStatusEnum.ACCEPTED)
    }
}
