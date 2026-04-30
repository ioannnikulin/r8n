package com.r8n.backend.users.service

import com.r8n.backend.users.persistence.AvatarStorageBackendPersistence
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeBytes

@Component
class LocalAvatarStorage(
    @Value("\${r8n.storage.avatar.local-root}") localRoot: String,
) : AvatarStorage {
    private val root: Path = Path.of(localRoot).toAbsolutePath().normalize()

    override fun store(
        userId: UUID,
        content: ByteArray,
    ): StoredAvatar {
        val objectKey = "avatars/$userId/${UUID.randomUUID()}"
        val path = resolveObjectKey(objectKey)

        path.parent.createDirectories()
        path.writeBytes(content)

        return StoredAvatar(
            storageBackend = AvatarStorageBackendPersistence.LOCAL,
            objectKey = objectKey,
        )
    }

    override fun load(objectKey: String): ByteArray {
        val path = resolveObjectKey(objectKey)
        if (!path.exists() || !path.isRegularFile()) {
            throw NoSuchFileException(path.toString())
        }
        return Files.readAllBytes(path)
    }

    override fun delete(objectKey: String) {
        resolveObjectKey(objectKey).deleteIfExists()
    }

    private fun resolveObjectKey(objectKey: String): Path {
        val resolved = root.resolve(objectKey).normalize()
        if (!resolved.startsWith(root)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar object key resolves outside the storage root")
        }
        return resolved
    }
}
