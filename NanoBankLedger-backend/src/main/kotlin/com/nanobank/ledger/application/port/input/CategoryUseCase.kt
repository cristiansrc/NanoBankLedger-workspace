package com.nanobank.ledger.application.port.input

import com.nanobank.ledger.application.dto.CategoryResponse
import java.util.UUID

interface CategoryUseCase {
    fun findAll(): List<CategoryResponse>
    fun findById(id: UUID): CategoryResponse
}
