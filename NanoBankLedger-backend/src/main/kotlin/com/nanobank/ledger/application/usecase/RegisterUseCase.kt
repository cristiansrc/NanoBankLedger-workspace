package com.nanobank.ledger.application.usecase

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.RefreshTokenRepositoryPort
import com.nanobank.ledger.application.port.output.UserRepositoryPort
import com.nanobank.ledger.domain.exception.EmailAlreadyExistsException
import com.nanobank.ledger.domain.model.RefreshToken
import com.nanobank.ledger.domain.model.User
import com.nanobank.ledger.domain.service.PasswordHasher
import com.nanobank.ledger.domain.service.TokenProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class RegisterUseCase(
    private val userRepository: UserRepositoryPort,
    private val refreshTokenRepository: RefreshTokenRepositoryPort,
    private val passwordHasher: PasswordHasher,
    private val tokenProvider: TokenProvider
) {
    @Transactional
    fun execute(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException()
        }

        val user = User(
            name = request.name,
            email = request.email,
            passwordHash = passwordHasher.hash(request.password)
        )

        val savedUser = userRepository.save(user)

        val accessToken = tokenProvider.generateAccessToken(savedUser)
        val refreshTokenResult = tokenProvider.generateRefreshToken()

        val refreshToken = RefreshToken(
            userId = savedUser.id,
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
                id = savedUser.id,
                name = savedUser.name,
                email = savedUser.email
            )
        )
    }
}
