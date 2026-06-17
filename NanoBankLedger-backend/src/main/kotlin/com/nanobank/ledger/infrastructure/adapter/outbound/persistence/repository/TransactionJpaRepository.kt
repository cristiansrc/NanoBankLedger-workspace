package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository

import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.TransactionEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.TransactionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface TransactionJpaRepository : JpaRepository<TransactionEntity, UUID> {

    fun findByWallet_IdOrderByDateDescCreatedAtDesc(walletId: UUID): List<TransactionEntity>

    @Query("""
        SELECT t FROM TransactionEntity t 
        WHERE t.wallet.id = :walletId 
        AND (:categoryId IS NULL OR t.category.id = :categoryId)
        AND (t.date >= COALESCE(:dateFrom, t.date))
        AND (t.date <= COALESCE(:dateTo, t.date))
        AND (:type IS NULL OR t.type = :type)
        ORDER BY t.date DESC, t.createdAt DESC
    """)
    fun findByFilters(
        @Param("walletId") walletId: UUID,
        @Param("categoryId") categoryId: UUID? = null,
        @Param("dateFrom") dateFrom: LocalDate? = null,
        @Param("dateTo") dateTo: LocalDate? = null,
        @Param("type") type: TransactionType? = null
    ): List<TransactionEntity>

    fun countByWallet_Id(walletId: UUID): Long
}
