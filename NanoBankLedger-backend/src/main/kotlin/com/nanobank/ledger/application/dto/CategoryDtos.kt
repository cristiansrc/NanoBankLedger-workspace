package com.nanobank.ledger.application.dto

import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val type: String,
    val icon: String?,
    val color: String?
)
