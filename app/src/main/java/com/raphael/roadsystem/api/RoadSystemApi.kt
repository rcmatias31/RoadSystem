package com.raphael.roadsystem.api

import com.raphael.roadsystem.model.Route
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface RoadSystemApi {

    @GET("get-routes-api")
    suspend fun getRoutes(
        @Header("Authorization") token: String
    ): List<RouteDto>

    @POST("register-checkin-api")
    suspend fun registerCheckIn(
        @Header("Authorization") token: String,
        @Body request: CheckInRequest
    ): CheckInResponse
}

data class CheckInRequest(
    val routeId: String,
    val timestamp: String,
    val type: String // "PRESENCIAL" ou "REMOTO"
)

data class CheckInResponse(
    val success: Boolean,
    val message: String
)
