package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.exception.SameWalletTransferException
import com.nanobank.ledger.domain.model.Transaction
import com.nanobank.ledger.domain.model.TransactionType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
class MoveTransactionUseCase(
    private val transactionRepository: TransactionRepositoryPort,
    private val walletRepository: WalletRepositoryPort
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID, request: MoveTransactionRequest): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            .orElseThrow { NoSuchElementException("Transaction not found: $transactionId") }

        val sourceWallet = walletRepository.findById(transaction.walletId)
            .orElseThrow { NoSuchElementException("Source wallet not found") }

        if (sourceWallet.userId != userId) {
            throw NoSuchElementException("Transaction not found: $transactionId")
        }

        val targetWallet = walletRepository.findById(request.targetWalletId)
            .orElseThrow { NoSuchElementException("Target wallet not found") }

        if (targetWallet.userId != userId) {
            throw NoSuchElementException("Target wallet not found")
        }

        // No permitir mover a la misma wallet
        if (transaction.walletId == request.targetWalletId) {
            throw SameWalletTransferException()
        }

        // 1. Revertir efecto en wallet origen
        val sourceNewBalance = when (transaction.type) {
            TransactionType.INCOME -> sourceWallet.balance.subtract(transaction.amount)
            TransactionType.EXPENSE -> sourceWallet.balance.add(transaction.amount)
        }
        walletRepository.save(sourceWallet.copy(balance = sourceNewBalance))

        // 2. Aplicar efecto en wallet destino
        val targetNewBalance = when (transaction.type) {
            TransactionType.INCOME -> targetWallet.balance.add(transaction.amount)
            TransactionType.EXPENSE -> targetWallet.balance.subtract(transaction.amount)
        }

        // Verificar saldo suficiente si es gasto
        if (transaction.type == TransactionType.EXPENSE && targetWallet.balance < transaction.amount) {
            // Revertir cambio en wallet origen si falla
            walletRepository.save(sourceWallet.copy(balance = sourceWallet.balance))
            throw com.nanobank.ledger.domain.exception.InsufficientBalanceException(
                targetWallet.id, targetWallet.balance, transaction.amount
            )
        }

        walletRepository.save(targetWallet.copy(balance = targetNewBalance))

        // 3. Actualizar walletId de la transacción
        val updated = transaction.copy(walletId = request.targetWalletId)
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
