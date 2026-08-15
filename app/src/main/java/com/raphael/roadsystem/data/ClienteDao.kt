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

    @Query("SELECT * FROM clientes WHERE sincronizado = 0")
    suspend fun listarClientesPendentesEnvio(): List<ClienteEntity>

    @Query("UPDATE clientes SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: String)

    @Query("SELECT * FROM clientes")
    suspend fun getAllCurrentSyncState(): List<ClienteEntity>

    @Query("SELECT * FROM clientes ORDER BY nomeCliente ASC")
    fun listarTodos(): Flow<List<ClienteEntity>>

    @Query("SELECT DISTINCT grupoFiltro FROM clientes WHERE grupoFiltro != '' ORDER BY grupoFiltro ASC")
    fun listarGrupos(): Flow<List<String>>

    @Query("SELECT * FROM clientes WHERE grupoFiltro = :grupo ORDER BY nomeCliente ASC")
    fun listarClientesPorGrupo(grupo: String): Flow<List<ClienteEntity>>

    @Query("UPDATE clientes SET status = :novoStatus WHERE id = :clienteId")
    suspend fun atualizarStatus(clienteId: String, novoStatus: String)

    @Query("UPDATE clientes SET grupoFiltro = :novoGrupo WHERE id = :clienteId")
    suspend fun atualizarGrupo(clienteId: String, novoGrupo: String)

    @Query("DELETE FROM clientes WHERE id = :clienteId")
    suspend fun deletarPorId(clienteId: String)

    @Query("DELETE FROM clientes")
    suspend fun limparTudo()
}
