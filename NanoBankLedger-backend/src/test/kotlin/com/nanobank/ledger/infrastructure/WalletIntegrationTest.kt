package com.nanobank.ledger.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.UserRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.User
import com.nanobank.ledger.domain.model.Wallet
import com.nanobank.ledger.domain.model.WalletType
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
import java.math.BigDecimal
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WalletIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepositoryPort

    @Autowired
    private lateinit var walletRepository: WalletRepositoryPort

    @Autowired
    private lateinit var passwordHasher: PasswordHasher

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    private lateinit var authToken: String
    private lateinit var userId: UUID

    @BeforeEach
    fun setUp() {
        val email = "wallet-test-${UUID.randomUUID()}@example.com"
        val user = User(name = "Wallet Tester", email = email, passwordHash = passwordHasher.hash("password123"))
        val savedUser = userRepository.save(user)
        userId = savedUser.id

        val accessToken = tokenProvider.generateAccessToken(savedUser)
        authToken = accessToken.token
    }

    @Test
    fun `should create wallet successfully`() {
        val request = CreateWalletRequest(
            name = "Ahorros",
            type = "SAVINGS",
            initialBalance = BigDecimal("1000.00")
        )

        mockMvc.perform(
            post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Ahorros"))
            .andExpect(jsonPath("$.type").value("SAVINGS"))
            .andExpect(jsonPath("$.balance").value(1000.00))
            .andExpect(jsonPath("$.user_id").value(userId.toString()))
    }

    @Test
    fun `should create wallet with CHECKING type by default`() {
        val request = CreateWalletRequest(name = "Checking", type = "", initialBalance = null)

        mockMvc.perform(
            post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("CHECKING"))
            .andExpect(jsonPath("$.balance").value(0.0))
    }

    @Test
    fun `should return 403 when creating wallet without auth`() {
        val request = CreateWalletRequest(name = "Test", type = "CHECKING")

        mockMvc.perform(
            post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should list wallets for authenticated user`() {
        // Create two wallets
        walletRepository.save(Wallet(userId = userId, name = "Wallet 1", type = WalletType.CHECKING))
        walletRepository.save(Wallet(userId = userId, name = "Wallet 2", type = WalletType.SAVINGS))

        mockMvc.perform(
            get("/api/v1/wallets")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").isString)
    }

    @Test
    fun `should return empty list when user has no wallets`() {
        mockMvc.perform(
            get("/api/v1/wallets")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should get wallet by id`() {
        val wallet = walletRepository.save(
            Wallet(userId = userId, name = "Mi Billetera", type = WalletType.SAVINGS, balance = BigDecimal("500.00"))
        )

        mockMvc.perform(
            get("/api/v1/wallets/${wallet.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Mi Billetera"))
            .andExpect(jsonPath("$.balance").value(500.00))
    }

    @Test
    fun `should return 404 when getting non-existent wallet`() {
        mockMvc.perform(
            get("/api/v1/wallets/${UUID.randomUUID()}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when getting wallet of other user - ownership`() {
        val otherUser = User(
            name = "Other",
            email = "other-${UUID.randomUUID()}@example.com",
            passwordHash = passwordHasher.hash("password123")
        )
        val savedOther = userRepository.save(otherUser)
        val otherWallet = walletRepository.save(
            Wallet(userId = savedOther.id, name = "Other Wallet", type = WalletType.CHECKING)
        )

        mockMvc.perform(
            get("/api/v1/wallets/${otherWallet.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should update wallet`() {
        val wallet = walletRepository.save(
            Wallet(userId = userId, name = "Viejo Nombre", type = WalletType.CHECKING)
        )

        val request = UpdateWalletRequest(name = "Nuevo Nombre", type = "SAVINGS")

        mockMvc.perform(
            patch("/api/v1/wallets/${wallet.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Nuevo Nombre"))
            .andExpect(jsonPath("$.type").value("SAVINGS"))
    }

    @Test
    fun `should delete wallet successfully`() {
        val wallet = walletRepository.save(
            Wallet(userId = userId, name = "Temp Wallet", type = WalletType.CHECKING)
        )

        mockMvc.perform(
            delete("/api/v1/wallets/${wallet.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNoContent())
    }

    @Test
    fun `should return 404 when deleting non-existent wallet`() {
        mockMvc.perform(
            delete("/api/v1/wallets/${UUID.randomUUID()}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should list categories with authentication`() {
        mockMvc.perform(
            get("/api/v1/categories")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `should return 403 when accessing categories without auth`() {
        mockMvc.perform(
            get("/api/v1/categories")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should complete full CRUD wallet flow`() {
        // Create
        val createResult = mockMvc.perform(
            post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreateWalletRequest(name = "CRUD Wallet", type = "CHECKING", initialBalance = BigDecimal("100"))
                ))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isCreated)
            .andReturn()

        val createdWallet = objectMapper.readValue(
            createResult.response.contentAsString,
            WalletResponse::class.java
        )

        // Read
        mockMvc.perform(
            get("/api/v1/wallets/${createdWallet.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)

        // Update
        mockMvc.perform(
            patch("/api/v1/wallets/${createdWallet.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    UpdateWalletRequest(name = "Updated CRUD Wallet", type = "SAVINGS")
                ))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)

        // Delete
        mockMvc.perform(
            delete("/api/v1/wallets/${createdWallet.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNoContent())

        // Verify deleted
        mockMvc.perform(
            get("/api/v1/wallets/${createdWallet.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNotFound)
    }
}
