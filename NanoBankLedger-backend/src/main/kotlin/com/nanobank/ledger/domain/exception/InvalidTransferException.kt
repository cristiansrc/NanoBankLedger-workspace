package com.nanobank.ledger.domain.exception

open class InvalidTransferException(message: String) : RuntimeException(message)

class SameWalletTransferException : InvalidTransferException("Cannot transfer transaction to the same wallet")
