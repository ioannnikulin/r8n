package com.r8n.backend.users.service

import com.r8n.backend.users.persistence.AvatarContentTypePersistence
import com.r8n.backend.users.persistence.ProfileAvatarPersistence
import com.r8n.backend.users.provider.database.ProfileAvatarRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.NoSuchFileException
import java.time.Instant
import java.util.UUID

@Service
class UserAvatarService(
    private val profileAvatarRepository: ProfileAvatarRepository,
    private val avatarStorage: AvatarStorage,
    @Value("\${r8n.storage.avatar.max-size}") private val maxSize: DataSize,
) {
    @Transactional
    fun uploadAvatar(
        userId: UUID,
        file: MultipartFile,
    ) {
        val avatarFile = validateAvatarFile(file)
        val previousAvatar = profileAvatarRepository.findByIdOrNull(userId)
        val storedAvatar = avatarStorage.store(userId, avatarFile.content)

        try {
            profileAvatarRepository.save(
                ProfileAvatarPersistence(
                    userId = userId,
                    storageBackend = storedAvatar.storageBackend,
                    objectKey = storedAvatar.objectKey,
                    contentType = avatarFile.contentType,
                    fileSize = avatarFile.content.size.toLong(),
                    updatedAt = Instant.now(),
                ),
            )
        } catch (e: RuntimeException) {
            avatarStorage.delete(storedAvatar.objectKey)
            throw e
        }

        previousAvatar
            ?.objectKey
            ?.takeIf { it != storedAvatar.objectKey }
            ?.let { avatarStorage.delete(it) }
    }

    fun getAvatar(userId: UUID): UserAvatarFile? {
        val avatar =
            profileAvatarRepository.findByIdOrNull(userId)
                ?: return null

        val content =
            try {
                avatarStorage.load(avatar.objectKey)
            } catch (_: NoSuchFileException) {
                return null
            }

        return UserAvatarFile(
            content = content,
            contentType = avatar.contentType.mediaType,
        )
    }

    @Transactional
    fun deleteAvatar(userId: UUID) {
        val avatar = profileAvatarRepository.findByIdOrNull(userId) ?: return

        profileAvatarRepository.delete(avatar)
        avatarStorage.delete(avatar.objectKey)
    }

    private fun validateAvatarFile(file: MultipartFile): ValidatedAvatarFile {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is empty")
        }

        if (file.size > maxSize.toBytes()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file is too large")
        }

        val contentType =
            file.contentType
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar content type is missing")

        val avatarContentType =
            AvatarContentTypePersistence.fromMediaType(contentType)
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar content type is not supported")

        val content = file.bytes
        if (!content.matchesContentType(avatarContentType)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar file content does not match its content type")
        }

        return ValidatedAvatarFile(
            content = content,
            contentType = avatarContentType,
        )
    }

    private fun ByteArray.matchesContentType(contentType: AvatarContentTypePersistence): Boolean =
        when (contentType) {
            AvatarContentTypePersistence.JPEG -> isJpeg()
            AvatarContentTypePersistence.PNG -> isPng()
            AvatarContentTypePersistence.WEBP -> isWebp()
        }

    private fun ByteArray.isJpeg(): Boolean =
        size >= 3 &&
            this[0] == 0xFF.toByte() &&
            this[1] == 0xD8.toByte() &&
            this[2] == 0xFF.toByte()

    private fun ByteArray.isPng(): Boolean {
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        return size >= signature.size && copyOfRange(0, signature.size).contentEquals(signature)
    }

    private fun ByteArray.isWebp(): Boolean =
        size >= 12 &&
            decodeToString(0, 4) == "RIFF" &&
            decodeToString(8, 12) == "WEBP"
}

data class UserAvatarFile(
    val content: ByteArray,
    val contentType: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserAvatarFile

        if (!content.contentEquals(other.content)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

private data class ValidatedAvatarFile(
    val content: ByteArray,
    val contentType: AvatarContentTypePersistence,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ValidatedAvatarFile

        if (!content.contentEquals(other.content)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}
