package com.raphael.roadsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkins_pendentes")
data class CheckInPendenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clienteId: String,
    val dataHora: String,
    val tipo: String, // "PRESENCIAL" ou "REMOTO"
    val auditLat: Double? = null, // Latitude onde o botão foi clicado
    val auditLng: Double? = null  // Longitude onde o botão foi clicado
)
