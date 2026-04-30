package com.r8n.backend.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class RateLimitingFilter : AbstractGatewayFilterFactory<RateLimitingFilter.Config>(Config::class.java) {
    private val counters = ConcurrentHashMap<String, RateLimitCounter>()

    data class Config(
        var limit: Int = 100,
        var durationSeconds: Long = 60,
    )

    data class RateLimitCounter(
        val count: AtomicInteger = AtomicInteger(0),
        val resetAt: Instant,
    )

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
            val key =
                exchange.request.headers.getFirst("X-API-KEY") ?: exchange.request.remoteAddress
                    ?.address
                    ?.hostAddress
                    ?: "unknown"
            val now = Instant.now()

            val counter =
                counters.compute(key) { _, current ->
                    if (current == null || now.isAfter(current.resetAt)) {
                        RateLimitCounter(AtomicInteger(1), now.plusSeconds(config.durationSeconds))
                    } else {
                        current.count.incrementAndGet()
                        current
                    }
                }!!

            if (counter.count.get() > config.limit) {
                exchange.response.statusCode = HttpStatus.TOO_MANY_REQUESTS
                exchange.response.headers.add("X-RateLimit-Limit", config.limit.toString())
                exchange.response.headers.add("X-RateLimit-Reset", counter.resetAt.epochSecond.toString())
                return@GatewayFilter exchange.response.setComplete()
            }

            chain.filter(exchange)
        }
    }
}
