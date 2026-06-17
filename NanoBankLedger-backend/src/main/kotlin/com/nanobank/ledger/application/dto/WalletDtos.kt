package com.nanobank.ledger.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class CreateWalletRequest(
    @field:NotBlank(message = "Wallet name is required")
    @field:Size(max = 100, message = "Name must be at most 100 characters")
    val name: String,

    val type: String = "CHECKING",

    val initialBalance: BigDecimal? = null
)

data class UpdateWalletRequest(
    @field:NotBlank(message = "Wallet name is required")
    @field:Size(max = 100, message = "Name must be at most 100 characters")
    val name: String,

    val type: String? = null
)

data class WalletResponse(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val type: String,
    val balance: BigDecimal,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant
)
