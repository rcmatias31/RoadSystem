package com.raphael.roadsystem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RotaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirRotas(rotas: List<RotaEntity>)

    @Query("SELECT * FROM tabela_rotas ORDER BY ordemVisita ASC")
    fun listarTodasRotas(): Flow<List<RotaEntity>>
}
