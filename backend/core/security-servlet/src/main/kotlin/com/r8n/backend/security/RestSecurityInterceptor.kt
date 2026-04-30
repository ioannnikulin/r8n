package com.r8n.backend.security

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class RestSecurityInterceptor(
    private val serviceTokenService: ServiceTokenService? = null,
) : ClientHttpRequestInterceptor {
    private val logger = LoggerFactory.getLogger(RestSecurityInterceptor::class.java)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val auth = SecurityContextHolder.getContext().authentication

        if (auth is JwtAuthenticationToken) {
            request.headers.add("Authorization", "Bearer ${auth.token.tokenValue}")
        } else {
            serviceTokenService?.generateServiceToken()?.let { token ->
                request.headers.add("Authorization", "Bearer $token")
            } ?: logger.warn("No authenticated user found in security context")
        }
        return execution.execute(request, body)
    }
}
