package com.nanobank.ledger.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.RefreshTokenRepositoryPort
import com.nanobank.ledger.application.port.output.UserRepositoryPort
import com.nanobank.ledger.domain.model.RefreshToken
import com.nanobank.ledger.domain.model.User
import com.nanobank.ledger.domain.service.PasswordHasher
import com.nanobank.ledger.domain.service.TokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertNotNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepositoryPort

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepositoryPort

    @Autowired
    private lateinit var passwordHasher: PasswordHasher

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    @BeforeEach
    fun cleanUp() {
        // No easy way to clean DB in tests without service; we use transactional tests
    }

    @Test
    fun `should register a new user successfully`() {
        val request = RegisterRequest(
            name = "Test User",
            email = "test-${UUID.randomUUID()}@example.com",
            password = "SecurePass123!"
        )

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.access_token").isNotEmpty)
            .andExpect(jsonPath("$.refresh_token").isNotEmpty)
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.user.name").value("Test User"))
            .andExpect(jsonPath("$.user.email").value(request.email))
    }

    @Test
    fun `should return 409 when registering with existing email`() {
        val email = "existing-${UUID.randomUUID()}@example.com"
        val user = User(name = "Existing", email = email, passwordHash = passwordHasher.hash("password123"))
        userRepository.save(user)

        val request = RegisterRequest(name = "Test", email = email, password = "password123")

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should return 400 when registering with invalid data`() {
        val request = RegisterRequest(name = "", email = "invalid", password = "short")

        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should login successfully`() {
        val email = "login-${UUID.randomUUID()}@example.com"
        val user = User(name = "Login User", email = email, passwordHash = passwordHasher.hash("password123"))
        userRepository.save(user)

        val request = LoginRequest(email = email, password = "password123")

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").isNotEmpty)
            .andExpect(jsonPath("$.refresh_token").isNotEmpty)
            .andExpect(jsonPath("$.user.email").value(email))
    }

    @Test
    fun `should return 401 when login with wrong credentials`() {
        val request = LoginRequest(email = "nonexistent@example.com", password = "wrong")

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should refresh token successfully`() {
        // Arrange: register a user to get tokens
        val email = "refresh-${UUID.randomUUID()}@example.com"
        val registerRequest = RegisterRequest(
            name = "Refresh User",
            email = email,
            password = "password123"
        )

        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val registerResponse = objectMapper.readValue(
            registerResult.response.contentAsString,
            AuthResponse::class.java
        )

        // Act: refresh with the obtained refresh token
        val refreshRequest = RefreshTokenRequest(refreshToken = registerResponse.refreshToken)

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").isNotEmpty)
            .andExpect(jsonPath("$.refresh_token").isNotEmpty)
            // The new refresh token should be different (rotation - RN-008)
            .andExpect(jsonPath("$.refresh_token").value(org.hamcrest.Matchers.not(registerResponse.refreshToken)))
    }

    @Test
    fun `should return 401 when refreshing with invalid token`() {
        val request = RefreshTokenRequest(refreshToken = "invalid-token")

        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should logout successfully - RN-012`() {
        // Register a user
        val email = "logout-${UUID.randomUUID()}@example.com"
        val registerRequest = RegisterRequest(
            name = "Logout User",
            email = email,
            password = "password123"
        )

        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val registerResponse = objectMapper.readValue(
            registerResult.response.contentAsString,
            AuthResponse::class.java
        )

        // Login to get user context for logout
        val loginRequest = LoginRequest(email = email, password = "password123")
        val loginResult = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val loginResponse = objectMapper.readValue(
            loginResult.response.contentAsString,
            AuthResponse::class.java
        )

        // Logout
        val logoutRequest = RefreshTokenRequest(refreshToken = registerResponse.refreshToken)

        mockMvc.perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest))
                .header("Authorization", "Bearer ${loginResponse.accessToken}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))
    }

    @Test
    fun `should complete full auth flow register-login-refresh-logout`() {
        // Register
        val email = "full-${UUID.randomUUID()}@example.com"
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    RegisterRequest(name = "Full Flow", email = email, password = "password123")
                ))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val authResponse = objectMapper.readValue(
            registerResult.response.contentAsString,
            AuthResponse::class.java
        )
        assertNotNull(authResponse.accessToken)
        assertNotNull(authResponse.refreshToken)

        // Login
        val loginResult = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    LoginRequest(email = email, password = "password123")
                ))
        )
            .andExpect(status().isOk)
            .andReturn()

        val loginResponse = objectMapper.readValue(
            loginResult.response.contentAsString,
            AuthResponse::class.java
        )

        // Refresh
        val refreshResult = mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    RefreshTokenRequest(refreshToken = authResponse.refreshToken)
                ))
        )
            .andExpect(status().isOk)
            .andReturn()

        val refreshResponse = objectMapper.readValue(
            refreshResult.response.contentAsString,
            AuthResponse::class.java
        )

        // Logout with new refresh token
        mockMvc.perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    RefreshTokenRequest(refreshToken = refreshResponse.refreshToken)
                ))
                .header("Authorization", "Bearer ${loginResponse.accessToken}")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should return health status`() {
        mockMvc.perform(
            get("/api/v1/health")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty)
    }

    @Test
    fun `should detect refresh token reuse - RN-009`() {
        // Register to get tokens
        val email = "reuse-${UUID.randomUUID()}@example.com"
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    RegisterRequest(name = "Reuse Test", email = email, password = "password123")
                ))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val authResponse = objectMapper.readValue(
            registerResult.response.contentAsString,
            AuthResponse::class.java
        )

        // First refresh (should succeed)
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    RefreshTokenRequest(refreshToken = authResponse.refreshToken)
                ))
        )
            .andExpect(status().isOk)

        // Second refresh with same token (should fail - token reuse detection)
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    RefreshTokenRequest(refreshToken = authResponse.refreshToken)
                ))
        )
            .andExpect(status().isUnauthorized)
    }
}
