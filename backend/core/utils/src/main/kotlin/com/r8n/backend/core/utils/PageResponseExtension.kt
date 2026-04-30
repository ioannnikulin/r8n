package com.r8n.backend.core.utils

import com.r8n.backend.core.api.PageRequestDto
import com.r8n.backend.core.api.PageResponseDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

fun <T : Any> Page<T>.toResponse(): PageResponseDto<T> =
    PageResponseDto(
        items = content,
        total = totalElements,
        page = number,
        size = size,
    )

fun PageRequestDto.toPageable(): Pageable =
    PageRequest.of(
        page,
        size,
        if (sort.isEmpty()) {
            Sort.unsorted()
        } else {
            Sort.by(sort.map { Sort.Order(Sort.Direction.valueOf(it.direction.name), it.property) })
        },
    )
