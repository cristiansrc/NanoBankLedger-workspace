package com.nanobank.ledger.infrastructure.persistence

import com.nanobank.ledger.domain.model.Category
import com.nanobank.ledger.domain.model.CategoryType
import com.nanobank.ledger.domain.model.Transaction
import com.nanobank.ledger.domain.model.TransactionType
import com.nanobank.ledger.domain.model.Wallet
import com.nanobank.ledger.domain.model.WalletType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.CategoryRepositoryAdapter
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.TransactionRepositoryAdapter
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.WalletRepositoryAdapter
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.CategoryEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.TransactionEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.WalletEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.UserEntity
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.WalletType as EntityWalletType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.CategoryType as EntityCategoryType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.entity.TransactionType as EntityTransactionType
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper.CategoryMapper
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper.TransactionMapper
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.mapper.WalletMapper
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.CategoryJpaRepository
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.TransactionJpaRepository
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.WalletJpaRepository
import com.nanobank.ledger.infrastructure.adapter.outbound.persistence.repository.UserJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class PersistenceAdapterTest {

    // Wallet adapter mocks
    private lateinit var walletJpaRepository: WalletJpaRepository
    private lateinit var userJpaRepository: UserJpaRepository
    private lateinit var walletMapper: WalletMapper
    private lateinit var walletAdapter: WalletRepositoryAdapter

    // Transaction adapter mocks
    private lateinit var transactionJpaRepository: TransactionJpaRepository
    private lateinit var categoryJpaRepository: CategoryJpaRepository
    private lateinit var transactionMapper: TransactionMapper
    private lateinit var transactionAdapter: TransactionRepositoryAdapter

    // Category adapter mocks
    private lateinit var categoryMapper: CategoryMapper
    private lateinit var categoryAdapter: CategoryRepositoryAdapter

    private val userId = UUID.randomUUID()
    private val walletId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()
    private val transactionId = UUID.randomUUID()
    private val userEntity = UserEntity(id = userId, name = "Test", email = "test@test.com", passwordHash = "hash")
    private val walletEntity = WalletEntity(
        id = walletId, user = userEntity, name = "Test",
        type = EntityWalletType.SAVINGS, balance = BigDecimal("100.00")
    )
    private val categoryEntity = CategoryEntity(
        id = categoryId, name = "Salary",
        type = EntityCategoryType.INCOME, icon = "icon", color = "#FFF"
    )
    private val transactionEntity = TransactionEntity(
        id = transactionId, wallet = walletEntity, category = null,
        type = EntityTransactionType.INCOME, amount = BigDecimal("500.00"), description = "Salary",
        date = LocalDate.now()
    )
    private val transactionEntityWithCategory = TransactionEntity(
        id = transactionId, wallet = walletEntity, category = categoryEntity,
        type = EntityTransactionType.INCOME, amount = BigDecimal("500.00"), description = "Salary",
        date = LocalDate.now()
    )

    private val walletDomain = Wallet(id = walletId, userId = userId, name = "Test", type = WalletType.SAVINGS, balance = BigDecimal("100.00"))
    private val categoryDomain = Category(id = categoryId, name = "Salary", type = CategoryType.INCOME, icon = "icon", color = "#FFF")
    private val transactionDomain = Transaction(
        id = transactionId, walletId = walletId, categoryId = null,
        type = TransactionType.INCOME, amount = BigDecimal("500.00"), description = "Salary",
        date = LocalDate.now()
    )

    @BeforeEach
    fun setUp() {
        walletJpaRepository = mockk()
        userJpaRepository = mockk()
        walletMapper = mockk()
        walletAdapter = WalletRepositoryAdapter(walletJpaRepository, userJpaRepository, walletMapper)

        transactionJpaRepository = mockk()
        categoryJpaRepository = mockk()
        transactionMapper = mockk()
        transactionAdapter = TransactionRepositoryAdapter(
            transactionJpaRepository, walletJpaRepository, categoryJpaRepository, transactionMapper
        )

        categoryMapper = mockk()
        categoryAdapter = CategoryRepositoryAdapter(categoryJpaRepository, categoryMapper)
    }

    // ==================== WALLET ADAPTER ====================

    @Test
    fun `wallet save should throw when user not found`() {
        every { userJpaRepository.findById(userId) } returns Optional.empty()

        val exception = assertThrows<RuntimeException> {
            walletAdapter.save(walletDomain)
        }
        assertTrue(exception.message!!.contains("User not found"))
    }

    @Test
    fun `wallet findById should return empty when not found`() {
        every { walletJpaRepository.findById(walletId) } returns Optional.empty()

        val result = walletAdapter.findById(walletId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `wallet findByUserId should return empty list when no wallets`() {
        every { walletJpaRepository.findByUser_Id(userId) } returns emptyList()

        val result = walletAdapter.findByUserId(userId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `wallet existsByIdAndUserId should return true when exists`() {
        every { walletJpaRepository.existsByIdAndUser_Id(walletId, userId) } returns true

        val result = walletAdapter.existsByIdAndUserId(walletId, userId)

        assertTrue(result)
    }

    @Test
    fun `wallet existsByIdAndUserId should return false when not exists`() {
        every { walletJpaRepository.existsByIdAndUser_Id(walletId, userId) } returns false

        val result = walletAdapter.existsByIdAndUserId(walletId, userId)

        assertFalse(result)
    }

    // ==================== TRANSACTION ADAPTER ====================

    @Test
    fun `transaction save should throw when wallet not found`() {
        every { walletJpaRepository.findById(walletId) } returns Optional.empty()

        val exception = assertThrows<RuntimeException> {
            transactionAdapter.save(transactionDomain)
        }
        assertTrue(exception.message!!.contains("Wallet not found"))
    }

    @Test
    fun `transaction save should work with null category`() {
        every { walletJpaRepository.findById(walletId) } returns Optional.of(walletEntity)
        every { transactionMapper.toEntity(transactionDomain, walletEntity, null) } returns transactionEntity
        every { transactionJpaRepository.save(transactionEntity) } returns transactionEntity
        every { transactionMapper.toDomain(transactionEntity) } returns transactionDomain

        val result = transactionAdapter.save(transactionDomain)

        assertEquals(transactionId, result.id)
        assertNull(result.categoryId)
    }

    @Test
    fun `transaction save should work with non-null category`() {
        val txWithCategory = transactionDomain.copy(categoryId = categoryId)

        every { walletJpaRepository.findById(walletId) } returns Optional.of(walletEntity)
        every { categoryJpaRepository.findById(categoryId) } returns Optional.of(categoryEntity)
        every { transactionMapper.toEntity(txWithCategory, walletEntity, categoryEntity) } returns transactionEntityWithCategory
        every { transactionJpaRepository.save(transactionEntityWithCategory) } returns transactionEntityWithCategory
        every { transactionMapper.toDomain(transactionEntityWithCategory) } returns txWithCategory

        val result = transactionAdapter.save(txWithCategory)

        assertEquals(categoryId, result.categoryId)
    }

    @Test
    fun `transaction save should ignore non-existent category`() {
        val txWithCategory = transactionDomain.copy(categoryId = UUID.randomUUID())

        every { walletJpaRepository.findById(walletId) } returns Optional.of(walletEntity)
        every { categoryJpaRepository.findById(any()) } returns Optional.empty()
        every { transactionMapper.toEntity(txWithCategory, walletEntity, null) } returns transactionEntity
        every { transactionJpaRepository.save(transactionEntity) } returns transactionEntity
        every { transactionMapper.toDomain(transactionEntity) } returns transactionDomain

        val result = transactionAdapter.save(txWithCategory)

        assertNull(result.categoryId)
    }

    @Test
    fun `transaction findById should return empty when not found`() {
        every { transactionJpaRepository.findById(transactionId) } returns Optional.empty()

        val result = transactionAdapter.findById(transactionId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `transaction findByWalletId should return empty list when no transactions`() {
        every { transactionJpaRepository.findByWallet_IdOrderByDateDescCreatedAtDesc(walletId) } returns emptyList()

        val result = transactionAdapter.findByWalletId(walletId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `transaction countByWalletId should return zero`() {
        every { transactionJpaRepository.countByWallet_Id(walletId) } returns 0L

        val result = transactionAdapter.countByWalletId(walletId)

        assertEquals(0L, result)
    }

    // ==================== CATEGORY ADAPTER ====================

    @Test
    fun `category findById should return empty when not found`() {
        every { categoryJpaRepository.findById(categoryId) } returns Optional.empty()

        val result = categoryAdapter.findById(categoryId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `category findAll should return empty list`() {
        every { categoryJpaRepository.findAllOrdered() } returns emptyList()

        val result = categoryAdapter.findAll()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `category findAll should return categories`() {
        every { categoryJpaRepository.findAllOrdered() } returns listOf(categoryEntity)
        every { categoryMapper.toDomain(categoryEntity) } returns categoryDomain

        val result = categoryAdapter.findAll()

        assertEquals(1, result.size)
        assertEquals("Salary", result[0].name)
    }

    @Test
    fun `category findById should return category when found`() {
        every { categoryJpaRepository.findById(categoryId) } returns Optional.of(categoryEntity)
        every { categoryMapper.toDomain(categoryEntity) } returns categoryDomain

        val result = categoryAdapter.findById(categoryId)

        assertTrue(result.isPresent)
        assertEquals("Salary", result.get().name)
    }
}
