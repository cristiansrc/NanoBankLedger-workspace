package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.port.output.RefreshTokenRepositoryPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

@Component
class LogoutUseCase(
    private val refreshTokenRepository: RefreshTokenRepositoryPort
) {
    @Transactional
    fun execute(refreshToken: String, userId: java.util.UUID) {
        val tokenHash = hashToken(refreshToken)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)

        storedToken.ifPresent { token ->
            if (token.userId == userId) {
                refreshTokenRepository.save(token.copy(revokedAt = java.time.Instant.now()))
            }
        }
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
