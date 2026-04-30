package com.r8n.backend.export.service

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import com.r8n.backend.messaging.api.MessagingApi
import com.r8n.backend.messaging.api.dto.SupportThreadDto
import com.r8n.backend.opinions.api.access.IncomingAccessRequestApi
import com.r8n.backend.opinions.api.access.OutgoingAccessRequestApi
import com.r8n.backend.opinions.integration.api.OpinionListsInternalApi
import com.r8n.backend.users.integration.api.UsersInternalApi
import com.r8n.backend.users.integration.api.dto.PersonalIdentifiableInformationSectionDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class DataExportService(
    private val usersClient: UsersInternalApi,
    private val opinionClient: OpinionListsInternalApi,
    private val incomingAccessRequestClient: IncomingAccessRequestApi,
    private val outgoingAccessRequestClient: OutgoingAccessRequestApi,
    private val messageClient: MessagingApi,
) {
    private val logger = LoggerFactory.getLogger(DataExportService::class.java)
    private val exportJobs = ConcurrentHashMap<UUID, ExportJob>()

    data class ExportJob(
        val userId: UUID,
        val status: ExportJobStatus,
        val createdAt: Instant,
        val completedAt: Instant? = null,
        val result: UserCompleteDataDto? = null,
    )

    enum class ExportJobStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED,
    }

    fun startExport(userId: UUID) {
        logger.debug("Starting export for user: {}", userId)
        cancelExistingJob(userId)
        exportJobs[userId] = ExportJob(userId, ExportJobStatus.PENDING, Instant.now())

        // For now, process synchronously to ensure security context is properly propagated
        // In a real implementation, this would be an async job
        try {
            updateExportStatus(userId, ExportJobStatus.IN_PROGRESS)
            logger.debug("Fetching user data for user: {}", userId)
            val data = fetchUserCompleteData(userId)
            logger.debug("User data fetched successfully for user: {}", userId)
            completeExport(userId, data)
            logger.debug("Export completed successfully for user: {}", userId)
        } catch (e: Exception) {
            logger.error("Export failed for user: $userId", e)
            failExport(userId)
        }
    }

    fun getJob(userId: UUID): ExportJob? = exportJobs[userId]

    private fun fetchUserCompleteData(userId: UUID): UserCompleteDataDto {
        val user = usersClient.getUser(userId)
        val sessions = usersClient.getSessionsForUser(userId, PageRequestDto(0, Int.MAX_VALUE))

        return UserCompleteDataDto(
            id = user.id,
            status = user.status,
            statusTimestamp = user.statusTimestamp,
            consents =
                PageResponseDto(
                    items = user.consents,
                    total = user.consents.size.toLong(),
                    page = 0,
                    size = user.consents.size,
                ),
            personalIdentifiableInformation =
                PersonalIdentifiableInformationSectionDto(
                    name = user.name,
                    email = user.email,
                    sessions = sessions,
                ),
            opinions = opinionClient.getMineFull(PageRequestDto(0, Int.MAX_VALUE)),
            outgoingRequests = outgoingAccessRequestClient.get(null, null, null, PageRequestDto(0, Int.MAX_VALUE)),
            incomingRequests = incomingAccessRequestClient.get(null, null, null, PageRequestDto(0, Int.MAX_VALUE)),
            messages = getSupportThreads(),
        )
    }

    private fun getSupportThreads(): PageResponseDto<SupportThreadDto> {
        val threads = messageClient.getSupportThreadSummaries(PageRequestDto(0, Int.MAX_VALUE))
        val items =
            threads.items.map { thread ->
                val messages = messageClient.getSupportThreadMessages(thread.id, PageRequestDto(0, Int.MAX_VALUE))
                SupportThreadDto(
                    id = thread.id,
                    messages = messages.items.map { it.text },
                )
            }

        return PageResponseDto(
            items = items,
            total = threads.total,
            page = threads.page,
            size = threads.size,
        )
    }

    private fun cancelExistingJob(userId: UUID) {
        exportJobs[userId]?.let {
            exportJobs[userId] = it.copy(status = ExportJobStatus.CANCELLED)
        }
    }

    private fun updateExportStatus(
        userId: UUID,
        status: ExportJobStatus,
    ) {
        exportJobs[userId]?.let { job ->
            exportJobs[userId] = job.copy(status = status)
        }
    }

    private fun completeExport(
        userId: UUID,
        data: UserCompleteDataDto,
    ) {
        exportJobs[userId]?.let { job ->
            exportJobs[userId] =
                job.copy(
                    status = ExportJobStatus.COMPLETED,
                    completedAt = Instant.now(),
                    result = data,
                )
        }
    }

    private fun failExport(userId: UUID) {
        exportJobs[userId]?.let { job ->
            exportJobs[userId] =
                job.copy(
                    status = ExportJobStatus.FAILED,
                    completedAt = Instant.now(),
                )
        }
    }
}
