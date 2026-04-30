package com.r8n.backend.users.facade

import com.r8n.backend.users.api.dto.UserStatusEnumDto
import com.r8n.backend.users.domain.Consent
import com.r8n.backend.users.domain.UserSession
import com.r8n.backend.users.domain.UserStatusEnum
import com.r8n.backend.users.integration.api.dto.ConsentDto
import com.r8n.backend.users.integration.api.dto.UserSessionDto

fun UserStatusEnum.toDto(): UserStatusEnumDto =
    when (this) {
        UserStatusEnum.ACTIVE -> UserStatusEnumDto.ACTIVE
        UserStatusEnum.SUSPENDED -> UserStatusEnumDto.SUSPENDED
        UserStatusEnum.DELETION_PENDING -> UserStatusEnumDto.DELETION_PENDING
        UserStatusEnum.DELETED -> UserStatusEnumDto.DELETED
    }

fun Consent.toDto() =
    ConsentDto(
        type = type,
        accepted = accepted,
        session = session.toDto(),
    )

fun UserSession.toDto() =
    UserSessionDto(
        id = id,
        created = created,
        expires = expires,
        ip = ip,
        os = os,
        userAgent = userAgent,
    )
