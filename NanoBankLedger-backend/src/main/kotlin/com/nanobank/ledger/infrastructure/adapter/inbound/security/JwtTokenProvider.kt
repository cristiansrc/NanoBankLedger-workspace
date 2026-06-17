package com.nanobank.ledger.infrastructure.adapter.inbound.security

import com.nanobank.ledger.domain.model.User
import com.nanobank.ledger.domain.service.AccessToken
import com.nanobank.ledger.domain.service.RefreshTokenResult
import com.nanobank.ledger.domain.service.TokenProvider
import com.nanobank.ledger.domain.service.TokenValidationResult
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.access-token.secret}") private val accessSecret: String,
    @Value("\${jwt.access-token.expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-token.expiration}") private val refreshExpiration: Long
) : TokenProvider {

    private val accessKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(accessSecret.toByteArray())
    }

    override fun generateAccessToken(user: User): AccessToken {
        val now = Date()
        val expiryDate = Date(now.time + accessExpiration)

        val jws = Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("name", user.name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(accessKey)
            .compact()

        return AccessToken(token = jws, expiresIn = accessExpiration)
    }

    override fun generateRefreshToken(): RefreshTokenResult {
        val random = SecureRandom()
        val tokenBytes = ByteArray(64)
        random.nextBytes(tokenBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)

        val digest = MessageDigest.getInstance("SHA-256")
        val tokenHash = digest.digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }

        return RefreshTokenResult(
            token = token,
            tokenHash = tokenHash,
            familyId = UUID.randomUUID()
        )
    }

    override fun validateToken(token: String): TokenValidationResult {
        return try {
            val claims = Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)

            TokenValidationResult(
                isValid = true,
                userId = UUID.fromString(claims.payload.subject)
            )
        } catch (e: ExpiredJwtException) {
            TokenValidationResult(isValid = false, error = "Token expired")
        } catch (e: JwtException) {
            TokenValidationResult(isValid = false, error = "Invalid token")
        }
    }
}
