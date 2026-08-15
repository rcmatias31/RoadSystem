package com.raphael.roadsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rota_ativa")
data class RotaAtivaEntity(
    @PrimaryKey val id: String, // ID do cliente
    val ordem: Int // Posição na rota
)
