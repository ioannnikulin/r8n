package com.r8n.backend.core.web

import com.r8n.backend.core.web.error.GlobalExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(GlobalExceptionHandler::class)
class WebCoreConfiguration
