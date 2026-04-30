package com.r8n.backend.opinions.opinions.facade

import com.r8n.backend.opinions.api.opinions.dto.ReferentDto
import com.r8n.backend.opinions.opinions.domain.SubjectReferent
import org.springframework.stereotype.Component

@Component
class ReferentMapper {
    fun toDto(referent: SubjectReferent): ReferentDto =
        ReferentDto(
            id = referent.id,
            name = referent.name,
            address = referent.address,
            latitude = referent.latitude,
            longitude = referent.longitude,
        )
}
