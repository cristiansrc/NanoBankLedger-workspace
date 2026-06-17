package com.nanobank.ledger.infrastructure.adapter.inbound.rest

import com.nanobank.ledger.application.dto.*
import com.nanobank.ledger.application.usecase.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val refreshAccessTokenUseCase: RefreshAccessTokenUseCase,
    private val logoutUseCase: LogoutUseCase
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = registerUseCase.execute(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = loginUseCase.execute(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val response = refreshAccessTokenUseCase.execute(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody request: RefreshTokenRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<MessageResponse> {
        val userId = UUID.fromString(userDetails.username)
        logoutUseCase.execute(request.refreshToken, userId)
        return ResponseEntity.ok(MessageResponse("Logged out successfully"))
    }
}
