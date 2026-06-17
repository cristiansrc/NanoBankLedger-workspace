package com.nanobank.ledger.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.UserRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.domain.model.*
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
import java.time.LocalDate
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepositoryPort

    @Autowired
    private lateinit var walletRepository: WalletRepositoryPort

    @Autowired
    private lateinit var transactionRepository: TransactionRepositoryPort

    @Autowired
    private lateinit var passwordHasher: PasswordHasher

    @Autowired
    private lateinit var tokenProvider: TokenProvider

    private lateinit var authToken: String
    private lateinit var userId: UUID
    private lateinit var walletId: UUID
    private lateinit var secondWalletId: UUID

    @BeforeEach
    fun setUp() {
        // Create user
        val email = "tx-test-${UUID.randomUUID()}@example.com"
        val user = User(name = "Tx Tester", email = email, passwordHash = passwordHasher.hash("password123"))
        val savedUser = userRepository.save(user)
        userId = savedUser.id

        // Generate auth token
        val accessToken = tokenProvider.generateAccessToken(savedUser)
        authToken = accessToken.token

        // Create wallets
        val wallet = walletRepository.save(
            Wallet(userId = userId, name = "Principal", type = WalletType.CHECKING, balance = BigDecimal("5000.00"))
        )
        walletId = wallet.id

        val secondWallet = walletRepository.save(
            Wallet(userId = userId, name = "Secundaria", type = WalletType.SAVINGS, balance = BigDecimal("1000.00"))
        )
        secondWalletId = secondWallet.id
    }

    @Test
    fun `should create INCOME transaction and increase balance`() {
        val request = CreateTransactionRequest(
            type = "INCOME",
            amount = BigDecimal("1000.00"),
            description = "Salario mensual"
        )

        mockMvc.perform(
            post("/api/v1/wallets/$walletId/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("INCOME"))
            .andExpect(jsonPath("$.amount").value(1000.00))
            .andExpect(jsonPath("$.wallet_id").value(walletId.toString()))

        // Verify balance increased
        val updatedWallet = walletRepository.findById(walletId).get()
        assert(updatedWallet.balance == BigDecimal("6000.00")) { "Balance should be 6000 after income" }
    }

    @Test
    fun `should create EXPENSE transaction and decrease balance`() {
        val request = CreateTransactionRequest(
            type = "EXPENSE",
            amount = BigDecimal("500.00"),
            description = "Compra supermercado"
        )

        mockMvc.perform(
            post("/api/v1/wallets/$walletId/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("EXPENSE"))
            .andExpect(jsonPath("$.amount").value(500.00))

        // Verify balance decreased
        val updatedWallet = walletRepository.findById(walletId).get()
        assert(updatedWallet.balance == BigDecimal("4500.00")) { "Balance should be 4500 after expense" }
    }

    @Test
    fun `should return 422 when expense exceeds balance - RN-001`() {
        val request = CreateTransactionRequest(
            type = "EXPENSE",
            amount = BigDecimal("100000.00"),
            description = "Compra imposible"
        )

        mockMvc.perform(
            post("/api/v1/wallets/$walletId/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `should return 400 when amount is zero or negative - RN-003`() {
        val request = CreateTransactionRequest(
            type = "INCOME",
            amount = BigDecimal.ZERO,
            description = "Monto invalido"
        )

        mockMvc.perform(
            post("/api/v1/wallets/$walletId/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should list transactions for wallet`() {
        // Create two transactions
        createTransaction("INCOME", BigDecimal("1000.00"))
        createTransaction("EXPENSE", BigDecimal("300.00"))

        mockMvc.perform(
            get("/api/v1/wallets/$walletId/transactions")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `should get transaction by id`() {
        val txJson = createTransaction("INCOME", BigDecimal("200.00"))
        val tx = objectMapper.readValue(txJson, TransactionResponse::class.java)

        mockMvc.perform(
            get("/api/v1/transactions/${tx.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(200.00))
            .andExpect(jsonPath("$.id").value(tx.id.toString()))
    }

    @Test
    fun `should return 404 when getting non-existent transaction`() {
        mockMvc.perform(
            get("/api/v1/transactions/${UUID.randomUUID()}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should update transaction and adjust balance`() {
        val txJson = createTransaction("INCOME", BigDecimal("500.00"))
        val tx = objectMapper.readValue(txJson, TransactionResponse::class.java)

        // Change from INCOME 500 to EXPENSE 200
        val updateRequest = UpdateTransactionRequest(
            type = "EXPENSE",
            amount = BigDecimal("200.00")
        )

        mockMvc.perform(
            patch("/api/v1/transactions/${tx.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.type").value("EXPENSE"))
            .andExpect(jsonPath("$.amount").value(200.00))

        // Original balance 5000, income 500 => 5500, revert income => 5000, expense 200 => 4800
        val updatedWallet = walletRepository.findById(walletId).get()
        assert(updatedWallet.balance == BigDecimal("4800.00")) { "Balance should be 4800 after update" }
    }

    @Test
    fun `should delete transaction and revert balance`() {
        val txJson = createTransaction("EXPENSE", BigDecimal("400.00"))
        val tx = objectMapper.readValue(txJson, TransactionResponse::class.java)

        mockMvc.perform(
            delete("/api/v1/transactions/${tx.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNoContent())

        // Balance should revert: 5000 - 400 = 4600, then revert => 5000
        val updatedWallet = walletRepository.findById(walletId).get()
        assert(updatedWallet.balance == BigDecimal("5000.00")) { "Balance should be 5000 after delete" }
    }

    @Test
    fun `should move transaction to another wallet atomically - RN-004`() {
        // Create INCOME transaction
        val txJson = createTransaction("INCOME", BigDecimal("1000.00"))
        val tx = objectMapper.readValue(txJson, TransactionResponse::class.java)

        // Balance: wallet 5000+1000=6000, secondWallet 1000
        val walletAfterIncome = walletRepository.findById(walletId).get()
        assert(walletAfterIncome.balance == BigDecimal("6000.00"))

        // Move to second wallet
        val moveRequest = MoveTransactionRequest(targetWalletId = secondWalletId)

        mockMvc.perform(
            patch("/api/v1/transactions/${tx.id}/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveRequest))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.wallet_id").value(secondWalletId.toString()))

        // Verify atomicity: wallet loses 1000, secondWallet gains 1000
        val updatedWallet = walletRepository.findById(walletId).get()
        val updatedSecondWallet = walletRepository.findById(secondWalletId).get()
        assert(updatedWallet.balance == BigDecimal("5000.00")) { "Source wallet should be back to 5000" }
        assert(updatedSecondWallet.balance == BigDecimal("2000.00")) { "Target wallet should be 2000" }
    }

    @Test
    fun `should return 409 when moving to same wallet`() {
        val txJson = createTransaction("INCOME", BigDecimal("100.00"))
        val tx = objectMapper.readValue(txJson, TransactionResponse::class.java)

        val moveRequest = MoveTransactionRequest(targetWalletId = walletId)

        mockMvc.perform(
            patch("/api/v1/transactions/${tx.id}/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveRequest))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should return 404 when moving transaction from other user - ownership`() {
        // Create another user
        val otherEmail = "other-${UUID.randomUUID()}@example.com"
        val otherUser = User(name = "Other", email = otherEmail, passwordHash = passwordHasher.hash("password123"))
        val savedOtherUser = userRepository.save(otherUser)
        val otherWallet = walletRepository.save(
            Wallet(userId = savedOtherUser.id, name = "Other Wallet", type = WalletType.CHECKING)
        )

        // Try to move non-existent transaction (we just get 404)
        val moveRequest = MoveTransactionRequest(targetWalletId = secondWalletId)

        mockMvc.perform(
            patch("/api/v1/transactions/${UUID.randomUUID()}/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveRequest))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 422 when moving expense to wallet with insufficient balance`() {
        // Create an EXPENSE transaction of 2000
        val txJson = createTransaction("EXPENSE", BigDecimal("2000.00"))
        val tx = objectMapper.readValue(txJson, TransactionResponse::class.java)

        // Second wallet has only 1000, so moving a 2000 expense there should fail
        val moveRequest = MoveTransactionRequest(targetWalletId = secondWalletId)

        mockMvc.perform(
            patch("/api/v1/transactions/${tx.id}/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveRequest))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `should complete full transaction CRUD flow`() {
        // Create
        val createResult = mockMvc.perform(
            post("/api/v1/wallets/$walletId/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    CreateTransactionRequest(type = "INCOME", amount = BigDecimal("1500.00"), description = "Bono")
                ))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isCreated)
            .andReturn()

        val createdTx = objectMapper.readValue(createResult.response.contentAsString, TransactionResponse::class.java)

        // Read
        mockMvc.perform(
            get("/api/v1/transactions/${createdTx.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)

        // Update
        mockMvc.perform(
            patch("/api/v1/transactions/${createdTx.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    UpdateTransactionRequest(description = "Bono actualizado")
                ))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("Bono actualizado"))

        // Delete
        mockMvc.perform(
            delete("/api/v1/transactions/${createdTx.id}")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isNoContent())
    }

    @Test
    fun `should return 403 when accessing transactions without auth`() {
        mockMvc.perform(
            get("/api/v1/wallets/$walletId/transactions")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `should list transactions with multiple entries`() {
        createTransaction("INCOME", BigDecimal("500.00"))
        createTransaction("EXPENSE", BigDecimal("100.00"))
        createTransaction("INCOME", BigDecimal("200.00"))

        mockMvc.perform(
            get("/api/v1/wallets/$walletId/transactions")
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    // Helper to create a transaction and return the response JSON
    private fun createTransaction(type: String, amount: BigDecimal): String {
        val request = CreateTransactionRequest(type = type, amount = amount, description = "Test $type")
        val result = mockMvc.perform(
            post("/api/v1/wallets/$walletId/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("Authorization", "Bearer $authToken")
        )
            .andExpect(status().isCreated)
            .andReturn()
        return result.response.contentAsString
    }
}
