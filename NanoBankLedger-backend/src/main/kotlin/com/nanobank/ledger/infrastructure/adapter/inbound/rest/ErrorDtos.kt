package com.nanobank.ledger.infrastructure.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ApiErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val path: String,
    @get:JsonProperty("trace_id")
    val traceId: String,
    val details: List<ApiErrorDetail> = emptyList()
)

data class ApiErrorDetail(
    val field: String? = null,
    val code: String,
    val message: String,
    @get:JsonProperty("rejected_value")
    val rejectedValue: Any? = null
)
