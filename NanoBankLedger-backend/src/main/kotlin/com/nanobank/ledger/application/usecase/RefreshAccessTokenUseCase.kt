package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.RefreshTokenRepositoryPort
import com.nanobank.ledger.application.port.output.UserRepositoryPort
import com.nanobank.ledger.domain.exception.TokenExpiredException
import com.nanobank.ledger.domain.exception.TokenFamilyReuseException
import com.nanobank.ledger.domain.exception.TokenRevokedException
import com.nanobank.ledger.domain.model.RefreshToken
import com.nanobank.ledger.domain.service.TokenProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class RefreshAccessTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val tokenProvider: TokenProvider
) {
    @Transactional
    fun execute(request: RefreshTokenRequest): AuthResponse {
        val tokenHash = hashToken(request.refreshToken)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow { TokenRevokedException() }

        // Deteccion de robo: si el token ya fue usado, revocar toda la familia
        if (storedToken.isUsed() || storedToken.isRevoked()) {
            val familyTokens = refreshTokenRepository.findByFamilyId(storedToken.familyId)
            familyTokens.forEach { token ->
                refreshTokenRepository.save(token.copy(revokedAt = Instant.now()))
            }
            throw TokenFamilyReuseException()
        }

        if (storedToken.isExpired()) {
            refreshTokenRepository.save(storedToken.copy(revokedAt = Instant.now()))
            throw TokenExpiredException()
        }

        // Marcar token actual como usado
        refreshTokenRepository.save(storedToken.copy(usedAt = Instant.now()))

        // Generar nuevo token (misma familia - rotation)
        val user = userRepository.findById(storedToken.userId)
            .orElseThrow { RuntimeException("User not found") }

        val accessToken = tokenProvider.generateAccessToken(user)
        val newRefreshTokenResult = tokenProvider.generateRefreshToken()

        val newRefreshToken = RefreshToken(
            userId = user.id,
            tokenHash = newRefreshTokenResult.tokenHash,
            familyId = storedToken.familyId, // Misma familia
            expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
        )
        refreshTokenRepository.save(newRefreshToken)

        return AuthResponse(
            accessToken = accessToken.token,
            refreshToken = newRefreshTokenResult.token,
            expiresIn = accessToken.expiresIn,
            user = UserSummary(
                id = user.id,
                name = user.name,
                email = user.email
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
