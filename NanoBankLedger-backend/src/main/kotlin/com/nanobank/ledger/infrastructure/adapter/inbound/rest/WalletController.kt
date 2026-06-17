package com.nanobank.ledger.infrastructure.adapter.inbound.rest

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.usecase.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import com.nanobank.ledger.infrastructure.adapter.inbound.security.UserPrincipal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/wallets")
class WalletController(
    private val createWalletUseCase: CreateWalletUseCase,
    private val listWalletsUseCase: ListWalletsUseCase,
    private val getWalletUseCase: GetWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase
) {

    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateWalletRequest
    ): ResponseEntity<WalletResponse> {
        val response = createWalletUseCase.execute(principal.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun findAll(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<WalletResponse>> {
        val wallets = listWalletsUseCase.execute(principal.id)
        return ResponseEntity.ok(wallets)
    }

    @GetMapping("/{id}")
    fun findById(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<WalletResponse> {
        val wallet = getWalletUseCase.execute(principal.id, id)
        return ResponseEntity.ok(wallet)
    }

    @PatchMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateWalletRequest
    ): ResponseEntity<WalletResponse> {
        val wallet = updateWalletUseCase.execute(principal.id, id, request)
        return ResponseEntity.ok(wallet)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        deleteWalletUseCase.execute(principal.id, id)
        return ResponseEntity.noContent().build()
    }
}
