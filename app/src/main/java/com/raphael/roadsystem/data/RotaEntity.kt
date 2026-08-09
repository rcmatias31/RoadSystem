package com.raphael.roadsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabela_rotas")
data class RotaEntity(
    @PrimaryKey val uid: String,
    val nomeCliente: String,
    val endereco: String,
    val latitude: Double,
    val longitude: Double,
    val ordemVisita: Int,
    val status: String
)
