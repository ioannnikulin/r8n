package com.r8n.backend.users.integration.api.dto

import com.r8n.backend.core.api.PageResponseDto

data class PersonalIdentifiableInformationSectionDto(
    val name: String,
    val email: String,
    val phone: String? = null,
    val sessions: PageResponseDto<UserSessionDto>,
    val about: String? = null,
    val location: String? = null,
)
