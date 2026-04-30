package com.r8n.backend.opinions.stub

import com.r8n.backend.opinions.api.opinions.dto.OpinionDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionStatusEnumDto
import com.r8n.backend.opinions.api.opinions.dto.WeightedOpinionReferenceDto
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.alexanderReferent
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.bernardReferent
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.brokenTrust
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.canadaTariffs
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.cappuccino1A
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.cappuccino2
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.cappuccino3
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.donaldReferent
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.moreMoney
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.president
import com.r8n.backend.opinions.stub.OpinionSubjectTestDataFactory.venezuelaInvasion
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

object OpinionTestDataFactory {
    fun alexanderOnDonald(id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")) =
        OpinionDto(
            id,
            alexanderReferent.id,
            alexanderReferent.name,
            president.id,
            president.name,
            listOf(),
            listOf("born 1946"),
            mark = 1.07,
            componentMark = 1.00,
            components =
                listOf(
                    WeightedOpinionReferenceDto(UUID.randomUUID(), venezuelaInvasion.id, 0.8),
                    WeightedOpinionReferenceDto(UUID.randomUUID(), canadaTariffs.id, 1.0),
                ),
            OpinionStatusEnumDto.DRAFT,
            LocalDateTime
                .of(2024, 1, 1, 12, 0)
                .toInstant(ZoneOffset.UTC),
        )

    fun alexanderOnVenezuela(id: UUID = UUID.randomUUID()) =
        OpinionDto(
            id,
            alexanderReferent.id,
            alexanderReferent.name,
            venezuelaInvasion.id,
            venezuelaInvasion.name,
            listOf("you can't just kidnap people"),
            listOf("03.01.2026"),
            2.50,
            null,
            emptyList(),
            OpinionStatusEnumDto.PUBLISHED,
            Instant.now(),
        )

    fun alexanderOnTariffs(id: UUID = UUID.randomUUID()) =
        OpinionDto(
            id,
            alexanderReferent.id,
            alexanderReferent.name,
            canadaTariffs.id,
            canadaTariffs.name,
            emptyList(),
            emptyList(),
            null,
            0.00,
            listOf(
                WeightedOpinionReferenceDto(UUID.randomUUID(), moreMoney.id, 1.0),
                WeightedOpinionReferenceDto(UUID.randomUUID(), brokenTrust.id, 0.3),
            ),
            OpinionStatusEnumDto.PENDING_PREMODERATION,
            Instant.now(),
        )

    fun alexanderOnMoney(id: UUID = UUID.randomUUID()) =
        OpinionDto(
            id,
            alexanderReferent.id,
            alexanderReferent.name,
            moreMoney.id,
            moreMoney.name,
            emptyList(),
            emptyList(),
            5.0,
            null,
            emptyList(),
            OpinionStatusEnumDto.PUBLISHED,
            Instant.now(),
        )

    fun alexanderOnTrust(id: UUID = UUID.randomUUID()) =
        OpinionDto(
            id,
            alexanderReferent.id,
            alexanderReferent.name,
            brokenTrust.id,
            brokenTrust.name,
            emptyList(),
            emptyList(),
            2.0,
            null,
            emptyList(),
            OpinionStatusEnumDto.PUBLISHED,
            Instant.now(),
        )

    fun bernardOnCap1(id: UUID = UUID.randomUUID()) =
        OpinionDto(
            id,
            bernardReferent.id,
            bernardReferent.name,
            cappuccino1A.id,
            cappuccino1A.name,
            listOf("reminds of grandma's home coffee"),
            listOf("5.50€", "lactose-free milk"),
            4.23,
            null,
            emptyList(),
            OpinionStatusEnumDto.DRAFT,
            Instant.now(),
        )

    fun bernardOnCap2(id: UUID = UUID.randomUUID()) =
        OpinionDto(
            id,
            bernardReferent.id,
            bernardReferent.name,
            cappuccino2.id,
            cappuccino2.name,
            listOf("so thiccccccc"),
            listOf("6.60€"),
            4.69,
            null,
            emptyList(),
            OpinionStatusEnumDto.PUBLISHED,
            Instant.now(),
        )

    fun bernardOnCap3(id: UUID = UUID.randomUUID()): OpinionDto {
        val opinionDto =
            OpinionDto(
                id,
                bernardReferent.id,
                bernardReferent.name,
                cappuccino3.id,
                cappuccino3.name,
                listOf("my favorite"),
                listOf("5.75€"),
                4.90,
                null,
                emptyList(),
                OpinionStatusEnumDto.PUBLISHED,
                Instant.now(),
            )
        return opinionDto
    }

    fun alexanderOnCap1(id: UUID = UUID.randomUUID()) =
        OpinionDto(
            id,
            alexanderReferent.id,
            alexanderReferent.name,
            cappuccino1A.id,
            cappuccino1A.name,
            emptyList(),
            listOf("how the hell do you call this coffee? this is piss"),
            1.0,
            null,
            emptyList(),
            OpinionStatusEnumDto.REJECTED,
            Instant.now(),
        )

    fun donaldOnCap1(id: UUID = UUID.randomUUID()): OpinionDto {
        val opinionDto =
            OpinionDto(
                id,
                donaldReferent.id,
                donaldReferent.name,
                cappuccino1A.id,
                cappuccino1A.name,
                emptyList(),
                listOf(
                    "nobody knows more about cappuccino than I do",
                    "Make Caffeine Great Again",
                    "Thank you for your attention to the matter",
                ),
                5.00,
                null,
                emptyList(),
                OpinionStatusEnumDto.PUBLISHED,
                Instant.now(),
            )
        return opinionDto
    }

    fun donaldOnCap3(id: UUID = UUID.randomUUID()) =
        OpinionDto(
            id,
            donaldReferent.id,
            donaldReferent.name,
            cappuccino3.id,
            cappuccino3.name,
            emptyList(),
            listOf(
                "coffee is a democrat hoax",
                "so this coffee is why Joe was so sleepy",
            ),
            0.00,
            null,
            emptyList(),
            OpinionStatusEnumDto.PUBLISHED,
            Instant.now(),
        )

    fun getOpinion(id: UUID) =
        when (id.mostSignificantBits % 11) {
            0L -> alexanderOnDonald(id)
            1L -> alexanderOnVenezuela(id)
            2L -> alexanderOnTariffs(id)
            3L -> alexanderOnMoney(id)
            4L -> alexanderOnTrust(id)
            5L -> bernardOnCap1(id)
            6L -> bernardOnCap2(id)
            7L -> bernardOnCap3(id)
            8L -> donaldOnCap1(id)
            9L -> donaldOnCap3(id)
            else -> alexanderOnCap1(id)
        }

    fun postOpinion(
        opinionId: UUID? = null,
        subjectId: UUID? = null,
        subjective: List<String>,
        objective: List<String>,
        mark: Double?,
    ) = OpinionDto(
        opinionId ?: UUID.randomUUID(),
        UUID.randomUUID(),
        "YourName",
        subjectId ?: UUID.randomUUID(),
        "the famous $subjectId",
        subjective,
        objective,
        mark,
        null,
        emptyList(),
        OpinionStatusEnumDto.PENDING_PREMODERATION,
        Instant.now(),
    )
}
