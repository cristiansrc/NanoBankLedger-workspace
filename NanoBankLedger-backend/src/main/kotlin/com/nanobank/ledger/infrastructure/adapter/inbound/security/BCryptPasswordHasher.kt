package com.nanobank.ledger.infrastructure.adapter.inbound.security

import com.nanobank.ledger.domain.service.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class BCryptPasswordHasher : PasswordHasher {

    private val encoder = BCryptPasswordEncoder()

    override fun hash(password: String): String = encoder.encode(password)

    override fun verify(password: String, hash: String): Boolean = encoder.matches(password, hash)
}
