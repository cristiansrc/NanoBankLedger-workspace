package com.nanobank.ledger.application.port.output

import com.nanobank.ledger.domain.model.User
import java.util.Optional
import java.util.UUID

interface UserRepositoryPort {
    fun save(user: User): User
    fun findById(id: UUID): Optional<User>
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
}
