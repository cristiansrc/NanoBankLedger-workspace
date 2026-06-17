package com.nanobank.ledger.application

import com.nanobank.ledger.application.dto.CategoryResponse
import com.nanobank.ledger.application.port.output.CategoryRepositoryPort
import com.nanobank.ledger.application.usecase.ListCategoriesUseCase
import com.nanobank.ledger.domain.model.Category
import com.nanobank.ledger.domain.model.CategoryType
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class CategoryUseCaseTest {

    private lateinit var categoryRepository: CategoryRepositoryPort
    private lateinit var listCategoriesUseCase: ListCategoriesUseCase

    private val categoryId = UUID.randomUUID()
    private val category = Category(
        id = categoryId,
        name = "Salary",
        type = CategoryType.INCOME,
        icon = "salary-icon",
        color = "#00FF00"
    )

    @BeforeEach
    fun setUp() {
        categoryRepository = mockk()
        listCategoriesUseCase = ListCategoriesUseCase(categoryRepository)
    }

    @Test
    fun `should return all categories`() {
        every { categoryRepository.findAll() } returns listOf(category)

        val results = listCategoriesUseCase.findAll()

        assertEquals(1, results.size)
        assertEquals("Salary", results[0].name)
        assertEquals("INCOME", results[0].type)
        assertEquals("salary-icon", results[0].icon)
        assertEquals("#00FF00", results[0].color)
    }

    @Test
    fun `should return empty list when no categories exist`() {
        every { categoryRepository.findAll() } returns emptyList()

        val results = listCategoriesUseCase.findAll()

        assertTrue(results.isEmpty())
    }

    @Test
    fun `should return category by id`() {
        every { categoryRepository.findById(categoryId) } returns Optional.of(category)

        val result = listCategoriesUseCase.findById(categoryId)

        assertEquals(categoryId, result.id)
        assertEquals("Salary", result.name)
    }

    @Test
    fun `should throw when category not found`() {
        val unknownId = UUID.randomUUID()
        every { categoryRepository.findById(unknownId) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            listCategoriesUseCase.findById(unknownId)
        }
    }
}
