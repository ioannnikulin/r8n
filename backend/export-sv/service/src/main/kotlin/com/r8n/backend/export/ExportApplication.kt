package com.r8n.backend.export

import com.r8n.backend.core.web.WebCoreConfiguration
import com.r8n.backend.messaging.integration.MessagingRestClientConfiguration
import com.r8n.backend.opinions.integration.configuration.AccessRestClientConfiguration
import com.r8n.backend.opinions.integration.configuration.OpinionListsRestClientConfiguration
import com.r8n.backend.opinions.integration.configuration.OpinionsRestClientConfiguration
import com.r8n.backend.users.integration.UsersRestClientConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
@Import(
    value = [
        MessagingRestClientConfiguration::class,
        UsersRestClientConfiguration::class,
        AccessRestClientConfiguration::class,
        OpinionsRestClientConfiguration::class,
        OpinionListsRestClientConfiguration::class,
        WebCoreConfiguration::class,
    ],
)
class ExportApplication

fun main(args: Array<String>) {
    runApplication<ExportApplication>(*args)
}
