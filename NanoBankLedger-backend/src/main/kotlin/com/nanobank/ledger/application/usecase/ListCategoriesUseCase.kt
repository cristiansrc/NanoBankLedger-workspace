package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.CategoryResponse
import com.nanobank.ledger.application.port.output.CategoryRepositoryPort
import org.springframework.stereotype.Component
import java.util.NoSuchElementException
import java.util.UUID

@Component
class ListCategoriesUseCase(
    private val categoryRepository: CategoryRepositoryPort
) {
    fun findAll(): List<CategoryResponse> {
        return categoryRepository.findAll().map { category ->
            CategoryResponse(
                id = category.id,
                name = category.name,
                type = category.type.name,
                icon = category.icon,
                color = category.color
            )
        }
    }

    fun findById(id: UUID): CategoryResponse {
        val category = categoryRepository.findById(id)
            .orElseThrow { NoSuchElementException("Category not found: $id") }
        return CategoryResponse(
            id = category.id,
            name = category.name,
            type = category.type.name,
            icon = category.icon,
            color = category.color
        )
    }
}
