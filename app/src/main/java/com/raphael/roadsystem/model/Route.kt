package com.raphael.roadsystem.model

/**
 * Modelo de domínio para rotas e clientes usado na UI.
 * As coordenadas são garantidas como Double.
 */
data class Route(
    val id: String,
    val clientName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val grupoFiltro: String? = null
)
