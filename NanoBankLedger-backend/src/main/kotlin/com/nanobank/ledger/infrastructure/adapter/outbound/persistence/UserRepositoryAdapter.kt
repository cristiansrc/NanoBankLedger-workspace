package com.nanobank.ledger.infrastructure.adapter.outbound.persistence

import com.nanobank.ledger.application.port.output.UserRepositoryPort
import com.nanobank.ledger.domain.model.User
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper.UserMapper
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.UserJpaRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserRepositoryAdapter(
    private val jpaRepository: UserJpaRepository,
    private val mapper: UserMapper
) : UserRepositoryPort {

    override fun save(user: User): User {
        val entity = mapper.toEntity(user)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findById(id: UUID): Optional<User> {
        return jpaRepository.findById(id).map { mapper.toDomain(it) }
    }

    override fun findByEmail(email: String): Optional<User> {
        return jpaRepository.findByEmail(email).map { mapper.toDomain(it) }
    }

    override fun existsByEmail(email: String): Boolean {
        return jpaRepository.existsByEmail(email)
    }
}
