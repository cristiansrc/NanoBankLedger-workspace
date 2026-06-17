package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository

import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CategoryJpaRepository : JpaRepository<CategoryEntity, UUID> {
    @Query("SELECT c FROM CategoryEntity c ORDER BY c.type, c.name")
    fun findAllOrdered(): List<CategoryEntity>
}
