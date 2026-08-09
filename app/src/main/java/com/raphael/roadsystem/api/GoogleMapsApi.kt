package com.raphael.roadsystem.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMapsApi {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String?,
        @Query("key") apiKey: String,
        @Query("mode") mode: String = "driving",
        @Query("language") language: String = "pt-BR"
    ): DirectionsResponse

    @GET("maps/api/geocode/json")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GeocodingResponse
}

data class GeocodingResponse(
    @SerializedName("status") val status: String,
    @SerializedName("results") val results: List<GeocodingResult>
)

data class GeocodingResult(
    @SerializedName("geometry") val geometry: GeocodingGeometry
)

data class GeocodingGeometry(
    @SerializedName("location") val location: LatLngLiteral
)

data class LatLngLiteral(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class DirectionsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("routes") val routes: List<DirectionsRoute>,
    @SerializedName("error_message") val errorMessage: String?
)

data class DirectionsRoute(
    @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline,
    @SerializedName("legs") val legs: List<Leg>,
    @SerializedName("waypoint_order") val waypointOrder: List<Int>?
)

data class OverviewPolyline(
    @SerializedName("points") val points: String
)

data class Leg(
    @SerializedName("distance") val distance: TextValue,
    @SerializedName("duration") val duration: TextValue,
    @SerializedName("steps") val steps: List<Step>
)

data class Step(
    @SerializedName("html_instructions") val instructions: String,
    @SerializedName("distance") val distance: TextValue
)

data class TextValue(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)
