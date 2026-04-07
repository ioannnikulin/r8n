package com.r8n.backend.users.api.dto

import com.r8n.backend.core.api.PageResponseDto

data class PersonalIdentifiableInformationSectionDto(
    val name: String,
    val email: String,
    val sessions: PageResponseDto<UserSessionDto>,
)