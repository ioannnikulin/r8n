package com.r8n.backend.users.provider.database

import com.r8n.backend.users.persistence.ProfileAvatarPersistence
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfileAvatarRepository : JpaRepository<ProfileAvatarPersistence, UUID>
