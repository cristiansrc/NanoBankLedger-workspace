package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.TransactionType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepositoryPort,
    private val walletRepository: WalletRepositoryPort
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID) {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("Transaction not found: $transactionId") }

        val wallet = walletRepository.findById(transaction.walletId)
            .orElseThrow { NoSuchElementException("Transaction not found: $transactionId") }

        if (wallet.userId != userId) {
            throw NoSuchElementException("Transaction not found: $transactionId")
        }

        // Revertir el efecto de la transacción en el saldo
        val revertedBalance = when (transaction.type) {
            TransactionType.INCOME -> wallet.balance.subtract(transaction.amount)
            TransactionType.EXPENSE -> wallet.balance.add(transaction.amount)
        }
        walletRepository.save(wallet.copy(balance = revertedBalance))

        transactionRepository.deleteById(transactionId)
    }
}
