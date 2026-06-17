package com.nanobank.ledger.application.dto

import com.nanobank.ledger.domain.model.TransactionType
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class TransactionFilters(
    val categoryId: UUID? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val type: TransactionType? = null
)

data class CreateTransactionRequest(
    @field:NotNull(message = "Transaction type is required")
    val type: String,

    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal,

    val categoryId: UUID? = null,

    @field:Size(max = 255, message = "Description must be at most 255 characters")
    val description: String? = null,

    val date: LocalDate? = null
)

data class UpdateTransactionRequest(
    val type: String? = null,

    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    val amount: BigDecimal? = null,

    val categoryId: UUID? = null,

    @field:Size(max = 255, message = "Description must be at most 255 characters")
    val description: String? = null,

    val date: LocalDate? = null
)

data class MoveTransactionRequest(
    @field:NotNull(message = "Target wallet ID is required")
    val targetWalletId: UUID
)

data class TransactionResponse(
    val id: UUID,
    val walletId: UUID,
    val categoryId: UUID?,
    val type: String,
    val amount: BigDecimal,
    val description: String?,
    val date: LocalDate,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant
)

data class PaginatedTransactionResponse(
    val content: List<TransactionResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
