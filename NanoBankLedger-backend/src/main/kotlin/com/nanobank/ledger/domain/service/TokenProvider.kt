package com.nanobank.ledger.domain.service

import com.nanobank.ledger.domain.model.User
import java.util.UUID

data class AccessToken(
    val token: String,
    val expiresIn: Long  // milliseconds
)

data class RefreshTokenResult(
    val token: String,
    val tokenHash: String,
    val familyId: UUID
)

interface TokenProvider {
    fun generateAccessToken(user: User): AccessToken
    fun generateRefreshToken(): RefreshTokenResult
    fun validateToken(token: String): TokenValidationResult
}

data class TokenValidationResult(
    val isValid: Boolean,
    val userId: UUID? = null,
    val error: String? = null
)
