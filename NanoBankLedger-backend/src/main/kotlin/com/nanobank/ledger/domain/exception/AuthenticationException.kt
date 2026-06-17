package com.nanobank.ledger.domain.exception

class InvalidCredentialsException : RuntimeException("Invalid email or password")

class TokenExpiredException : RuntimeException("Token has expired")

class TokenRevokedException : RuntimeException("Token has been revoked")

class TokenFamilyReuseException : RuntimeException("Token family reuse detected. All tokens in this family have been revoked")

class EmailAlreadyExistsException : RuntimeException("Email already registered")
