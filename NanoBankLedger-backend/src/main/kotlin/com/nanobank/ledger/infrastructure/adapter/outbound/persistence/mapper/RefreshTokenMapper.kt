package com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper

import com.nanobank.ledger.domain.model.RefreshToken
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.RefreshTokenEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class RefreshTokenMapper {

    fun toDomain(entity: RefreshTokenEntity): RefreshToken = RefreshToken(
        id = entity.id,
        userId = entity.user.id,
        tokenHash = entity.tokenHash,
        familyId = entity.familyId,
        expiresAt = entity.expiresAt,
        issuedAt = entity.issuedAt,
        revokedAt = entity.revokedAt,
        usedAt = entity.usedAt,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(domain: RefreshToken, userEntity: UserEntity): RefreshTokenEntity = RefreshTokenEntity(
        id = domain.id,
        user = userEntity,
        tokenHash = domain.tokenHash,
        familyId = domain.familyId,
        expiresAt = domain.expiresAt,
        issuedAt = domain.issuedAt,
        revokedAt = domain.revokedAt,
        usedAt = domain.usedAt,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}
