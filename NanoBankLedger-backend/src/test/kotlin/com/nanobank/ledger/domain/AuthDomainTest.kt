package com.nanobank.ledger.domain

import com.nanobank.ledger.domain.model.RefreshToken
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class AuthDomainTest {

    @Test
    fun `should not be expired when expiresAt is in the future`() {
        val token = createToken(expiresAt = Instant.now().plus(1, ChronoUnit.DAYS))
        assertFalse(token.isExpired())
    }

    @Test
    fun `should be expired when expiresAt is in the past`() {
        val token = createToken(expiresAt = Instant.now().minus(1, ChronoUnit.DAYS))
        assertTrue(token.isExpired())
    }

    @Test
    fun `should not be revoked when revokedAt is null`() {
        val token = createToken(revokedAt = null)
        assertFalse(token.isRevoked())
    }

    @Test
    fun `should be revoked when revokedAt is not null`() {
        val token = createToken(revokedAt = Instant.now())
        assertTrue(token.isRevoked())
    }

    @Test
    fun `should not be used when usedAt is null`() {
        val token = createToken(usedAt = null)
        assertFalse(token.isUsed())
    }

    @Test
    fun `should be used when usedAt is not null`() {
        val token = createToken(usedAt = Instant.now())
        assertTrue(token.isUsed())
    }

    @Test
    fun `should create refresh token with random id`() {
        val token1 = createToken()
        val token2 = createToken()
        assertNotNull(token1.id)
        assertNotNull(token2.id)
        assertNotEquals(token1.id, token2.id)
    }

    @Test
    fun `should have correct family relationship`() {
        val familyId = UUID.randomUUID()
        val token1 = createToken(familyId = familyId)
        val token2 = createToken(familyId = familyId)
        assertEquals(token1.familyId, token2.familyId)
    }

    private fun createToken(
        expiresAt: Instant = Instant.now().plus(7, ChronoUnit.DAYS),
        revokedAt: Instant? = null,
        usedAt: Instant? = null,
        familyId: UUID = UUID.randomUUID()
    ): RefreshToken = RefreshToken(
        userId = UUID.randomUUID(),
        tokenHash = "testHash123",
        familyId = familyId,
        expiresAt = expiresAt,
        revokedAt = revokedAt,
        usedAt = usedAt
    )
}
