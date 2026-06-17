package com.nanobank.ledger.application.port.output

import com.nanobank.ledger.domain.model.Wallet
import java.util.Optional
import java.util.UUID

interface WalletRepositoryPort {
    fun save(wallet: Wallet): Wallet
    fun findById(id: UUID): Optional<Wallet>
    fun findByUserId(userId: UUID): List<Wallet>
    fun existsByIdAndUserId(id: UUID, userId: UUID): Boolean
    fun deleteById(id: UUID)
}
