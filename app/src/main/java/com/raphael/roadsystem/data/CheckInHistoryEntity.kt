package com.raphael.roadsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checkin_history")
data class CheckInHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clienteId: String,
    val nomeCliente: String,
    val dataHora: String, // dd/MM/yyyy HH:mm:ss para exibição
    val dataIso: String,  // yyyy-MM-dd para filtros SQL
    val tipo: String,
    val latitude: Double?,
    val longitude: Double?
)
