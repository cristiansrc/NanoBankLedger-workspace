package com.nanobank.ledger.application.port.input

import com.nanobank.ledger.application.dto.*
import java.util.UUID

interface TransactionUseCase {
    fun create(userId: UUID, walletId: UUID, request: CreateTransactionRequest): TransactionResponse
    fun findByWalletId(userId: UUID, walletId: UUID, filters: TransactionFilters): List<TransactionResponse>
    fun findById(userId: UUID, transactionId: UUID): TransactionResponse
    fun update(userId: UUID, transactionId: UUID, request: UpdateTransactionRequest): TransactionResponse
    fun delete(userId: UUID, transactionId: UUID)
    fun moveToWallet(userId: UUID, transactionId: UUID, request: MoveTransactionRequest): TransactionResponse
}


