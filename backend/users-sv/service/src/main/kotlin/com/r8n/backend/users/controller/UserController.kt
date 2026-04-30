package com.r8n.backend.users.controller

import com.r8n.backend.security.Authority.IS_ADMIN
import com.r8n.backend.security.Authority.IS_USER
import com.r8n.backend.security.CurrentUserIdentifier.getCurrentUserId
import com.r8n.backend.users.api.UsersApi
import com.r8n.backend.users.api.dto.AssignRoleRequestDto
import com.r8n.backend.users.api.dto.RoleEnumDto
import com.r8n.backend.users.api.dto.UpdateMyPublicProfileRequestDto
import com.r8n.backend.users.api.dto.UsernameDto
import com.r8n.backend.users.facade.UserFacade
import com.r8n.backend.users.service.UserAvatarService
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
class UserController(
    private val userFacade: UserFacade,
    private val userAvatarService: UserAvatarService,
) : UsersApi {
    @PreAuthorize(IS_USER)
    override fun getMyName(): UsernameDto = userFacade.getMyName(getCurrentUserId())

    @PreAuthorize(IS_USER)
    override fun getUserProfile(id: UUID) = userFacade.getUserProfile(id)

    @PreAuthorize(IS_USER)
    override fun searchUsers(query: String) = userFacade.searchUsers(getCurrentUserId(), query)

    @PreAuthorize(IS_USER)
    override fun updateMyPublicProfile(request: UpdateMyPublicProfileRequestDto) =
        userFacade.updateMyPublicProfile(getCurrentUserId(), request)

    @PreAuthorize(IS_USER)
    override fun getUserAvatar(id: UUID): ResponseEntity<ByteArray> {
        val avatar = userAvatarService.getAvatar(id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(avatar.contentType))
            .cacheControl(CacheControl.noStore())
            .body(avatar.content)
    }

    @PreAuthorize(IS_USER)
    override fun uploadMyAvatar(file: MultipartFile): ResponseEntity<Void> {
        userAvatarService.uploadAvatar(getCurrentUserId(), file)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize(IS_USER)
    override fun deleteMyAvatar(): ResponseEntity<Void> {
        userAvatarService.deleteAvatar(getCurrentUserId())
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize(IS_ADMIN)
    override fun listUsersWithRoles() = userFacade.listUsersWithRoles()

    @PreAuthorize(IS_ADMIN)
    override fun assignRole(
        userId: UUID,
        request: AssignRoleRequestDto,
    ): ResponseEntity<Void> {
        userFacade.assignRole(getCurrentUserId(), request, userId)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize(IS_ADMIN)
    override fun revokeRole(
        userId: UUID,
        role: String,
    ): ResponseEntity<Void> {
        val roleEnum =
            runCatching { RoleEnumDto.valueOf(role) }.getOrElse {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: $role")
            }
        userFacade.revokeRole(getCurrentUserId(), userId, roleEnum)
        return ResponseEntity.noContent().build()
    }
}
