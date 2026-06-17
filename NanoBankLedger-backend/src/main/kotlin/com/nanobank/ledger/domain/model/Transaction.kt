package com.nanobank.ledger.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class TransactionType {
    INCOME, EXPENSE
}

data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val walletId: UUID,
    val categoryId: UUID? = null,
    val type: TransactionType,
    val amount: BigDecimal,
    val description: String? = null,
    val date: LocalDate = LocalDate.now(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
