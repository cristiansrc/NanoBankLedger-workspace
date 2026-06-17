package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.Wallet
import org.springframework.stereotype.Component
import java.util.NoSuchElementException
import java.util.UUID

@Component
class GetWalletUseCase(
    private val walletRepository: WalletRepositoryPort
) {
    fun execute(userId: UUID, walletId: UUID): WalletResponse {
        val wallet = walletRepository.findById(walletId)
            .orElseThrow { NoSuchElementException("Wallet not found: $walletId") }

        if (wallet.userId != userId) {
            throw NoSuchElementException("Wallet not found: $walletId")
        }

        return toResponse(wallet)
    }

    private fun toResponse(wallet: Wallet): WalletResponse = WalletResponse(
        id = wallet.id,
        userId = wallet.userId,
        name = wallet.name,
        type = wallet.type.name,
        balance = wallet.balance,
        createdAt = wallet.createdAt,
        updatedAt = wallet.updatedAt
    )
}
