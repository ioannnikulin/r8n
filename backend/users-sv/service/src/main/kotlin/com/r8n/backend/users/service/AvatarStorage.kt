package com.r8n.backend.users.service

import com.r8n.backend.users.persistence.AvatarStorageBackendPersistence
import java.util.UUID

interface AvatarStorage {
    fun store(
        userId: UUID,
        content: ByteArray,
    ): StoredAvatar

    fun load(objectKey: String): ByteArray

    fun delete(objectKey: String)
}

data class StoredAvatar(
    val storageBackend: AvatarStorageBackendPersistence,
    val objectKey: String,
)
