package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.Transaction
import org.springframework.stereotype.Component
import java.util.*

@Component
class GetTransactionUseCase(
    private val transactionRepository: TransactionRepositoryPort,
    private val walletRepository: WalletRepositoryPort
) {
    fun execute(userId: UUID, transactionId: UUID): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("Transaction not found: $transactionId") }

        // Verificar ownership a través de la wallet
        val wallet = walletRepository.findById(transaction.walletId)
            .orElseThrow { NoSuchElementException("Transaction not found: $transactionId") }

        if (wallet.userId != userId) {
            throw NoSuchElementException("Transaction not found: $transactionId")
        }

        return toResponse(transaction)
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
