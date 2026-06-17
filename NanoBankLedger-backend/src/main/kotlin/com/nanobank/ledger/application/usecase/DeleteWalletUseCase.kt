package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.NoSuchElementException
import java.util.UUID

@Component
class DeleteWalletUseCase(
    private val walletRepository: WalletRepositoryPort,
    private val transactionRepository: TransactionRepositoryPort
) {
    @Transactional
    fun execute(userId: UUID, walletId: UUID) {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { NoSuchElementException("Wallet not found: $walletId") }

        if (wallet.userId != userId) {
            throw NoSuchElementException("Wallet not found: $walletId")
        }

        // RN-017: No permitir eliminar wallet con transacciones
        if (transactionRepository.countByWalletId(walletId) > 0) {
            throw IllegalStateException("Cannot delete wallet with existing transactions")
        }

        walletRepository.deleteById(walletId)
    }
}
