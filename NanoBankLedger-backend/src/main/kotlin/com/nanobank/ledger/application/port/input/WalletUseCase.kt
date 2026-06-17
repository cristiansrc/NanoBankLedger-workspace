package com.nanobank.ledger.application.port.input

import com.nanobank.ledger.application.dto.*
import java.util.UUID

interface WalletUseCase {
    fun create(userId: UUID, request: CreateWalletRequest): WalletResponse
    fun findAllByUserId(userId: UUID): List<WalletResponse>
    fun findById(userId: UUID, walletId: UUID): WalletResponse
    fun update(userId: UUID, walletId: UUID, request: UpdateWalletRequest): WalletResponse
    fun delete(userId: UUID, walletId: UUID)
}
