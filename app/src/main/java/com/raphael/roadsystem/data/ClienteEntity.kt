package com.raphael.roadsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que representa um cliente no banco de dados local.
 * Inclui o grupo para filtragem dinâmica.
 */
@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey val id: String,
    val nomeCliente: String,
    val endereco: String,
    val latitude: Double,
    val longitude: Double,
    val ordemVisita: Int = 0,
    val status: String = "PENDENTE",
    val grupoFiltro: String = "Sem Categoria",
    val sincronizado: Boolean = false
)
