package com.raphael.roadsystem.api

import com.google.gson.annotations.SerializedName

/**
 * Modelo de dados bruto vindo da API/Google Sheets.
 * Latitude e Longitude são tratados como Any? para lidar com formatos inconsistentes (notação científica, falta de ponto, etc).
 */
data class RouteDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("clientName")
    val clientName: String,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("latitude")
    val latitude: Any?,
    
    @SerializedName("longitude")
    val longitude: Any?,
    
    @SerializedName("status")
    val status: String?,
    
    @SerializedName("grupoFiltro")
    val grupoFiltro: String? = null
)
