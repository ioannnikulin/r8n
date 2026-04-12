package com.r8n.backend.export.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService
import org.springframework.security.task.DelegatingSecurityContextTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class AsyncSecurityConfig {

    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 10
        executor.setThreadNamePrefix("export-async-")
        executor.initialize()
        return DelegatingSecurityContextTaskExecutor(executor)
    }
}