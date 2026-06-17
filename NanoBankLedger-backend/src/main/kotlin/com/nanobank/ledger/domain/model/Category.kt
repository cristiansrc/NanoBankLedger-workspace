package com.nanobank.ledger.domain.model

import java.time.Instant
import java.util.UUID

enum class CategoryType {
    INCOME, EXPENSE
}

data class Category(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: CategoryType,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
