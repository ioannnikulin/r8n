package com.r8n.backend.opinions.opinions.service

import com.r8n.backend.opinions.opinions.database.OpinionRepository
import com.r8n.backend.opinions.opinions.database.WeightedOpinionReferenceRepository
import com.r8n.backend.opinions.opinions.domain.ComponentSection
import com.r8n.backend.opinions.opinions.domain.WeightedOpinionReference
import com.r8n.backend.opinions.opinions.persistence.WeightedOpinionReferencePersistence
import org.springframework.stereotype.Service
import java.util.UUID

// does not check permissions, so should be exposed only to OpinionService or another service that checks them
@Service
class ComponentService(
    private val weightedOpinionReferenceRepository: WeightedOpinionReferenceRepository,
    private val opinionRepository: OpinionRepository,
) {
    fun hasLink(
        parentOpinionId: UUID,
        childOpinionId: UUID,
    ): Boolean =
        weightedOpinionReferenceRepository.existsByParentOpinionAndChildOpinion(
            parentOpinionId,
            childOpinionId,
        )

    fun linkComponent(
        parentOpinionId: UUID,
        childOpinionId: UUID,
        weight: Double,
    ) {
        weightedOpinionReferenceRepository.save(
            WeightedOpinionReferencePersistence(
                parentOpinion = parentOpinionId,
                childOpinion = childOpinionId,
                weight = weight,
            ),
        )
    }

    fun unlinkComponent(linkId: UUID) {
        weightedOpinionReferenceRepository.deleteById(linkId)
    }

    fun getParentOpinionId(linkId: UUID): UUID? =
        weightedOpinionReferenceRepository
            .findById(linkId)
            .map { it.parentOpinion }
            .orElse(null)

    fun adjustComponentWeight(
        linkId: UUID,
        weight: Double,
    ): Boolean {
        val link = weightedOpinionReferenceRepository.findById(linkId).orElse(null) ?: return false
        link.weight = weight
        weightedOpinionReferenceRepository.save(link)
        return true
    }

    fun getComponentSection(parentOpinionId: UUID): ComponentSection {
        val components =
            weightedOpinionReferenceRepository
                .findAllByParentOpinionOrderByIdAsc(parentOpinionId)
                .map { WeightedOpinionReference(it.id!!, it.childOpinion, it.weight) }
        val childMarksById =
            opinionRepository
                .findAllById(components.map { it.opinion }.distinct())
                .associate { it.id!! to it.mark }
        val weightedMarks =
            components.mapNotNull { component ->
                childMarksById[component.opinion]?.let { mark -> component.weight * mark }
            }
        val componentMark = weightedMarks.takeIf { it.isNotEmpty() && it.size == components.size }?.sum()
        return ComponentSection(
            componentMark,
            components,
        )
    }
}
