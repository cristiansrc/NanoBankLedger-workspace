package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository

import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.WalletEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface WalletJpaRepository : JpaRepository<WalletEntity, UUID> {
    fun findByUser_Id(userId: UUID): List<WalletEntity>
    fun existsByIdAndUser_Id(id: UUID, userId: UUID): Boolean
}
