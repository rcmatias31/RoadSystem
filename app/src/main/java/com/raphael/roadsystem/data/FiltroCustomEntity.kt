package com.raphael.roadsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filtros_custom")
data class FiltroCustomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val corHex: String,
    val idsClientes: String // CSV de IDs
)
