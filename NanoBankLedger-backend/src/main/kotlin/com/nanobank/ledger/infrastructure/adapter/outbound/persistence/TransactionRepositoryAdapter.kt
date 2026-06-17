package com.nanobank.ledger.infrastructure.adapter.outbound.persistence

import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.domain.model.Transaction
import com.nanobank.ledger.domain.model.TransactionType as DomainTransactionType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.TransactionType as EntityTransactionType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper.TransactionMapper
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.CategoryJpaRepository
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.TransactionJpaRepository
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.WalletJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.*

@Component
class TransactionRepositoryAdapter(
    private val jpaRepository: TransactionJpaRepository,
    private val walletJpaRepository: WalletJpaRepository,
    private val categoryJpaRepository: CategoryJpaRepository,
    private val mapper: TransactionMapper
) : TransactionRepositoryPort {

    override fun save(transaction: Transaction): Transaction {
        val walletEntity = walletJpaRepository.findById(transaction.walletId)
            .orElseThrow { RuntimeException("Wallet not found: ${transaction.walletId}") }
        val categoryEntity = transaction.categoryId?.let {
            categoryJpaRepository.findById(it).orElse(null)
        }
        val entity = mapper.toEntity(transaction, walletEntity, categoryEntity)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findById(id: UUID): Optional<Transaction> {
        return jpaRepository.findById(id).map { mapper.toDomain(it) }
    }

    override fun findByWalletId(walletId: UUID): List<Transaction> {
        return jpaRepository.findByWallet_IdOrderByDateDescCreatedAtDesc(walletId)
            .map { mapper.toDomain(it) }
    }

    override fun findByFilters(
        walletId: UUID,
        categoryId: UUID?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        type: DomainTransactionType?
    ): List<Transaction> {
        val entityType = type?.let { EntityTransactionType.valueOf(it.name) }
        return jpaRepository.findByFilters(walletId, categoryId, dateFrom, dateTo, entityType)
            .map { mapper.toDomain(it) }
    }

    override fun countByWalletId(walletId: UUID): Long {
        return jpaRepository.countByWallet_Id(walletId)
    }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}
