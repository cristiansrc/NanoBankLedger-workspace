package com.nanobank.ledger.application

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.application.usecase.*
import com.nanobank.ledger.domain.model.Wallet
import com.nanobank.ledger.domain.model.WalletType
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*

class WalletUseCaseTest {

    private lateinit var walletRepository: WalletRepositoryPort
    private lateinit var transactionRepository: TransactionRepositoryPort

    private lateinit var createWalletUseCase: CreateWalletUseCase
    private lateinit var listWalletsUseCase: ListWalletsUseCase
    private lateinit var getWalletUseCase: GetWalletUseCase
    private lateinit var updateWalletUseCase: UpdateWalletUseCase
    private lateinit var deleteWalletUseCase: DeleteWalletUseCase

    private val userId = UUID.randomUUID()
    private val walletId = UUID.randomUUID()
    private val wallet = Wallet(
        id = walletId,
        userId = userId,
        name = "Ahorros",
        type = WalletType.SAVINGS,
        balance = BigDecimal("1000.00")
    )

    @BeforeEach
    fun setUp() {
        walletRepository = mockk()
        transactionRepository = mockk()

        createWalletUseCase = CreateWalletUseCase(walletRepository)
        listWalletsUseCase = ListWalletsUseCase(walletRepository)
        getWalletUseCase = GetWalletUseCase(walletRepository)
        updateWalletUseCase = UpdateWalletUseCase(walletRepository)
        deleteWalletUseCase = DeleteWalletUseCase(walletRepository, transactionRepository)
    }

    // ==================== CREATE ====================

    @Test
    fun `should create wallet successfully`() {
        val request = CreateWalletRequest(name = "Ahorros", type = "SAVINGS", initialBalance = BigDecimal("1000.00"))

        every { walletRepository.save(any()) } returns wallet

        val result = createWalletUseCase.execute(userId, request)

        assertEquals("Ahorros", result.name)
        assertEquals("SAVINGS", result.type)
        assertEquals(BigDecimal("1000.00"), result.balance)
        assertEquals(userId, result.userId)
        verify { walletRepository.save(any()) }
    }

    @Test
    fun `should create wallet with zero balance when initialBalance is null`() {
        val request = CreateWalletRequest(name = "Gastos", type = "CHECKING", initialBalance = null)
        val walletWithZeroBalance = wallet.copy(balance = BigDecimal.ZERO, name = "Gastos", type = WalletType.CHECKING)

        every { walletRepository.save(any()) } returns walletWithZeroBalance

        val result = createWalletUseCase.execute(userId, request)

        assertEquals(BigDecimal.ZERO, result.balance)
        assertEquals("CHECKING", result.type)
    }

    @Test
    fun `should default to CHECKING type when invalid type provided`() {
        val request = CreateWalletRequest(name = "Test", type = "INVALID_TYPE", initialBalance = null)
        val walletWithDefaultType = wallet.copy(balance = BigDecimal.ZERO, name = "Test", type = WalletType.CHECKING)

        every { walletRepository.save(any()) } returns walletWithDefaultType

        val result = createWalletUseCase.execute(userId, request)

        assertEquals("CHECKING", result.type)
    }

    // ==================== LIST ====================

    @Test
    fun `should return wallets for user`() {
        every { walletRepository.findByUserId(userId) } returns listOf(wallet)

        val results = listWalletsUseCase.execute(userId)

        assertEquals(1, results.size)
        assertEquals("Ahorros", results[0].name)
    }

    @Test
    fun `should return empty list when user has no wallets`() {
        every { walletRepository.findByUserId(userId) } returns emptyList()

        val results = listWalletsUseCase.execute(userId)

        assertTrue(results.isEmpty())
    }

    // ==================== GET ====================

    @Test
    fun `should return wallet by id`() {
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        val result = getWalletUseCase.execute(userId, walletId)

        assertEquals(walletId, result.id)
        assertEquals("Ahorros", result.name)
    }

    @Test
    fun `should throw when wallet not found`() {
        every { walletRepository.findById(walletId) } returns Optional.empty()

        assertThrows<NoSuchElementException> { getWalletUseCase.execute(userId, walletId) }
    }

    @Test
    fun `should throw when wallet belongs to different user - ownership`() {
        val otherUserId = UUID.randomUUID()
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> { getWalletUseCase.execute(otherUserId, walletId) }
    }

    // ==================== UPDATE ====================

    @Test
    fun `should update wallet successfully`() {
        val request = UpdateWalletRequest(name = "Ahorros Actualizado", type = "INVESTMENT")
        val updatedWallet = wallet.copy(name = "Ahorros Actualizado", type = WalletType.INVESTMENT)

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.save(any()) } returns updatedWallet

        val result = updateWalletUseCase.execute(userId, walletId, request)

        assertEquals("Ahorros Actualizado", result.name)
        assertEquals("INVESTMENT", result.type)
    }

    @Test
    fun `should throw when updating non-existent wallet`() {
        val request = UpdateWalletRequest(name = "Test", type = "CHECKING")

        every { walletRepository.findById(walletId) } returns Optional.empty()

        assertThrows<NoSuchElementException> { updateWalletUseCase.execute(userId, walletId, request) }
    }

    @Test
    fun `should throw when updating wallet of different user - ownership`() {
        val otherUserId = UUID.randomUUID()
        val request = UpdateWalletRequest(name = "Test", type = "CHECKING")

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> { updateWalletUseCase.execute(otherUserId, walletId, request) }
    }

    // ==================== DELETE ====================

    @Test
    fun `should delete wallet successfully`() {
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { transactionRepository.countByWalletId(walletId) } returns 0L
        every { walletRepository.deleteById(walletId) } just Runs

        deleteWalletUseCase.execute(userId, walletId)

        verify { walletRepository.deleteById(walletId) }
    }

    @Test
    fun `should throw when deleting non-existent wallet`() {
        every { walletRepository.findById(walletId) } returns Optional.empty()

        assertThrows<NoSuchElementException> { deleteWalletUseCase.execute(userId, walletId) }
    }

    @Test
    fun `should throw when deleting wallet of different user - ownership`() {
        val otherUserId = UUID.randomUUID()

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> { deleteWalletUseCase.execute(otherUserId, walletId) }
    }

    @Test
    fun `should throw when deleting wallet with existing transactions - RN-017`() {
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { transactionRepository.countByWalletId(walletId) } returns 5L

        val exception = assertThrows<IllegalStateException> {
            deleteWalletUseCase.execute(userId, walletId)
        }
        assertTrue(exception.message!!.contains("Cannot delete wallet with existing transactions"))
        verify(exactly = 0) { walletRepository.deleteById(any()) }
    }
}
