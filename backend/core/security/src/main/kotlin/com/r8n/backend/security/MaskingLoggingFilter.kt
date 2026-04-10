package com.r8n.backend.security

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

class MaskingLoggingFilter : Filter {
    private val log = LoggerFactory.getLogger(MaskingLoggingFilter::class.java)

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        if (request is HttpServletRequest && response is HttpServletResponse) {
            val requestWrapper = ContentCachingRequestWrapper(request, 1024)
            val responseWrapper = ContentCachingResponseWrapper(response)

            val startTime = System.currentTimeMillis()
            try {
                chain.doFilter(requestWrapper, responseWrapper)
            } finally {
                val duration = System.currentTimeMillis() - startTime
                logRequest(requestWrapper, duration)
                responseWrapper.copyBodyToResponse()
            }
        } else {
            chain.doFilter(request, response)
        }
    }

    private fun logRequest(
        request: HttpServletRequest,
        duration: Long,
    ) {
        val method = request.method
        val uri = request.requestURI
        val params = request.queryString?.let { "?$it" } ?: ""

        // Header masking
        val headers =
            request.headerNames.asSequence().associateWith { name ->
                if (name.equals("Authorization", ignoreCase = true) ||
                    name.equals("Proxy-Authorization", ignoreCase = true)
                ) {
                    "Bearer ********"
                } else if (name.equals("Cookie", ignoreCase = true) ||
                    name.equals("Set-Cookie", ignoreCase = true)
                ) {
                    "********"
                } else {
                    request.getHeader(name)
                }
            }

        log.info("Request: {} {}{} | Duration: {}ms | Headers: {}", method, uri, params, duration, headers)
    }
}