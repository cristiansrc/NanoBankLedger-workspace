package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.RefreshTokenRepositoryPort
import com.nanobank.ledger.application.port.output.UserRepositoryPort
import com.nanobank.ledger.domain.exception.InvalidCredentialsException
import com.nanobank.ledger.domain.model.RefreshToken
import com.nanobank.ledger.domain.service.PasswordHasher
import com.nanobank.ledger.domain.service.TokenProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class LoginUseCase(
    private val userRepository: UserRepositoryPort,
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val passwordHasher: PasswordHasher,
    private val tokenProvider: TokenProvider
) {
    @Transactional
    fun execute(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { InvalidCredentialsException() }

        if (!passwordHasher.verify(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val accessToken = tokenProvider.generateAccessToken(user)
        val refreshTokenResult = tokenProvider.generateRefreshToken()

        val refreshToken = RefreshToken(
            userId = user.id,
            tokenHash = refreshTokenResult.tokenHash,
            familyId = refreshTokenResult.familyId,
            expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
        )
        refreshTokenRepository.save(refreshToken)

        return AuthResponse(
            accessToken = accessToken.token,
            refreshToken = refreshTokenResult.token,
            expiresIn = accessToken.expiresIn,
            user = UserSummary(
                id = user.id,
                name = user.name,
                email = user.email
            )
        )
    }
}
