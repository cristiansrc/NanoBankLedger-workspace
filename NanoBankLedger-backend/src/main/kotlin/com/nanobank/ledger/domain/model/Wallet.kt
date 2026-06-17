package com.nanobank.ledger.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class WalletType {
    SAVINGS, CHECKING, INVESTMENT, CASH
}

data class Wallet(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val name: String,
    val type: WalletType = WalletType.CHECKING,
    val balance: BigDecimal = BigDecimal.ZERO,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
