package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.Transaction
import org.springframework.stereotype.Component
import java.util.*

@Component
class ListTransactionsUseCase(
    private val transactionRepository: TransactionRepositoryPort,
    private val walletRepository: WalletRepositoryPort
) {
    fun execute(userId: UUID, walletId: UUID, filters: TransactionFilters): List<TransactionResponse> {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { NoSuchElementException("Wallet not found: $walletId") }

        if (wallet.userId != userId) {
            throw NoSuchElementException("Wallet not found: $walletId")
        }

        val transactions = transactionRepository.findByFilters(
            walletId = walletId,
            categoryId = filters.categoryId,
            dateFrom = filters.dateFrom,
            dateTo = filters.dateTo,
            type = filters.type
        )

        return transactions.map { toResponse(it) }
    }

    private fun toResponse(t: Transaction): TransactionResponse = TransactionResponse(
        id = t.id,
        walletId = t.walletId,
        categoryId = t.categoryId,
        type = t.type.name,
        amount = t.amount,
        description = t.description,
        date = t.date,
        createdAt = t.createdAt,
        updatedAt = t.updatedAt
    )
}
