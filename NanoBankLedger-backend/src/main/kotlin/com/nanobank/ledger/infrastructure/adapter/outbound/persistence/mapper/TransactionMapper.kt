package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper

import com.nanobank.ledger.domain.model.Transaction
import com.nanobank.ledger.domain.model.TransactionType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.CategoryEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.TransactionEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.WalletEntity
import org.springframework.stereotype.Component

@Component
class TransactionMapper {

    fun toDomain(entity: TransactionEntity): Transaction = Transaction(
        id = entity.id,
        walletId = entity.wallet.id,
        categoryId = entity.category?.id,
        type = TransactionType.valueOf(entity.type.name),
        amount = entity.amount,
        description = entity.description,
        date = entity.date,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(domain: Transaction, walletEntity: WalletEntity, categoryEntity: CategoryEntity? = null): TransactionEntity = TransactionEntity(
        id = domain.id,
        wallet = walletEntity,
        category = categoryEntity,
        type = com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.TransactionType.valueOf(domain.type.name),
        amount = domain.amount,
        description = domain.description,
        date = domain.date,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}
