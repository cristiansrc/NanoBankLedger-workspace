package com.nanobank.ledger.application.port.output

import com.nanobank.ledger.domain.model.Category
import java.util.Optional
import java.util.UUID

interface CategoryRepositoryPort {
    fun findAll(): List<Category>
    fun findById(id: UUID): Optional<Category>
}
