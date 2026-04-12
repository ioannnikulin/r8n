package com.r8n.backend.export.facade

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import com.r8n.backend.export.api.dto.UserCompleteDataDto
import com.r8n.backend.mock.api.IncomingAccessRequestApi
import com.r8n.backend.mock.api.MessagingApi
import com.r8n.backend.mock.api.OutgoingAccessRequestApi
import com.r8n.backend.mock.integration.api.OpinionListInternalApi
import com.r8n.backend.users.api.dto.ConsentDto
import com.r8n.backend.users.api.dto.PersonalIdentifiableInformationSectionDto
import com.r8n.backend.users.integration.api.UsersInternalApi
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class DataExportFacade(
    private val usersInternalApi: UsersInternalApi,
    private val opinionClient: OpinionListInternalApi,
    private val incomingAccessRequestClient: IncomingAccessRequestApi,
    private val outgoingAccessRequestClient: OutgoingAccessRequestApi,
    private val messageClient: MessagingApi,
) {
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
    }

    fun startExport(userId: UUID) {
        exportJobs[userId] = ExportJob(userId, ExportJobStatus.PENDING, Instant.now())

        // In a real implementation, this would trigger an async job
        // For now, we'll mark it as completed immediately for simplicity
        Thread {
            try {
                updateExportStatus(userId, ExportJobStatus.IN_PROGRESS)
                val data = getUserCompleteDataDto(userId)
                completeExport(userId, data)
            } catch (e: Exception) {
                failExport(userId)
            }
        }.start()
    }

    fun getExportStatus(userId: UUID): com.r8n.backend.export.api.dto.ExportStateDto {
        val job =
            exportJobs[userId]
                ?: throw NoSuchElementException("No export job found for user $userId")

        return com.r8n.backend.export.api.dto.ExportStateDto(
            userId = job.userId,
            status = job.status.toApiStatus(),
            createdAt = job.createdAt,
            completedAt = job.completedAt,
            estimatedCompletionTime = job.completedAt ?: job.createdAt.plusSeconds(300), // 5 min estimate
        )
    }

    fun getExportData(userId: UUID): UserCompleteDataDto {
        val job =
            exportJobs[userId]
                ?: throw NoSuchElementException("No export job found for user $userId")

        if (job.status != ExportJobStatus.COMPLETED) {
            throw IllegalStateException("Export not completed yet. Status: ${job.status}")
        }

        return job.result
            ?: throw NoSuchElementException("Completed export has no data for user $userId")
    }

    fun getUserCompleteDataDto(id: UUID): UserCompleteDataDto {
        val user = usersInternalApi.getUser(id)
        val sessions = usersInternalApi.getSessionsForUser(id, PageRequestDto(0, -1))

        return UserCompleteDataDto(
            id = user.id,
            status = user.status,
            statusTimestamp = user.statusTimestamp,
            consents =
                PageResponseDto(
                    items = user.consents.map { it.toExportDto() },
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
            opinions = opinionClient.getMineFull(PageRequestDto(0, -1)),
            outgoingRequests = outgoingAccessRequestClient.get(null, null, null, PageRequestDto(0, -1)),
            incomingRequests = incomingAccessRequestClient.get(null, null, null, PageRequestDto(0, -1)),
            messages = messageClient.getSupportThreads(),
        )
    }

    private fun ExportJobStatus.toApiStatus(): com.r8n.backend.export.api.dto.ExportStatus =
        when (this) {
            ExportJobStatus.PENDING -> com.r8n.backend.export.api.dto.ExportStatus.PENDING
            ExportJobStatus.IN_PROGRESS -> com.r8n.backend.export.api.dto.ExportStatus.IN_PROGRESS
            ExportJobStatus.COMPLETED -> com.r8n.backend.export.api.dto.ExportStatus.COMPLETED
            ExportJobStatus.FAILED -> com.r8n.backend.export.api.dto.ExportStatus.FAILED
        }

    private fun com.r8n.backend.users.api.dto.ConsentDto.toExportDto(): ConsentDto =
        ConsentDto(
            type = this.type,
            accepted = this.accepted,
            session = this.session,
        )

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