package com.nanobank.ledger.infrastructure.adapter.outbound.persistence

import com.nanobank.ledger.application.port.output.RefreshTokenRepositoryPort
import com.nanobank.ledger.domain.model.RefreshToken
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper.RefreshTokenMapper
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.RefreshTokenJpaRepository
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

@Component
class RefreshTokenRepositoryAdapter(
    private val jpaRepository: RefreshTokenJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val mapper: RefreshTokenMapper
) : RefreshTokenRepositoryPort {

    override fun save(token: RefreshToken): RefreshToken {
        val userEntity = userJpaRepository.findById(token.userId)
            .orElseThrow { RuntimeException("User not found: ${token.userId}") }
        val entity = mapper.toEntity(token, userEntity)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findByTokenHash(tokenHash: String): Optional<RefreshToken> {
        return jpaRepository.findByTokenHash(tokenHash)
            .map { mapper.toDomain(it) }
    }

    override fun findByUserIdAndNotRevoked(userId: UUID): List<RefreshToken> {
        return jpaRepository.findByUser_IdAndRevokedAtIsNull(userId)
            .map { mapper.toDomain(it) }
    }

    override fun findByFamilyId(familyId: UUID): List<RefreshToken> {
        return jpaRepository.findByFamilyId(familyId)
            .map { mapper.toDomain(it) }
    }

    override fun deleteByUserId(userId: UUID) {
        jpaRepository.deleteByUser_Id(userId)
    }
}
