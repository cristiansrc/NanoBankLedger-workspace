package com.nanobank.ledger.infrastructure.adapter.inbound.rest

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.usecase.*
import com.nanobank.ledger.domain.model.TransactionType
import com.nanobank.ledger.infrastructure.adapter.inbound.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
class TransactionController(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val listTransactionsUseCase: ListTransactionsUseCase,
    private val getTransactionUseCase: GetTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val moveTransactionUseCase: MoveTransactionUseCase
) {

    @PostMapping("/api/v1/wallets/{walletId}/transactions")
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable walletId: UUID,
        @Valid @RequestBody request: CreateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val response = createTransactionUseCase.execute(principal.id, walletId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/api/v1/wallets/{walletId}/transactions")
    fun findByWalletId(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable walletId: UUID,
        @RequestParam("category_id", required = false) categoryId: UUID?,
        @RequestParam("date_from", required = false) dateFrom: String?,
        @RequestParam("date_to", required = false) dateTo: String?,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<List<TransactionResponse>> {
        val transactionType = type?.uppercase()?.let {
            try {
                TransactionType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        val filters = TransactionFilters(
            categoryId = categoryId,
            dateFrom = dateFrom?.let { LocalDate.parse(it) },
            dateTo = dateTo?.let { LocalDate.parse(it) },
            type = transactionType
        )
        val transactions = listTransactionsUseCase.execute(principal.id, walletId, filters)
        return ResponseEntity.ok(transactions)
    }

    @GetMapping("/api/v1/transactions/{id}")
    fun findById(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<TransactionResponse> {
        val transaction = getTransactionUseCase.execute(principal.id, id)
        return ResponseEntity.ok(transaction)
    }

    @PatchMapping("/api/v1/transactions/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val transaction = updateTransactionUseCase.execute(principal.id, id, request)
        return ResponseEntity.ok(transaction)
    }

    @DeleteMapping("/api/v1/transactions/{id}")
    fun delete(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        deleteTransactionUseCase.execute(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/api/v1/transactions/{id}/move")
    fun moveToWallet(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: MoveTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val transaction = moveTransactionUseCase.execute(principal.id, id, request)
        return ResponseEntity.ok(transaction)
    }
}
