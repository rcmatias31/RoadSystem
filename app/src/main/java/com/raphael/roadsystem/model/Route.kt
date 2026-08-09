package com.raphael.roadsystem.model

import com.google.gson.annotations.SerializedName

data class Route(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("clientName")
    val clientName: String,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("status")
    val status: String
)
