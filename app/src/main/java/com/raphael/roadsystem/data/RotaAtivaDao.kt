package com.raphael.roadsystem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RotaAtivaDao {
    @Query("SELECT id FROM rota_ativa ORDER BY ordem ASC")
    fun getIdsRotaAtiva(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarRota(rota: List<RotaAtivaEntity>)

    @Query("DELETE FROM rota_ativa")
    suspend fun limparRota()

    @Query("DELETE FROM rota_ativa WHERE id = :clienteId")
    suspend fun removerCliente(clienteId: String)
}
