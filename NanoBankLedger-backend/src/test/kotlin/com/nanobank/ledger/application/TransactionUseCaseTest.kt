package com.nanobank.ledger.application

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.port.output.TransactionRepositoryPort
import com.nanobank.ledger.application.port.output.WalletRepositoryPort
import com.nanobank.ledger.application.usecase.*
import com.nanobank.ledger.domain.exception.InsufficientBalanceException
import com.nanobank.ledger.domain.exception.SameWalletTransferException
import com.nanobank.ledger.domain.model.Transaction
import com.nanobank.ledger.domain.model.TransactionType
import com.nanobank.ledger.domain.model.Wallet
import com.nanobank.ledger.domain.model.WalletType
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class TransactionUseCaseTest {

    private lateinit var transactionRepository: TransactionRepositoryPort
    private lateinit var walletRepository: WalletRepositoryPort

    private lateinit var createTransactionUseCase: CreateTransactionUseCase
    private lateinit var listTransactionsUseCase: ListTransactionsUseCase
    private lateinit var getTransactionUseCase: GetTransactionUseCase
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
    private lateinit var moveTransactionUseCase: MoveTransactionUseCase

    private val userId = UUID.randomUUID()
    private val walletId = UUID.randomUUID()
    private val targetWalletId = UUID.randomUUID()
    private val transactionId = UUID.randomUUID()

    private val wallet = Wallet(
        id = walletId,
        userId = userId,
        name = "Cuenta Corriente",
        type = WalletType.CHECKING,
        balance = BigDecimal("5000.00")
    )

    private val targetWallet = Wallet(
        id = targetWalletId,
        userId = userId,
        name = "Ahorros",
        type = WalletType.SAVINGS,
        balance = BigDecimal("1000.00")
    )

    private val incomeTransaction = Transaction(
        id = transactionId,
        walletId = walletId,
        categoryId = null,
        type = TransactionType.INCOME,
        amount = BigDecimal("500.00"),
        description = "Salary",
        date = LocalDate.now()
    )

    private val expenseTransaction = Transaction(
        id = UUID.randomUUID(),
        walletId = walletId,
        categoryId = null,
        type = TransactionType.EXPENSE,
        amount = BigDecimal("200.00"),
        description = "Groceries",
        date = LocalDate.now()
    )

    @BeforeEach
    fun setUp() {
        transactionRepository = mockk()
        walletRepository = mockk()

        createTransactionUseCase = CreateTransactionUseCase(transactionRepository, walletRepository)
        listTransactionsUseCase = ListTransactionsUseCase(transactionRepository, walletRepository)
        getTransactionUseCase = GetTransactionUseCase(transactionRepository, walletRepository)
        updateTransactionUseCase = UpdateTransactionUseCase(transactionRepository, walletRepository)
        deleteTransactionUseCase = DeleteTransactionUseCase(transactionRepository, walletRepository)
        moveTransactionUseCase = MoveTransactionUseCase(transactionRepository, walletRepository)
    }

    // ==================== CREATE ====================

    @Test
    fun `should create INCOME transaction and increase balance`() {
        val request = CreateTransactionRequest(
            type = "INCOME",
            amount = BigDecimal("500.00"),
            description = "Salary"
        )
        val updatedWallet = wallet.copy(balance = BigDecimal("5500.00"))

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.save(any()) } returns updatedWallet
        every { transactionRepository.save(any()) } returns incomeTransaction

        val result = createTransactionUseCase.execute(userId, walletId, request)

        assertEquals(TransactionType.INCOME.name, result.type)
        assertEquals(BigDecimal("500.00"), result.amount)
        verify { walletRepository.save(match { it.balance == BigDecimal("5500.00") }) }
    }

    @Test
    fun `should create EXPENSE transaction and decrease balance`() {
        val request = CreateTransactionRequest(
            type = "EXPENSE",
            amount = BigDecimal("200.00"),
            description = "Groceries"
        )
        val updatedWallet = wallet.copy(balance = BigDecimal("4800.00"))

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.save(any()) } returns updatedWallet
        every { transactionRepository.save(any()) } returns expenseTransaction

        val result = createTransactionUseCase.execute(userId, walletId, request)

        assertEquals(TransactionType.EXPENSE.name, result.type)
        assertEquals(BigDecimal("200.00"), result.amount)
        verify { walletRepository.save(match { it.balance == BigDecimal("4800.00") }) }
    }

    @Test
    fun `should throw InsufficientBalanceException when expense exceeds balance - RN-001`() {
        val request = CreateTransactionRequest(
            type = "EXPENSE",
            amount = BigDecimal("10000.00"),
            description = "Big purchase"
        )

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<InsufficientBalanceException> {
            createTransactionUseCase.execute(userId, walletId, request)
        }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `should throw when creating transaction in non-existent wallet`() {
        val request = CreateTransactionRequest(
            type = "INCOME",
            amount = BigDecimal("100.00")
        )

        every { walletRepository.findById(walletId) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            createTransactionUseCase.execute(userId, walletId, request)
        }
    }

    @Test
    fun `should throw when creating transaction in wallet of different user - ownership`() {
        val otherUserId = UUID.randomUUID()
        val request = CreateTransactionRequest(
            type = "INCOME",
            amount = BigDecimal("100.00")
        )

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> {
            createTransactionUseCase.execute(otherUserId, walletId, request)
        }
    }

    @Test
    fun `should throw when transaction type is invalid`() {
        val request = CreateTransactionRequest(
            type = "INVALID",
            amount = BigDecimal("100.00")
        )

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<IllegalArgumentException> {
            createTransactionUseCase.execute(userId, walletId, request)
        }
    }

    @Test
    fun `should create transaction with category id`() {
        val categoryId = UUID.randomUUID()
        val request = CreateTransactionRequest(
            type = "EXPENSE",
            amount = BigDecimal("50.00"),
            categoryId = categoryId
        )
        val updatedWallet = wallet.copy(balance = BigDecimal("4950.00"))
        val transactionWithCategory = incomeTransaction.copy(
            categoryId = categoryId,
            type = TransactionType.EXPENSE,
            amount = BigDecimal("50.00")
        )

        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.save(any()) } returns updatedWallet
        every { transactionRepository.save(any()) } returns transactionWithCategory

        val result = createTransactionUseCase.execute(userId, walletId, request)

        assertEquals(categoryId, result.categoryId)
    }

    // ==================== LIST ====================

    @Test
    fun `should list transactions by wallet`() {
        val filters = TransactionFilters()
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { transactionRepository.findByFilters(walletId, null, null, null, null) } returns listOf(incomeTransaction)

        val results = listTransactionsUseCase.execute(userId, walletId, filters)

        assertEquals(1, results.size)
        assertEquals(transactionId, results[0].id)
    }

    @Test
    fun `should list transactions with filters`() {
        val categoryId = UUID.randomUUID()
        val filters = TransactionFilters(
            categoryId = categoryId,
            type = TransactionType.INCOME
        )
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { transactionRepository.findByFilters(walletId, categoryId, null, null, TransactionType.INCOME) } returns listOf(incomeTransaction)

        val results = listTransactionsUseCase.execute(userId, walletId, filters)

        assertEquals(1, results.size)
    }

    @Test
    fun `should throw when listing transactions of non-existent wallet`() {
        val filters = TransactionFilters()
        every { walletRepository.findById(walletId) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            listTransactionsUseCase.execute(userId, walletId, filters)
        }
    }

    @Test
    fun `should throw when listing transactions of wallet owned by other user - ownership`() {
        val otherUserId = UUID.randomUUID()
        val filters = TransactionFilters()
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> {
            listTransactionsUseCase.execute(otherUserId, walletId, filters)
        }
    }

    // ==================== GET ====================

    @Test
    fun `should get transaction by id`() {
        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        val result = getTransactionUseCase.execute(userId, transactionId)

        assertEquals(transactionId, result.id)
        assertEquals(BigDecimal("500.00"), result.amount)
    }

    @Test
    fun `should throw when transaction not found`() {
        every { transactionRepository.findById(transactionId) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            getTransactionUseCase.execute(userId, transactionId)
        }
    }

    @Test
    fun `should throw when getting transaction of different user - ownership`() {
        val otherUserId = UUID.randomUUID()
        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> {
            getTransactionUseCase.execute(otherUserId, transactionId)
        }
    }

    // ==================== UPDATE ====================

    @Test
    fun `should update transaction and adjust balance`() {
        val request = UpdateTransactionRequest(
            amount = BigDecimal("300.00"),
            description = "Updated salary"
        )
        // Revert original INCOME of 500, then apply new INCOME of 300
        val revertedWallet = wallet.copy(balance = BigDecimal("4500.00"))
        val finalWallet = wallet.copy(balance = BigDecimal("4800.00"))
        val updatedTransaction = incomeTransaction.copy(
            amount = BigDecimal("300.00"),
            description = "Updated salary"
        )

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.save(any()) } returnsMany listOf(revertedWallet, finalWallet)
        every { transactionRepository.save(any()) } returns updatedTransaction

        val result = updateTransactionUseCase.execute(userId, transactionId, request)

        assertEquals(BigDecimal("300.00"), result.amount)
        assertEquals("Updated salary", result.description)
    }

    @Test
    fun `should throw when updating non-existent transaction`() {
        val request = UpdateTransactionRequest(amount = BigDecimal("100.00"))

        every { transactionRepository.findById(transactionId) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            updateTransactionUseCase.execute(userId, transactionId, request)
        }
    }

    @Test
    fun `should throw when updating transaction of different user - ownership`() {
        val otherUserId = UUID.randomUUID()
        val request = UpdateTransactionRequest(amount = BigDecimal("100.00"))

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> {
            updateTransactionUseCase.execute(otherUserId, transactionId, request)
        }
    }

    @Test
    fun `should throw InsufficientBalanceException when updating to expense with insufficient balance`() {
        val request = UpdateTransactionRequest(
            type = "EXPENSE",
            amount = BigDecimal("10000.00")
        )

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        // After reverting income of 500, balance = 4500, but expense is 10000
        assertThrows<InsufficientBalanceException> {
            updateTransactionUseCase.execute(userId, transactionId, request)
        }
    }

    // ==================== DELETE ====================

    @Test
    fun `should delete INCOME transaction and revert balance`() {
        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.save(any()) } returns wallet
        every { transactionRepository.deleteById(transactionId) } just Runs

        deleteTransactionUseCase.execute(userId, transactionId)

        // Balance should decrease by 500 (revert income)
        verify { walletRepository.save(match { it.balance == BigDecimal("4500.00") }) }
        verify { transactionRepository.deleteById(transactionId) }
    }

    @Test
    fun `should delete EXPENSE transaction and revert balance`() {
        every { transactionRepository.findById(expenseTransaction.id) } returns Optional.of(expenseTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.save(any()) } returns wallet
        every { transactionRepository.deleteById(expenseTransaction.id) } just Runs

        deleteTransactionUseCase.execute(userId, expenseTransaction.id)

        // Balance should increase by 200 (revert expense)
        verify { walletRepository.save(match { it.balance == BigDecimal("5200.00") }) }
        verify { transactionRepository.deleteById(expenseTransaction.id) }
    }

    @Test
    fun `should throw when deleting non-existent transaction`() {
        every { transactionRepository.findById(transactionId) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            deleteTransactionUseCase.execute(userId, transactionId)
        }
    }

    @Test
    fun `should throw when deleting transaction of different user - ownership`() {
        val otherUserId = UUID.randomUUID()

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> {
            deleteTransactionUseCase.execute(otherUserId, transactionId)
        }
    }

    // ==================== MOVE (Transferencia atómica) ====================

    @Test
    fun `should move INCOME transaction between wallets atomically - RN-004`() {
        val request = MoveTransactionRequest(targetWalletId = targetWalletId)

        // INCOME transaction originally in wallet: +500 income
        // Revert: wallet loses 500 (4500)
        // Apply: target gains 500 (1500)
        val walletAfterRevert = wallet.copy(balance = BigDecimal("4500.00"))
        val targetAfterApply = targetWallet.copy(balance = BigDecimal("1500.00"))

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.findById(targetWalletId) } returns Optional.of(targetWallet)
        every { walletRepository.save(walletAfterRevert) } returns walletAfterRevert
        every { walletRepository.save(targetAfterApply) } returns targetAfterApply
        every { transactionRepository.save(any()) } returns incomeTransaction.copy(walletId = targetWalletId)

        val result = moveTransactionUseCase.execute(userId, transactionId, request)

        assertEquals(targetWalletId, result.walletId)
        verify { walletRepository.save(walletAfterRevert) }
        verify { walletRepository.save(targetAfterApply) }
    }

    @Test
    fun `should move EXPENSE transaction between wallets atomically - RN-004`() {
        val request = MoveTransactionRequest(targetWalletId = targetWalletId)

        // EXPENSE transaction originally in wallet: -200 expense
        // Revert: wallet gains 200 (5200)
        // Apply: target loses 200 (800)
        val walletAfterRevert = wallet.copy(balance = BigDecimal("5200.00"))
        val targetAfterApply = targetWallet.copy(balance = BigDecimal("800.00"))

        every { transactionRepository.findById(expenseTransaction.id) } returns Optional.of(expenseTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.findById(targetWalletId) } returns Optional.of(targetWallet)
        every { walletRepository.save(walletAfterRevert) } returns walletAfterRevert
        every { walletRepository.save(targetAfterApply) } returns targetAfterApply
        every { transactionRepository.save(any()) } returns expenseTransaction.copy(walletId = targetWalletId)

        val result = moveTransactionUseCase.execute(userId, expenseTransaction.id, request)

        assertEquals(targetWalletId, result.walletId)
    }

    @Test
    fun `should throw SameWalletTransferException when moving to same wallet`() {
        val request = MoveTransactionRequest(targetWalletId = walletId) // misma wallet

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<SameWalletTransferException> {
            moveTransactionUseCase.execute(userId, transactionId, request)
        }
    }

    @Test
    fun `should throw when moving transaction to non-existent target wallet`() {
        val request = MoveTransactionRequest(targetWalletId = UUID.randomUUID())

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.findById(any()) } returns Optional.empty()

        assertThrows<NoSuchElementException> {
            moveTransactionUseCase.execute(userId, transactionId, request)
        }
    }

    @Test
    fun `should throw when moving transaction with insufficient balance in target wallet for EXPENSE`() {
        val request = MoveTransactionRequest(targetWalletId = targetWalletId)
        val poorTargetWallet = targetWallet.copy(balance = BigDecimal("50.00")) // Solo 50, no alcanza para 200

        // revert phase saves source wallet (balance + 200 = 5200)
        val revertedSource = wallet.copy(balance = BigDecimal("5200.00"))

        every { transactionRepository.findById(expenseTransaction.id) } returns Optional.of(expenseTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.findById(targetWalletId) } returns Optional.of(poorTargetWallet)
        every { walletRepository.save(any()) } returnsMany listOf(revertedSource, wallet) // revert + revert-back

        assertThrows<InsufficientBalanceException> {
            moveTransactionUseCase.execute(userId, expenseTransaction.id, request)
        }
    }

    @Test
    fun `should throw when moving transaction of different user - ownership`() {
        val otherUserId = UUID.randomUUID()
        val request = MoveTransactionRequest(targetWalletId = targetWalletId)

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)

        assertThrows<NoSuchElementException> {
            moveTransactionUseCase.execute(otherUserId, transactionId, request)
        }
    }

    @Test
    fun `should throw when moving to target wallet of different user - ownership`() {
        val otherUserWallet = Wallet(
            id = targetWalletId,
            userId = UUID.randomUUID(), // Otro usuario
            name = "Other Wallet",
            type = WalletType.CHECKING,
            balance = BigDecimal("5000.00")
        )
        val request = MoveTransactionRequest(targetWalletId = targetWalletId)

        every { transactionRepository.findById(transactionId) } returns Optional.of(incomeTransaction)
        every { walletRepository.findById(walletId) } returns Optional.of(wallet)
        every { walletRepository.findById(targetWalletId) } returns Optional.of(otherUserWallet)

        assertThrows<NoSuchElementException> {
            moveTransactionUseCase.execute(userId, transactionId, request)
        }
    }
}
