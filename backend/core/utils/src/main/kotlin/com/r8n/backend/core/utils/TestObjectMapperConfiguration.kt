package com.r8n.backend.core.utils

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@TestConfiguration
class TestObjectMapperConfiguration {
    @Bean
    fun objectMapper(): JsonMapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .build()
}
