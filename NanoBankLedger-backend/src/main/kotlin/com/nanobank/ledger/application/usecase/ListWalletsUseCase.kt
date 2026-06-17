package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.Wallet
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ListWalletsUseCase(
    private val walletRepository: WalletRepositoryPort
) {
    fun execute(userId: UUID): List<WalletResponse> {
        return walletRepository.findByUserId(userId)
            .map { toResponse(it) }
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
