package com.raphael.roadsystem.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInHistoryDao {
    @Query("SELECT * FROM checkin_history WHERE dataIso = :dataIso ORDER BY id DESC")
    fun getHistoricoPorData(dataIso: String): Flow<List<CheckInHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(item: CheckInHistoryEntity)

    @Query("SELECT COUNT(*) FROM checkin_history WHERE dataIso = :dataIso")
    fun countAtendimentosPorData(dataIso: String): Flow<Int>

    @Query("DELETE FROM checkin_history WHERE dataIso < :dataLimiteIso")
    suspend fun limparHistoricoAntigo(dataLimiteIso: String)

    @Query("DELETE FROM checkin_history")
    suspend fun limparTudo()
}
