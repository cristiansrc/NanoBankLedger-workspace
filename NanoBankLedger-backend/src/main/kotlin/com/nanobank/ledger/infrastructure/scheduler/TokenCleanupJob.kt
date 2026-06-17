package com.nanobank.ledger.infrastructure.scheduler

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class TokenCleanupJob(
    private val jdbcTemplate: JdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(TokenCleanupJob::class.java)

    /**
     * Ejecuta cada día a las 3:00 AM.
     * Elimina refresh tokens que:
     * - Han expirado (expires_at < now)
     * - O han sido revocados hace más de 30 días (revoked_at IS NOT NULL AND revoked_at < now - 30 days)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    fun cleanupExpiredTokens() {
        val startTime = Instant.now()
        logger.info("Iniciando limpieza de tokens expirados/revocados...")

        val deletedCount = jdbcTemplate.update(
            """DELETE FROM refresh_tokens 
               WHERE expires_at < ? 
               OR (revoked_at IS NOT NULL AND revoked_at < ?)""",
            java.sql.Timestamp.from(startTime),
            java.sql.Timestamp.from(startTime.minusSeconds(30 * 24 * 60 * 60L))
        )

        val duration = Duration.between(startTime, Instant.now()).toMillis()
        logger.info("Limpieza completada: $deletedCount tokens eliminados en ${duration}ms")
    }
}
