package com.controlcom.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ControlComApi {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @GET("api/pairing/info")
    suspend fun getPairingInfo(): PairingInfoResponse

    @POST("api/auth/pair")
    suspend fun pair(@Body request: PairRequest): PairResponse

    @POST("api/power/sleep")
    suspend fun sleep(): OkResponse

    @POST("api/power/shutdown")
    suspend fun shutdown(): ShutdownResponse

    @GET("api/audio/mute")
    suspend fun getMute(): MuteResponse

    @POST("api/audio/mute/toggle")
    suspend fun toggleMute(): MuteResponse

    @GET("api/display/mode")
    suspend fun getDisplayMode(): DisplayModeResponse

    @POST("api/display/mode")
    suspend fun setDisplayMode(@Body request: DisplayModeRequest): DisplayModeResponse
}
