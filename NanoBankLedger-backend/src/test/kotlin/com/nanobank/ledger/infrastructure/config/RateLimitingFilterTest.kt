package com.nanobank.ledger.infrastructure.config

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimitingFilterTest {

    private lateinit var filter: RateLimitingFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var chain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = RateLimitingFilter()
        request = mockk()
        response = mockk(relaxed = true)
        chain = mockk()
    }

    @Test
    fun `should allow login request within rate limit`() {
        every { request.method } returns "POST"
        every { request.requestURI } returns "/api/v1/auth/login"
        every { request.getHeader("X-Forwarded-For") } returns null
        every { request.remoteAddr } returns "127.0.0.1"
        every { chain.doFilter(any(), any()) } just runs

        // First 5 requests should be allowed
        for (i in 1..5) {
            filter.doFilter(request, response, chain)
        }

        verify(exactly = 5) { chain.doFilter(any(), any()) }
    }

    @Test
    fun `should block login request exceeding rate limit`() {
        every { request.method } returns "POST"
        every { request.requestURI } returns "/api/v1/auth/login"
        every { request.getHeader("X-Forwarded-For") } returns null
        every { request.remoteAddr } returns "192.168.1.1"
        every { chain.doFilter(any(), any()) } just runs

        // First 5 requests allowed
        for (i in 1..5) {
            filter.doFilter(request, response, chain)
        }

        // 6th request should be blocked (429)
        filter.doFilter(request, response, chain)

        verify(exactly = 1) { response.status = 429 }
        verify(exactly = 1) { response.contentType = "application/json" }
    }

    @Test
    fun `should not rate-limit non-login endpoints`() {
        every { request.method } returns "GET"
        every { request.requestURI } returns "/api/v1/wallets"
        every { request.remoteAddr } returns "127.0.0.1"
        every { chain.doFilter(any(), any()) } just runs

        filter.doFilter(request, response, chain)

        verify(exactly = 1) { chain.doFilter(any(), any()) }
        verify(exactly = 0) { response.status = 429 }
    }

    @Test
    fun `should reset rate limit for different IPs`() {
        every { request.method } returns "POST"
        every { request.requestURI } returns "/api/v1/auth/login"
        every { chain.doFilter(any(), any()) } just runs

        // First IP gets 5 requests - all allowed
        every { request.remoteAddr } returns "10.0.0.1"
        every { request.getHeader("X-Forwarded-For") } returns null
        for (i in 1..5) {
            filter.doFilter(request, response, chain)
        }

        // Second IP gets 1 request - should be allowed (different bucket)
        every { request.remoteAddr } returns "10.0.0.2"
        filter.doFilter(request, response, chain)

        verify(exactly = 6) { chain.doFilter(any(), any()) }
        verify(exactly = 0) { response.status = 429 }
    }

    @Test
    fun `should use X-Forwarded-For header when present`() {
        every { request.method } returns "POST"
        every { request.requestURI } returns "/api/v1/auth/login"
        every { request.getHeader("X-Forwarded-For") } returns "203.0.113.5, 10.0.0.1"
        every { request.remoteAddr } returns "10.0.0.1" // fallback, should not be used
        every { chain.doFilter(any(), any()) } just runs

        // First 5 should be allowed
        for (i in 1..5) {
            filter.doFilter(request, response, chain)
        }

        // 6th should be blocked
        filter.doFilter(request, response, chain)

        verify(exactly = 1) { response.status = 429 }
    }
}
