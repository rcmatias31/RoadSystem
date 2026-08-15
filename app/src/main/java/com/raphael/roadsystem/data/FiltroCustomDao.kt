package com.raphael.roadsystem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FiltroCustomDao {
    @Query("SELECT * FROM filtros_custom ORDER BY nome ASC")
    fun listarTodos(): Flow<List<FiltroCustomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(filtro: FiltroCustomEntity)

    @Query("DELETE FROM filtros_custom WHERE nome = :nome")
    suspend fun deletar(nome: String)
}
