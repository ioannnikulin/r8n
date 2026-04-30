package com.r8n.backend.opinions.stub

import com.r8n.backend.opinions.api.lists.dto.OpinionListDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListPrivacyEnumDto
import com.r8n.backend.opinions.api.lists.dto.OpinionListSummaryDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionRowDto
import com.r8n.backend.opinions.api.opinions.dto.OpinionSummaryDto
import java.util.UUID

object OpinionListTestDataFactory {
    val SEEDED_LIST_ID: UUID = UUID.fromString("70000000-0000-0000-0000-000000000001")

    fun getListSummary(id: UUID = SEEDED_LIST_ID) =
        OpinionListSummaryDto(
            id,
            "${OpinionSubjectTestDataFactory.bernard.name}'s cappuccino rating",
            OpinionSubjectTestDataFactory.bernard.id,
            OpinionSubjectTestDataFactory.bernard.name,
            3,
            1,
            OpinionListPrivacyEnumDto.SEARCHABLE,
        )

    fun getOpinionSummary1() =
        OpinionSummaryDto(
            subject = OpinionTestDataFactory.bernardOnCap1().subject,
            subjectName = OpinionTestDataFactory.bernardOnCap1().subjectName,
            ownMark = OpinionTestDataFactory.bernardOnCap1().mark,
            componentMark = OpinionTestDataFactory.bernardOnCap1().componentMark,
            opinions =
                listOf(
                    OpinionRowDto(
                        opinionId = OpinionTestDataFactory.bernardOnCap1().id,
                        owner = OpinionTestDataFactory.bernardOnCap1().owner,
                        ownerName = OpinionTestDataFactory.bernardOnCap1().ownerName,
                        subjective = OpinionTestDataFactory.bernardOnCap1().subjective,
                        objective = OpinionTestDataFactory.bernardOnCap1().objective,
                        mark = OpinionTestDataFactory.bernardOnCap1().mark,
                        status = OpinionTestDataFactory.bernardOnCap1().status,
                        timestamp = OpinionTestDataFactory.bernardOnCap1().timestamp,
                        weight = 1.0,
                    ),
                    OpinionRowDto(
                        opinionId = OpinionTestDataFactory.donaldOnCap1().id,
                        owner = OpinionTestDataFactory.donaldOnCap1().owner,
                        ownerName = OpinionTestDataFactory.donaldOnCap1().ownerName,
                        subjective = OpinionTestDataFactory.donaldOnCap1().subjective,
                        objective = OpinionTestDataFactory.donaldOnCap1().objective,
                        mark = OpinionTestDataFactory.donaldOnCap1().mark,
                        status = OpinionTestDataFactory.donaldOnCap1().status,
                        timestamp = OpinionTestDataFactory.donaldOnCap1().timestamp,
                        weight = 0.3,
                    ),
                ),
        )

    fun getOpinionSummary3() =
        OpinionSummaryDto(
            subject = OpinionTestDataFactory.bernardOnCap3().subject,
            subjectName = OpinionTestDataFactory.bernardOnCap3().subjectName,
            ownMark = OpinionTestDataFactory.bernardOnCap3().mark,
            componentMark = OpinionTestDataFactory.bernardOnCap3().componentMark,
            opinions =
                listOf(
                    OpinionRowDto(
                        opinionId = OpinionTestDataFactory.bernardOnCap3().id,
                        owner = OpinionTestDataFactory.bernardOnCap3().owner,
                        ownerName = OpinionTestDataFactory.bernardOnCap3().ownerName,
                        subjective = OpinionTestDataFactory.bernardOnCap3().subjective,
                        objective = OpinionTestDataFactory.bernardOnCap3().objective,
                        mark = OpinionTestDataFactory.bernardOnCap3().mark,
                        status = OpinionTestDataFactory.bernardOnCap3().status,
                        timestamp = OpinionTestDataFactory.bernardOnCap3().timestamp,
                        weight = 1.0,
                    ),
                    OpinionRowDto(
                        opinionId = OpinionTestDataFactory.donaldOnCap3().id,
                        owner = OpinionTestDataFactory.donaldOnCap3().owner,
                        ownerName = OpinionTestDataFactory.donaldOnCap3().ownerName,
                        subjective = OpinionTestDataFactory.donaldOnCap3().subjective,
                        objective = OpinionTestDataFactory.donaldOnCap3().objective,
                        mark = OpinionTestDataFactory.donaldOnCap3().mark,
                        status = OpinionTestDataFactory.donaldOnCap3().status,
                        timestamp = OpinionTestDataFactory.donaldOnCap3().timestamp,
                        weight = 0.1,
                    ),
                ),
        )

    fun getList(id: UUID = UUID.randomUUID()): OpinionListDto {
        val opinionListDto =
            OpinionListDto(
                id,
                "${OpinionSubjectTestDataFactory.bernard.name}'s cappuccino rating",
                OpinionSubjectTestDataFactory.bernard.id,
                OpinionSubjectTestDataFactory.bernard.name,
                listOf(getOpinionSummary1(), getOpinionSummary3()),
                OpinionListPrivacyEnumDto.SEARCHABLE,
            )
        return opinionListDto
    }
}
