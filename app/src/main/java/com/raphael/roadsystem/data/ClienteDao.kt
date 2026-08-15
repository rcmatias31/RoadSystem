package com.raphael.roadsystem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarTodos(clientes: List<ClienteEntity>)

    @Query("SELECT * FROM clientes ORDER BY nomeCliente ASC")
    fun listarTodos(): Flow<List<ClienteEntity>>

    @Query("SELECT DISTINCT grupoFiltro FROM clientes WHERE grupoFiltro != '' ORDER BY grupoFiltro ASC")
    fun listarGrupos(): Flow<List<String>>

    @Query("SELECT * FROM clientes WHERE grupoFiltro = :grupo ORDER BY nomeCliente ASC")
    fun listarClientesPorGrupo(grupo: String): Flow<List<ClienteEntity>>

    @Query("DELETE FROM clientes")
    suspend fun limparTudo()
}
