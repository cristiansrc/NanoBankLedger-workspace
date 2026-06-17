package com.nanobank.ledger.domain.exception

class InsufficientBalanceException(
    walletId: java.util.UUID,
    currentBalance: java.math.BigDecimal,
    requiredAmount: java.math.BigDecimal
) : RuntimeException(
    "Insufficient balance in wallet $walletId. Current: $currentBalance, Required: $requiredAmount"
)
