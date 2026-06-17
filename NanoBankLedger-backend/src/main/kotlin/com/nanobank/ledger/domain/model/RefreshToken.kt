package com.nanobank.ledger.domain.model

import java.time.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val tokenHash: String,
    val familyId: UUID,
    val expiresAt: Instant,
    val issuedAt: Instant = Instant.now(),
    val revokedAt: Instant? = null,
    val usedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    fun isRevoked(): Boolean = revokedAt != null
    fun isUsed(): Boolean = usedAt != null
}
