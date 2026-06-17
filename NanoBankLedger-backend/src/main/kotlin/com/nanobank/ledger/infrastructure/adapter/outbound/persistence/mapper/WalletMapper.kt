package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper

import com.nanobank.ledger.domain.model.Wallet
import com.nanobank.ledger.domain.model.WalletType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.WalletEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class WalletMapper {

    fun toDomain(entity: WalletEntity): Wallet = Wallet(
        id = entity.id,
        userId = entity.user.id,
        name = entity.name,
        type = WalletType.valueOf(entity.type.name),
        balance = entity.balance,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(domain: Wallet, userEntity: UserEntity): WalletEntity = WalletEntity(
        id = domain.id,
        user = userEntity,
        name = domain.name,
        type = com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.WalletType.valueOf(domain.type.name),
        balance = domain.balance,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}
