package com.r8n.backend.users.service

import com.r8n.backend.users.domain.Consent
import com.r8n.backend.users.provider.database.ConsentRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ConsentService(
    private val consentRepository: ConsentRepository,
    private val sessionService: UserSessionService,
) {
    fun getConsentsForUser(userId: UUID): List<Consent> =
        consentRepository.findAllByUserId(userId).map {
            Consent(
                id = it.id!!,
                type = it.type,
                accepted = it.accepted,
                session = sessionService.getSession(it.session, userId),
            )
        }
}
