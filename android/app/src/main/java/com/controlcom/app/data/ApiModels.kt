package com.controlcom.app.data

data class HealthResponse(
    val ok: Boolean,
    val service: String,
    val version: String
)

data class OkResponse(
    val ok: Boolean
)

data class PairRequest(
    val code: String
)

data class PairResponse(
    val token: String
)

data class MuteResponse(
    val muted: Boolean
)

data class DisplayModeRequest(
    val mode: String
)

data class DisplayModeResponse(
    val mode: String
)

data class ShutdownResponse(
    val ok: Boolean,
    val message: String,
    val windowsNotified: Int,
    val delaySeconds: Int
)

data class PairingInfoResponse(
    val paired: Boolean,
    val ip: String?,
    val port: Int,
    val code: String?,
    val qrPayload: String?
)

data class ErrorResponse(
    val error: String,
    val message: String? = null
)
