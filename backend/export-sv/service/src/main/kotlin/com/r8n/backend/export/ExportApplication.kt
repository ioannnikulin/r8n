package com.r8n.backend.export

import com.r8n.backend.mock.integration.configuration.MockRestClientConfiguration
import com.r8n.backend.users.integration.UsersRestClientConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
@Import(
    value = [
        UsersRestClientConfiguration::class,
        MockRestClientConfiguration::class,
    ],
)
class ExportApplication

fun main(args: Array<String>) {
    runApplication<ExportApplication>(*args)
}