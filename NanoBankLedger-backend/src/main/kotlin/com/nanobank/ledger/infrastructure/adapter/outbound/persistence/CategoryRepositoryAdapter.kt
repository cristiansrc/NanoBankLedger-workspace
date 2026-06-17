package com.nanobank.ledger.infrastructure.adapter.outbound.persistence

import com.nanobank.ledger.application.port.output.CategoryRepositoryPort
import com.nanobank.ledger.domain.model.Category
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper.CategoryMapper
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.CategoryJpaRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class CategoryRepositoryAdapter(
    private val jpaRepository: CategoryJpaRepository,
    private val mapper: CategoryMapper
) : CategoryRepositoryPort {

    override fun findAll(): List<Category> {
        return jpaRepository.findAllOrdered().map { mapper.toDomain(it) }
    }

    override fun findById(id: UUID): Optional<Category> {
        return jpaRepository.findById(id).map { mapper.toDomain(it) }
    }
}
