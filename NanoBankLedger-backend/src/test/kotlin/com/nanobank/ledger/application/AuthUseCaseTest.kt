package com.nanobank.ledger.application

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.RefreshTokenRepositoryPort
import com.nanobank.ledger.application.port.output.UserRepositoryPort
import com.nanobank.ledger.application.usecase.*
import com.nanobank.ledger.domain.exception.*
import com.nanobank.ledger.domain.model.RefreshToken
import com.nanobank.ledger.domain.model.User
import com.nanobank.ledger.domain.service.AccessToken
import com.nanobank.ledger.domain.service.PasswordHasher
import com.nanobank.ledger.domain.service.RefreshTokenResult
import com.nanobank.ledger.domain.service.TokenProvider
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class AuthUseCaseTest {

    // Mocks
    private lateinit var userRepository: UserRepositoryPort
    private lateinit var refreshTokenRepository: RefreshTokenRepositoryPort
    private lateinit var passwordHasher: PasswordHasher
    private lateinit var tokenProvider: TokenProvider

    // Use cases
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var refreshAccessTokenUseCase: RefreshAccessTokenUseCase
    private lateinit var logoutUseCase: LogoutUseCase

    private val userId = UUID.randomUUID()
    private val familyId = UUID.randomUUID()
    private val accessTokenStr = "access-token-123"
    private val refreshTokenStr = "refresh-token-456"
    private val refreshTokenHash = "hashed-refresh-token-456"

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        refreshTokenRepository = mockk()
        passwordHasher = mockk()
        tokenProvider = mockk()

        registerUseCase = RegisterUseCase(
            userRepository, refreshTokenRepository, passwordHasher, tokenProvider
        )
        loginUseCase = LoginUseCase(
            userRepository, refreshTokenRepository, passwordHasher, tokenProvider
        )
        refreshAccessTokenUseCase = RefreshAccessTokenUseCase(
            refreshTokenRepository, userRepository, tokenProvider
        )
        logoutUseCase = LogoutUseCase(refreshTokenRepository)
    }

    // ==================== REGISTER ====================

    @Test
    fun `should register user successfully`() {
        val request = RegisterRequest(name = "John", email = "john@test.com", password = "password123")
        val savedUser = User(id = userId, name = "John", email = "john@test.com", passwordHash = "hashed")

        every { userRepository.existsByEmail("john@test.com") } returns false
        every { passwordHasher.hash("password123") } returns "hashed"
        every { userRepository.save(any()) } returns savedUser
        every { tokenProvider.generateAccessToken(savedUser) } returns AccessToken(accessTokenStr, 900000L)
        every { tokenProvider.generateRefreshToken() } returns RefreshTokenResult(refreshTokenStr, refreshTokenHash, familyId)
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        val response = registerUseCase.execute(request)

        assertEquals("John", response.user.name)
        assertEquals("john@test.com", response.user.email)
        assertEquals(accessTokenStr, response.accessToken)
        assertEquals(refreshTokenStr, response.refreshToken)
        assertEquals(900000L, response.expiresIn)

        verify { userRepository.existsByEmail("john@test.com") }
        verify { userRepository.save(any()) }
        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `should throw EmailAlreadyExistsException when registering duplicate email`() {
        val request = RegisterRequest(name = "John", email = "existing@test.com", password = "password123")

        every { userRepository.existsByEmail("existing@test.com") } returns true

        assertThrows<EmailAlreadyExistsException> { registerUseCase.execute(request) }

        verify { userRepository.existsByEmail("existing@test.com") }
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    // ==================== LOGIN ====================

    @Test
    fun `should login successfully`() {
        val request = LoginRequest(email = "john@test.com", password = "password123")
        val user = User(id = userId, name = "John", email = "john@test.com", passwordHash = "hashed")

        every { userRepository.findByEmail("john@test.com") } returns Optional.of(user)
        every { passwordHasher.verify("password123", "hashed") } returns true
        every { tokenProvider.generateAccessToken(user) } returns AccessToken(accessTokenStr, 900000L)
        every { tokenProvider.generateRefreshToken() } returns RefreshTokenResult(refreshTokenStr, refreshTokenHash, familyId)
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        val response = loginUseCase.execute(request)

        assertEquals("John", response.user.name)
        assertEquals(accessTokenStr, response.accessToken)
    }

    @Test
    fun `should throw InvalidCredentialsException when email not found`() {
        val request = LoginRequest(email = "unknown@test.com", password = "password123")

        every { userRepository.findByEmail("unknown@test.com") } returns Optional.empty()

        assertThrows<InvalidCredentialsException> { loginUseCase.execute(request) }
    }

    @Test
    fun `should throw InvalidCredentialsException when password is wrong`() {
        val request = LoginRequest(email = "john@test.com", password = "wrong")
        val user = User(id = userId, name = "John", email = "john@test.com", passwordHash = "hashed")

        every { userRepository.findByEmail("john@test.com") } returns Optional.of(user)
        every { passwordHasher.verify("wrong", "hashed") } returns false

        assertThrows<InvalidCredentialsException> { loginUseCase.execute(request) }
    }

    // ==================== REFRESH ====================

    @Test
    fun `should refresh token successfully with rotation`() {
        val request = RefreshTokenRequest(refreshToken = refreshTokenStr)
        val storedToken = RefreshToken(
            userId = userId,
            tokenHash = refreshTokenHash,
            familyId = familyId,
            expiresAt = Instant.now().plus(1, ChronoUnit.DAYS),
            usedAt = null,
            revokedAt = null
        )
        val user = User(id = userId, name = "John", email = "john@test.com", passwordHash = "hashed")
        val newRefreshTokenStr = "new-refresh-token"
        val newRefreshTokenHash = "new-hashed-refresh-token"
        val newFamilyId = familyId

        every { refreshTokenRepository.findByTokenHash(any()) } returns Optional.of(storedToken)
        every { refreshTokenRepository.findByFamilyId(familyId) } returns listOf(storedToken)
        every { refreshTokenRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { tokenProvider.generateAccessToken(user) } returns AccessToken("new-access-token", 900000L)
        every { tokenProvider.generateRefreshToken() } returns RefreshTokenResult(newRefreshTokenStr, newRefreshTokenHash, newFamilyId)

        val response = refreshAccessTokenUseCase.execute(request)

        assertEquals("new-access-token", response.accessToken)
        assertEquals(newRefreshTokenStr, response.refreshToken)
        verify(exactly = 2) { refreshTokenRepository.save(any()) } // marcar usado + guardar nuevo
    }

    @Test
    fun `should throw TokenRevokedException when token not found`() {
        val request = RefreshTokenRequest(refreshToken = "invalid-token")

        every { refreshTokenRepository.findByTokenHash(any()) } returns Optional.empty()

        assertThrows<TokenRevokedException> { refreshAccessTokenUseCase.execute(request) }
    }

    @Test
    fun `should detect token reuse and revoke entire family - RN-009`() {
        val request = RefreshTokenRequest(refreshToken = refreshTokenStr)
        val storedToken = RefreshToken(
            userId = userId,
            tokenHash = refreshTokenHash,
            familyId = familyId,
            expiresAt = Instant.now().plus(1, ChronoUnit.DAYS),
            usedAt = Instant.now(), // Ya usado!
            revokedAt = null
        )
        val familyTokens = listOf(
            storedToken,
            RefreshToken(
                userId = userId,
                tokenHash = "other-hash",
                familyId = familyId,
                expiresAt = Instant.now().plus(1, ChronoUnit.DAYS)
            )
        )

        every { refreshTokenRepository.findByTokenHash(any()) } returns Optional.of(storedToken)
        every { refreshTokenRepository.findByFamilyId(familyId) } returns familyTokens
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        assertThrows<TokenFamilyReuseException> { refreshAccessTokenUseCase.execute(request) }

        // Verificar que todos los tokens de la familia fueron revocados
        verify(exactly = 2) { refreshTokenRepository.save(match { it.revokedAt != null }) }
    }

    @Test
    fun `should throw TokenExpiredException when token is expired`() {
        val request = RefreshTokenRequest(refreshToken = refreshTokenStr)
        val storedToken = RefreshToken(
            userId = userId,
            tokenHash = refreshTokenHash,
            familyId = familyId,
            expiresAt = Instant.now().minus(1, ChronoUnit.HOURS), // Expirado
            usedAt = null,
            revokedAt = null
        )

        every { refreshTokenRepository.findByTokenHash(any()) } returns Optional.of(storedToken)
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        assertThrows<TokenExpiredException> { refreshAccessTokenUseCase.execute(request) }

        // Verificar que el token expirado fue revocado
        verify { refreshTokenRepository.save(match { it.revokedAt != null }) }
    }

    // ==================== LOGOUT ====================

    @Test
    fun `should revoke refresh token on logout - RN-012`() {
        val storedToken = RefreshToken(
            userId = userId,
            tokenHash = refreshTokenHash,
            familyId = familyId,
            expiresAt = Instant.now().plus(1, ChronoUnit.DAYS),
            revokedAt = null
        )

        every { refreshTokenRepository.findByTokenHash(any()) } returns Optional.of(storedToken)
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        logoutUseCase.execute(refreshTokenStr, userId)

        verify { refreshTokenRepository.save(match { it.revokedAt != null }) }
    }

    @Test
    fun `should not revoke token of different user on logout`() {
        val otherUserId = UUID.randomUUID()
        val storedToken = RefreshToken(
            userId = otherUserId,
            tokenHash = refreshTokenHash,
            familyId = familyId,
            expiresAt = Instant.now().plus(1, ChronoUnit.DAYS),
            revokedAt = null
        )

        every { refreshTokenRepository.findByTokenHash(any()) } returns Optional.of(storedToken)

        logoutUseCase.execute(refreshTokenStr, userId)

        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `should handle logout when token not found gracefully`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns Optional.empty()

        // Should not throw
        logoutUseCase.execute(refreshTokenStr, userId)

        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }
}
