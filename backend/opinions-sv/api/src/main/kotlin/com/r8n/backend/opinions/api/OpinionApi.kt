package com.r8n.backend.opinions.api

import com.r8n.backend.opinions.api.dto.OpinionDto
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.UUID

interface OpinionApi {
    @GetMapping("/opinions/{id}")
    fun getOpinionById(
        @PathVariable id: UUID,
    ): OpinionDto

    @GetMapping("/opinions/for/{subjectId}")
    fun getOpinionFor(
        @PathVariable subjectId: UUID,
    ): OpinionDto

    @PostMapping("/opinions")
    fun createOpinion(
        @RequestParam(required = true)
        subjectId: UUID,
        @RequestParam(required = false)
        subjective: List<String>,
        @RequestParam(required = false)
        objective: List<String>,
        @RequestParam(required = false)
        mark: Double?,
    ): OpinionDto

    @PatchMapping("/opinions/{opinionId}")
    fun updateOpinion(
        @PathVariable opinionId: UUID,
        @RequestParam(required = false)
        subjective: List<String>,
        @RequestParam(required = false)
        objective: List<String>,
        @RequestParam(required = false)
        mark: Double?,
    ): OpinionDto

    @DeleteMapping("/opinions/{opinionId}")
    fun deleteOpinion(
        @PathVariable opinionId: UUID,
    )

    @PostMapping("/opinions/link")
    fun linkComponent(
        parentOpinionId: UUID,
        childOpinionId: UUID,
        weight: Double,
    ): OpinionDto

    @DeleteMapping("/opinions/unlink/{linkId}")
    fun unlinkComponent(
        @PathVariable linkId: UUID,
    ): OpinionDto

    @PatchMapping("/opinions/adjustWeight/{linkId}")
    fun adjustComponentWeight(
        @PathVariable linkId: UUID,
        weight: Double,
    ): OpinionDto
}