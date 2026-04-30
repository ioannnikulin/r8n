package com.r8n.backend.opinions.opinions.facade

import com.r8n.backend.opinions.api.opinions.dto.OpinionSubjectDto
import com.r8n.backend.opinions.api.opinions.dto.ReferentDto
import com.r8n.backend.opinions.opinions.domain.SubjectDetails
import com.r8n.backend.opinions.opinions.domain.SubjectReferent
import org.springframework.stereotype.Component

@Component
class SubjectMapper {
    fun toDto(subject: SubjectDetails): OpinionSubjectDto =
        with(subject) {
            OpinionSubjectDto(
                id = id,
                name = name,
                primaryReferent = primaryReferent.toDto(),
                alternativeReferents = alternativeReferents.map { it.toDto() },
            )
        }

    private fun SubjectReferent.toDto(): ReferentDto =
        ReferentDto(
            id = id,
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
        )
}
