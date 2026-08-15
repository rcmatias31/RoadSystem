package com.raphael.roadsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filtros_custom")
data class FiltroCustomEntity(
    @PrimaryKey val nome: String,
    val corHex: String,
    val idsClientes: String // Armazenado como CSV ou JSON string (ex: "id1,id2,id3")
)
