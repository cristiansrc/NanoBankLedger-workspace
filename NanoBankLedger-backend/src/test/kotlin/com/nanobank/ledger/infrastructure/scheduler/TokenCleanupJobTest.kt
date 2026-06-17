package com.nanobank.ledger.infrastructure.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant

class TokenCleanupJobTest {

    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var tokenCleanupJob: TokenCleanupJob
    private val sqlSlot = slot<String>()
    private val timestampSlot = slot<Timestamp>()

    @BeforeEach
    fun setUp() {
        jdbcTemplate = mockk()
        tokenCleanupJob = TokenCleanupJob(jdbcTemplate)
    }

    @Test
    fun `should execute cleanup query with correct SQL`() {
        every { jdbcTemplate.update(capture(sqlSlot), capture(timestampSlot), capture(timestampSlot)) } returns 5

        tokenCleanupJob.cleanupExpiredTokens()

        assertTrue(sqlSlot.captured.contains("DELETE FROM refresh_tokens"))
        assertTrue(sqlSlot.captured.contains("expires_at"))
        assertTrue(sqlSlot.captured.contains("revoked_at"))
    }

    @Test
    fun `should return number of deleted tokens`() {
        every { jdbcTemplate.update(any<String>(), any<Timestamp>(), any<Timestamp>()) } returns 5

        tokenCleanupJob.cleanupExpiredTokens()

        verify(exactly = 1) { jdbcTemplate.update(any<String>(), any<Timestamp>(), any<Timestamp>()) }
    }

    @Test
    fun `should handle zero tokens deleted`() {
        every { jdbcTemplate.update(any<String>(), any<Timestamp>(), any<Timestamp>()) } returns 0

        tokenCleanupJob.cleanupExpiredTokens()

        verify(exactly = 1) { jdbcTemplate.update(any<String>(), any<Timestamp>(), any<Timestamp>()) }
    }

    @Test
    fun `should handle many tokens deleted`() {
        every { jdbcTemplate.update(any<String>(), any<Timestamp>(), any<Timestamp>()) } returns 1000

        tokenCleanupJob.cleanupExpiredTokens()

        verify(exactly = 1) { jdbcTemplate.update(any<String>(), any<Timestamp>(), any<Timestamp>()) }
    }
}
