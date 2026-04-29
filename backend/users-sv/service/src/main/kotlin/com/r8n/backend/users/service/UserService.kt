package com.r8n.backend.users.service

import com.r8n.backend.users.domain.User
import com.r8n.backend.users.domain.UserProfile
import com.r8n.backend.users.domain.UserStatusEnum
import com.r8n.backend.users.domain.UserWithRoles
import com.r8n.backend.users.domain.Username
import com.r8n.backend.users.persistence.RoleEnumPersistence
import com.r8n.backend.users.persistence.UserRoleAssignmentPersistence
import com.r8n.backend.users.provider.database.PIIRepository
import com.r8n.backend.users.provider.database.UserRepository
import com.r8n.backend.users.provider.database.UserRoleAssignmentRepository
import org.springframework.dao.CannotSerializeTransactionException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val consentService: ConsentService,
    private val userRepository: UserRepository,
    private val piiRepository: PIIRepository,
    private val userRoleAssignmentRepository: UserRoleAssignmentRepository,
) {
    private companion object {
        const val NAME_MAX_LENGTH = 255
        const val ABOUT_MAX_LENGTH = 255
        const val LOCATION_MAX_LENGTH = 255
    }

    fun getName(id: UUID) = piiRepository.findByIdOrNull(id)?.name ?: "Unknown"

    fun isAnyModerator(id: UUID): Boolean = isHumanModerator(id) || isAiModerator(id)

    fun isHumanModerator(id: UUID): Boolean =
        hasAnyRole(id, RoleEnumPersistence.MODERATOR, RoleEnumPersistence.SUPPORT, RoleEnumPersistence.ADMIN)

    fun isAiModerator(id: UUID): Boolean = hasRole(id, RoleEnumPersistence.AI_MODERATOR)

    fun isAdmin(id: UUID): Boolean = hasRole(id, RoleEnumPersistence.ADMIN)

    private fun hasRole(
        userId: UUID,
        role: RoleEnumPersistence,
    ): Boolean = userRoleAssignmentRepository.findAllByUser(userId).any { it.role == role }

    private fun hasAnyRole(
        userId: UUID,
        vararg roles: RoleEnumPersistence,
    ): Boolean = userRoleAssignmentRepository.findAllByUser(userId).any { it.role in roles }

    fun getUser(id: UUID): User {
        val userPersistence =
            userRepository.findByIdOrNull(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User $id not found")
        val piiPersistence =
            piiRepository.findByIdOrNull(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "PII for user $id not found")
        val consents = consentService.getConsentsForUser(id)

        return User(
            id = userPersistence.id,
            name = piiPersistence.name,
            email = piiPersistence.email,
            phone = piiPersistence.phone,
            status = userPersistence.status,
            about = piiPersistence.about,
            location = piiPersistence.location,
            statusTimestamp = userPersistence.statusTimestamp,
            consents = consents,
        )
    }

    fun getMyName(userId: UUID): Username {
        val dbRoles = userRoleAssignmentRepository.findAllByUser(userId).map { it.role.name }
        val roles = (listOf("USER") + dbRoles).distinct()
        return Username(userId, getName(userId), roles)
    }

    fun listUsersWithRoles(): List<UserWithRoles> =
        userRepository
            .findAll()
            .filter { it.status != UserStatusEnum.DELETED }
            .map { user ->
                val pii = piiRepository.findByIdOrNull(user.id)
                val roles = userRoleAssignmentRepository.findAllByUser(user.id).map { it.role }
                UserWithRoles(
                    id = user.id,
                    name = pii?.name ?: "Unknown",
                    email = pii?.email ?: "",
                    status = user.status,
                    roles = roles,
                )
            }

    @Transactional
    fun assignRole(
        adminId: UUID,
        userId: UUID,
        role: RoleEnumPersistence,
    ) {
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User $userId not found")
        if (user.status == UserStatusEnum.DELETED) {
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot assign roles to a deleted user")
        }
        if (!userRoleAssignmentRepository.existsByUserAndRole(userId, role)) {
            userRoleAssignmentRepository.save(
                UserRoleAssignmentPersistence(
                    user = userId,
                    role = role,
                    grantedBy = adminId,
                    timestamp = Instant.now(),
                ),
            )
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun revokeRole(
        adminId: UUID,
        userId: UUID,
        role: RoleEnumPersistence,
    ) {
        try {
            if (role == RoleEnumPersistence.ADMIN) {
                if (adminId == userId) {
                    throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove your own admin role")
                }
                if (userRoleAssignmentRepository.countByRoleExcludingStatus(
                        RoleEnumPersistence.ADMIN,
                        UserStatusEnum.DELETED,
                    ) <= 1
                ) {
                    throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot remove the last admin")
                }
            }
            userRoleAssignmentRepository.deleteByUserAndRole(userId, role)
        } catch (_: CannotSerializeTransactionException) {
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot remove the last admin")
        }
    }

    fun getProfile(id: UUID): UserProfile {
        val user =
            userRepository.findByIdOrNull(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User $id not found")
        val pii =
            piiRepository.findByIdOrNull(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "PII for user $id not found")
        return UserProfile(
            id,
            pii.name,
            user.status,
            user.lastSeenAt,
            pii.about,
            pii.location,
        )
    }

    @Transactional
    fun updateProfile(
        userId: UUID,
        name: String,
        about: String?,
        location: String?,
    ): UserProfile {
        val normalizedName = normalizeRequiredText(name, "Name", NAME_MAX_LENGTH)
        val normalizedAbout = normalizeOptionalText(about, "About", ABOUT_MAX_LENGTH)
        val normalizedLocation = normalizeOptionalText(location, "Location", LOCATION_MAX_LENGTH)

        try {
            val updatedRows =
                piiRepository.updatePublicProfile(
                    userId = userId,
                    name = normalizedName,
                    about = normalizedAbout,
                    location = normalizedLocation,
                )
            if (updatedRows == 0) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "PII for user $userId not found")
            }
        } catch (_: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Display name is already taken")
        }

        return getProfile(userId)
    }

    private fun normalizeRequiredText(
        value: String,
        fieldName: String,
        maxLength: Int,
    ): String {
        val normalizedValue = value.trim()
        if (normalizedValue.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName is required")
        }
        if (normalizedValue.length > maxLength) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName must be $maxLength characters or fewer")
        }
        return normalizedValue
    }

    private fun normalizeOptionalText(
        value: String?,
        fieldName: String,
        maxLength: Int,
    ): String? {
        val normalizedValue = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (normalizedValue.length > maxLength) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName must be $maxLength characters or fewer")
        }
        return normalizedValue
    }

    fun findUsersByNameSubstring(nameSubstring: String): List<com.r8n.backend.users.domain.User> =
        piiRepository.findAllByNameContainingIgnoreCase(nameSubstring).map { pii ->
            getUser(pii.userId)
        }
}
