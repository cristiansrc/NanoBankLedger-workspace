package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository

import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): Optional<RefreshTokenEntity>
    fun findByUser_IdAndRevokedAtIsNull(userId: UUID): List<RefreshTokenEntity>
    fun findByFamilyId(familyId: UUID): List<RefreshTokenEntity>
    fun deleteByUser_Id(userId: UUID)
}
