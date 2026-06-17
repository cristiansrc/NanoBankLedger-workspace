package com.nanobank.ledger.infrastructure.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
@Order(1)
class RateLimitingFilter : Filter {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        // Solo aplicar rate limiting a POST /api/v1/auth/login
        if (httpRequest.method == "POST" && httpRequest.requestURI.contains("/api/v1/auth/login")) {
            val clientIp = httpRequest.getHeader("X-Forwarded-For")
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
                ?: httpRequest.remoteAddr

            val bucket = buckets.computeIfAbsent(clientIp) { createBucket() }

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response)
            } else {
                httpResponse.status = 429
                httpResponse.contentType = "application/json"
                httpResponse.writer.write(
                    """{
                        "timestamp": "${Instant.now()}",
                        "status": 429,
                        "error": "Too Many Requests",
                        "code": "RATE_LIMIT_EXCEEDED",
                        "message": "Demasiados intentos de inicio de sesión. Intenta de nuevo en 1 minuto.",
                        "path": "${httpRequest.requestURI}",
                        "trace_id": "",
                        "details": []
                    }"""
                )
            }
        } else {
            chain.doFilter(request, response)
        }
    }

    private fun createBucket(): Bucket {
        val limit = Bandwidth.builder()
            .capacity(5)
            .refillIntervally(5, Duration.ofMinutes(1))
            .build()
        return Bucket.builder().addLimit(limit).build()
    }
}
