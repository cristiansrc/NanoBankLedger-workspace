package com.nanobank.ledger.infrastructure.adapter.outbound.persistence

import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.Wallet
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper.WalletMapper
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.UserJpaRepository
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.WalletJpaRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class WalletRepositoryAdapter(
    private val jpaRepository: WalletJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val mapper: WalletMapper
) : WalletRepositoryPort {

    override fun save(wallet: Wallet): Wallet {
        val userEntity = userJpaRepository.findById(wallet.userId)
            .orElseThrow { RuntimeException("User not found: ${wallet.userId}") }
        val entity = mapper.toEntity(wallet, userEntity)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findById(id: UUID): Optional<Wallet> {
        return jpaRepository.findById(id).map { mapper.toDomain(it) }
    }

    override fun findByUserId(userId: UUID): List<Wallet> {
        return jpaRepository.findByUser_Id(userId).map { mapper.toDomain(it) }
    }

    override fun existsByIdAndUserId(id: UUID, userId: UUID): Boolean {
        return jpaRepository.existsByIdAndUser_Id(id, userId)
    }

    override fun deleteById(id: UUID) {
        jpaRepository.deleteById(id)
    }
}
