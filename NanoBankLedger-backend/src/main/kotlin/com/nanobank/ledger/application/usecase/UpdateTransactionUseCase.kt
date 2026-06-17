package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.exception.InsufficientBalanceException
import com.nanobank.ledger.domain.model.Transaction
import com.nanobank.ledger.domain.model.TransactionType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepositoryPort,
    private val walletRepository: WalletRepositoryPort
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID, request: UpdateTransactionRequest): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("Transaction not found: $transactionId") }

        val wallet = walletRepository.findById(transaction.walletId)
            .orElseThrow { NoSuchElementException("Transaction not found: $transactionId") }

        if (wallet.userId != userId) {
            throw NoSuchElementException("Transaction not found: $transactionId") // 404 no 403 por seguridad
        }

        // Revertir el efecto de la transacción original en el saldo
        val revertedBalance = when (transaction.type) {
            TransactionType.INCOME -> wallet.balance.subtract(transaction.amount)
            TransactionType.EXPENSE -> wallet.balance.add(transaction.amount)
        }

        // Aplicar los cambios solicitados
        val newType = request.type?.let {
            try { TransactionType.valueOf(it.uppercase()) } catch (e: IllegalArgumentException) { transaction.type }
        } ?: transaction.type

        val newAmount = request.amount ?: transaction.amount

        // Verificar saldo si es gasto
        if (newType == TransactionType.EXPENSE && revertedBalance < newAmount) {
            throw InsufficientBalanceException(wallet.id, revertedBalance, newAmount)
        }

        // Actualizar saldo con los nuevos valores
        val finalBalance = when (newType) {
            TransactionType.INCOME -> revertedBalance.add(newAmount)
            TransactionType.EXPENSE -> revertedBalance.subtract(newAmount)
        }
        walletRepository.save(wallet.copy(balance = finalBalance))

        // Actualizar transacción
        val updated = transaction.copy(
            type = newType,
            amount = newAmount,
            categoryId = request.categoryId ?: transaction.categoryId,
            description = request.description ?: transaction.description,
            date = request.date ?: transaction.date
        )

        val saved = transactionRepository.save(updated)
        return toResponse(saved)
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
