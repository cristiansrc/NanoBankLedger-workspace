package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.Wallet
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CreateWalletUseCase(
    private val walletRepository: WalletRepositoryPort
) {
    @Transactional
    fun execute(userId: UUID, request: CreateWalletRequest): WalletResponse {
        val wallet = Wallet(
            userId = userId,
            name = request.name,
            type = try {
                com.nanobank.ledger.domain.model.WalletType.valueOf(request.type.uppercase())
            } catch (e: IllegalArgumentException) {
                com.nanobank.ledger.domain.model.WalletType.CHECKING
            },
            balance = request.initialBalance ?: java.math.BigDecimal.ZERO
        )

        val saved = walletRepository.save(wallet)
        return toResponse(saved)
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
