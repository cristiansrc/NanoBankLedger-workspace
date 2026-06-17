package com.nanobank.ledger.application.port.output

import com.nanobank.ledger.domain.model.Transaction
import com.nanobank.ledger.domain.model.TransactionType
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

interface TransactionRepositoryPort {
    fun save(transaction: Transaction): Transaction
    fun findById(id: UUID): Optional<Transaction>
    fun findByWalletId(walletId: UUID): List<Transaction>
    fun findByFilters(
        walletId: UUID,
        categoryId: UUID? = null,
        dateFrom: LocalDate? = null,
        dateTo: LocalDate? = null,
        type: TransactionType? = null
    ): List<Transaction>
    fun countByWalletId(walletId: UUID): Long
    fun deleteById(id: UUID)
}
