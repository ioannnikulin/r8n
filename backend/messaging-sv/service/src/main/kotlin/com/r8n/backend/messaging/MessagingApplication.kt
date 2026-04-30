package com.r8n.backend.messaging

import com.r8n.backend.users.integration.UsersRestClientConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(
    UsersRestClientConfiguration::class,
)
class MessagingApplication

fun main(args: Array<String>) {
    runApplication<MessagingApplication>(*args)
}
