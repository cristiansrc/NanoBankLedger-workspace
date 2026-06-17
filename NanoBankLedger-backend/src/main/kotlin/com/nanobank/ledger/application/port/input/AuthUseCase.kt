package com.nanobank.ledger.application.port.input

import com.nanobank.ledger.application.dto.*

interface AuthUseCase {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
    fun refreshAccessToken(request: RefreshTokenRequest): AuthResponse
    fun logout(refreshToken: String, userId: java.util.UUID)
}
