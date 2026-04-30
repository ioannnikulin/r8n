package com.r8n.backend.opinions.opinions.service

import com.r8n.backend.opinions.opinions.database.ReferentRepository
import com.r8n.backend.opinions.opinions.domain.SubjectReferent
import com.r8n.backend.opinions.opinions.persistence.ReferentPersistence
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ReferentService(
    private val referentRepository: ReferentRepository,
) {
    @Transactional(readOnly = true)
    fun findReferents(
        query: String?,
        pageable: Pageable,
    ): Page<SubjectReferent> {
        val trimmedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        return if (trimmedQuery != null) {
            referentRepository.findByNameContainingIgnoreCaseOrderByNameAsc(trimmedQuery, pageable)
        } else {
            referentRepository.findAllByOrderByNameAsc(pageable)
        }.map { it.toModel() }
    }

    @Transactional
    fun createReferent(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
    ): SubjectReferent {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank")
        }

        val saved =
            referentRepository.save(
                ReferentPersistence(
                    name = trimmedName,
                    address = address.trimToNull(),
                    latitude = latitude,
                    longitude = longitude,
                    referentGroup = UUID.randomUUID(),
                ),
            )

        return saved.toModel()
    }

    private fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun ReferentPersistence.toModel(): SubjectReferent =
        SubjectReferent(
            id = id!!,
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
        )
}
