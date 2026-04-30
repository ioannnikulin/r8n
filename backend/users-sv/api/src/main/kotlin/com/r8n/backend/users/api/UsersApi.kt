package com.r8n.backend.users.api

import com.r8n.backend.users.api.dto.AssignRoleRequestDto
import com.r8n.backend.users.api.dto.UpdateMyPublicProfileRequestDto
import com.r8n.backend.users.api.dto.UserProfileDto
import com.r8n.backend.users.api.dto.UserSearchResultDto
import com.r8n.backend.users.api.dto.UserWithRolesDto
import com.r8n.backend.users.api.dto.UsernameDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Tag(name = "Users", description = "User profile, avatar, and administrative role management endpoints.")
interface UsersApi {
    companion object {
        private const val ROOT_PATH = "/api/users"
        const val ME_PATH = "$ROOT_PATH/me"
        const val USER_PATH = "$ROOT_PATH/{id}"
        const val USER_SEARCH_PATH = "$ROOT_PATH/search"
        const val MY_PUBLIC_PROFILE_PATH = "$ME_PATH/public-profile"
        const val MY_AVATAR_PATH = "$ME_PATH/avatar"
        const val USER_AVATAR_PATH = "$USER_PATH/avatar"
        private const val ADMIN_ROOT_PATH = "/api/admin/users"
        const val ADMIN_USERS_PATH = ADMIN_ROOT_PATH
        const val ADMIN_USER_ROLES_PATH = "$ADMIN_ROOT_PATH/{userId}/roles"
        const val ADMIN_USER_ROLE_PATH = "$ADMIN_ROOT_PATH/{userId}/roles/{role}"
    }

    @GetMapping(ME_PATH)
    @Operation(
        summary = "Get my username",
        description = "Returns the display username for the authenticated user.",
    )
    fun getMyName(): UsernameDto

    @GetMapping(USER_PATH)
    @Operation(
        summary = "Get public user profile",
        description = "Returns the public profile fields for a user.",
    )
    fun getUserProfile(
        @Parameter(description = "Public user identifier.")
        @PathVariable
        id: UUID,
    ): UserProfileDto

    @GetMapping(USER_SEARCH_PATH)
    fun searchUsers(
        @RequestParam
        query: String,
    ): List<UserSearchResultDto>

    @PatchMapping(MY_PUBLIC_PROFILE_PATH)
    @Operation(
        summary = "Update my public profile",
        description = "Updates public profile fields for the authenticated user.",
    )
    fun updateMyPublicProfile(
        @RequestBody
        request: UpdateMyPublicProfileRequestDto,
    ): UserProfileDto

    @GetMapping(USER_AVATAR_PATH)
    @Operation(
        summary = "Get user avatar",
        description = "Returns a user's avatar bytes when one is available.",
    )
    fun getUserAvatar(
        @Parameter(description = "Public user identifier.")
        @PathVariable
        id: UUID,
    ): ResponseEntity<ByteArray>

    @PostMapping(
        MY_AVATAR_PATH,
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    @Operation(
        summary = "Upload my avatar",
        description = "Stores or replaces the authenticated user's avatar image.",
    )
    fun uploadMyAvatar(
        @Parameter(description = "Avatar image file.")
        @RequestPart("file")
        file: MultipartFile,
    ): ResponseEntity<Void>

    @DeleteMapping(MY_AVATAR_PATH)
    @Operation(
        summary = "Delete my avatar",
        description = "Removes the authenticated user's stored avatar image.",
    )
    fun deleteMyAvatar(): ResponseEntity<Void>

    @GetMapping(ADMIN_USERS_PATH)
    @Operation(
        summary = "List users with roles",
        description = "Returns users and their assigned roles for administrative review.",
    )
    fun listUsersWithRoles(): List<UserWithRolesDto>

    @PostMapping(ADMIN_USER_ROLES_PATH)
    @Operation(
        summary = "Assign user role",
        description = "Assigns a role to a user. Requires an authenticated administrator.",
    )
    fun assignRole(
        @Parameter(description = "User identifier receiving the role.")
        @PathVariable userId: UUID,
        @RequestBody request: AssignRoleRequestDto,
    ): ResponseEntity<Void>

    @DeleteMapping(ADMIN_USER_ROLE_PATH)
    @Operation(
        summary = "Revoke user role",
        description = "Removes a role from a user. Requires an authenticated administrator.",
    )
    fun revokeRole(
        @Parameter(description = "User identifier losing the role.")
        @PathVariable userId: UUID,
        @Parameter(description = "Role name to revoke.")
        @PathVariable role: String,
    ): ResponseEntity<Void>
}
