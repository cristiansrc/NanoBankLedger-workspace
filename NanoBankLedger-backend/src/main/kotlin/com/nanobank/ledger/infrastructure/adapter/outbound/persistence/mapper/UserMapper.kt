package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper

import com.nanobank.ledger.domain.model.User
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class UserMapper {

    fun toDomain(entity: UserEntity): User = User(
        id = entity.id,
        name = entity.name,
        email = entity.email,
        passwordHash = entity.passwordHash,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(domain: User): UserEntity = UserEntity(
        id = domain.id,
        name = domain.name,
        email = domain.email,
        passwordHash = domain.passwordHash,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}
