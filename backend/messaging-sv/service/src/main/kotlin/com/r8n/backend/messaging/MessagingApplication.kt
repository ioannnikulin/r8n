package com.r8n.backend.messaging

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MessagingApplication

fun main(args: Array<String>) {
    runApplication<MessagingApplication>(*args)
}