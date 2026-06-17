package com.nanobank.ledger.infrastructure.adapter.inbound.rest

import com.nanobank.ledger.application.dto.CategoryResponse
import com.nanobank.ledger.application.usecase.ListCategoriesUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val listCategoriesUseCase: ListCategoriesUseCase
) {

    @GetMapping
    fun findAll(): ResponseEntity<List<CategoryResponse>> {
        val categories = listCategoriesUseCase.findAll()
        return ResponseEntity.ok(categories)
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: UUID): ResponseEntity<CategoryResponse> {
        val category = listCategoriesUseCase.findById(id)
        return ResponseEntity.ok(category)
    }
}
