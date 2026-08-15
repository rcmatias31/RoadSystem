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

    @POST("add-client-api")
    suspend fun addClient(
        @Header("Authorization") token: String,
        @Body request: AddClientRequest
    ): AddClientResponse
}

data class AddClientRequest(
    val id: String,
    val clientName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

data class AddClientResponse(
    val success: Boolean,
    val message: String
)

data class CheckInRequest(
    val routeId: String,
    val timestamp: String,
    val type: String, // "PRESENCIAL" ou "REMOTO"
    val lat: Double? = null,
    val lng: Double? = null
)

data class CheckInResponse(
    val success: Boolean,
    val message: String
)
