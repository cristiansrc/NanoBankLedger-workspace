package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.exception.InsufficientBalanceException
import com.nanobank.ledger.domain.model.Transaction
import com.nanobank.ledger.domain.model.TransactionType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Component
class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepositoryPort,
    private val walletRepository: WalletRepositoryPort
) {
    @Transactional
    fun execute(userId: UUID, walletId: UUID, request: CreateTransactionRequest): TransactionResponse {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { NoSuchElementException("Wallet not found: $walletId") }

        if (wallet.userId != userId) {
            throw NoSuchElementException("Wallet not found: $walletId")
        }

        val type = try {
            TransactionType.valueOf(request.type.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid transaction type: ${request.type}")
        }

        // Para gastos, verificar saldo suficiente
        if (type == TransactionType.EXPENSE && wallet.balance < request.amount) {
            throw InsufficientBalanceException(walletId, wallet.balance, request.amount)
        }

        // Actualizar saldo de la wallet
        val newBalance = when (type) {
            TransactionType.INCOME -> wallet.balance.add(request.amount)
            TransactionType.EXPENSE -> wallet.balance.subtract(request.amount)
        }
        walletRepository.save(wallet.copy(balance = newBalance))

        // Crear transacción
        val transaction = Transaction(
            walletId = walletId,
            categoryId = request.categoryId,
            type = type,
            amount = request.amount,
            description = request.description,
            date = request.date ?: LocalDate.now()
        )

        val saved = transactionRepository.save(transaction)
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
