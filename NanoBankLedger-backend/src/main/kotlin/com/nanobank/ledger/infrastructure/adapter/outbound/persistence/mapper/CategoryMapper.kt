package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper

import com.nanobank.ledger.domain.model.Category
import com.nanobank.ledger.domain.model.CategoryType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.CategoryEntity
import org.springframework.stereotype.Component

@Component
class CategoryMapper {

    fun toDomain(entity: CategoryEntity): Category = Category(
        id = entity.id,
        name = entity.name,
        type = CategoryType.valueOf(entity.type.name),
        icon = entity.icon,
        color = entity.color,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(domain: Category): CategoryEntity = CategoryEntity(
        id = domain.id,
        name = domain.name,
        type = com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.CategoryType.valueOf(domain.type.name),
        icon = domain.icon,
        color = domain.color,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}
