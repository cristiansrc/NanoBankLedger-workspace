package com.nanobank.ledger.infrastructure.adapter.inbound.rest

import com.nanobank.ledger.domain.exception.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler {

    // ============ 400 - Validation Errors ============

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val details = ex.bindingResult.fieldErrors.map { error ->
            ApiErrorDetail(
                field = error.field,
                code = "FIELD_INVALID",
                message = error.defaultMessage ?: "Invalid field value",
                rejectedValue = safeRejectedValue(error)
            )
        }
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "VALIDATION_ERROR",
            message = "The request contains invalid fields",
            request = request,
            details = details
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleInvalidJson(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "INVALID_REQUEST_BODY",
            message = "The request body could not be parsed",
            request = request
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "INVALID_PARAMETER",
            message = "Invalid value for parameter: ${ex.name}",
            request = request
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.BAD_REQUEST,
            code = "INVALID_ARGUMENT",
            message = ex.message ?: "Invalid argument",
            request = request
        )
    }

    // ============ 401 - Unauthorized ============

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(
        ex: InvalidCredentialsException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            code = "INVALID_CREDENTIALS",
            message = "Invalid email or password",
            request = request
        )
    }

    @ExceptionHandler(TokenExpiredException::class)
    fun handleTokenExpired(
        ex: TokenExpiredException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            code = "TOKEN_EXPIRED",
            message = "Token has expired",
            request = request
        )
    }

    @ExceptionHandler(TokenRevokedException::class)
    fun handleTokenRevoked(
        ex: TokenRevokedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            code = "TOKEN_REVOKED",
            message = "Token has been revoked",
            request = request
        )
    }

    @ExceptionHandler(TokenFamilyReuseException::class)
    fun handleTokenFamilyReuse(
        ex: TokenFamilyReuseException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.UNAUTHORIZED,
            code = "TOKEN_FAMILY_REUSE",
            message = "Token reuse detected. All tokens in this family have been revoked",
            request = request
        )
    }

    // ============ 403 - Forbidden ============

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.FORBIDDEN,
            code = "ACCESS_DENIED",
            message = "Access denied",
            request = request
        )
    }

    // ============ 404 - Not Found ============

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        ex: NoSuchElementException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.NOT_FOUND,
            code = "RESOURCE_NOT_FOUND",
            message = ex.message ?: "Resource not found",
            request = request
        )
    }

    // ============ 409 - Conflict ============

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExists(
        ex: EmailAlreadyExistsException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.CONFLICT,
            code = "EMAIL_ALREADY_EXISTS",
            message = "Email already registered",
            request = request
        )
    }

    @ExceptionHandler(SameWalletTransferException::class)
    fun handleSameWalletTransfer(
        ex: SameWalletTransferException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.CONFLICT,
            code = "TRANSFER_SAME_WALLET",
            message = ex.message ?: "Cannot transfer to the same wallet",
            request = request
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        ex: IllegalStateException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.CONFLICT,
            code = "ILLEGAL_STATE",
            message = ex.message ?: "Operation not allowed in current state",
            request = request
        )
    }

    // ============ 422 - Unprocessable Entity ============

    @ExceptionHandler(InsufficientBalanceException::class)
    fun handleInsufficientBalance(
        ex: InsufficientBalanceException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        return buildErrorResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY,
            code = "INSUFFICIENT_BALANCE",
            message = ex.message ?: "Insufficient balance",
            request = request
        )
    }

    // ============ 500 - Internal Server Error ============

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        // Log the error (in a real app, use a logger)
        println("Unhandled exception: ${ex.message}")
        ex.printStackTrace()

        return buildErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            code = "INTERNAL_ERROR",
            message = "An unexpected error occurred",
            request = request
        )
    }

    // ============ Helper Methods ============

    private fun buildErrorResponse(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
        details: List<ApiErrorDetail> = emptyList()
    ): ResponseEntity<ApiErrorResponse> {
        val response = ApiErrorResponse(
            timestamp = Instant.now(),
            status = status.value(),
            error = status.reasonPhrase,
            code = code,
            message = message,
            path = request.requestURI,
            traceId = generateTraceId(),
            details = details
        )
        return ResponseEntity.status(status).body(response)
    }

    private fun generateTraceId(): String {
        val uuid = UUID.randomUUID().toString()
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(uuid.toByteArray())
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    private fun safeRejectedValue(error: FieldError): Any? {
        return if (error.rejectedValue is String || error.rejectedValue == null) {
            error.rejectedValue
        } else {
            error.rejectedValue.toString()
        }
    }
}
