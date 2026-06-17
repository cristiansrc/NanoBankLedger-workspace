package com.nanobank.ledger.application.port.output

import com.nanobank.ledger.domain.model.RefreshToken
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepositoryPort {
    fun save(token: RefreshToken): RefreshToken
    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>
    fun findByUserIdAndNotRevoked(userId: UUID): List<RefreshToken>
    fun findByFamilyId(familyId: UUID): List<RefreshToken>
    fun deleteByUserId(userId: UUID)
}
