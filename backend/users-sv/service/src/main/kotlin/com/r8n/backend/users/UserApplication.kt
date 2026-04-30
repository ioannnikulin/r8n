package com.r8n.backend.users

import com.r8n.backend.core.web.WebCoreConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@Import(
    WebCoreConfiguration::class,
)
class UserApplication

fun main(args: Array<String>) {
    runApplication<UserApplication>(*args)
}
