package com.raphael.roadsystem.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirCheckIn(checkIn: CheckInPendenteEntity)

    @Query("SELECT * FROM checkins_pendentes")
    suspend fun listarTodosPendentes(): List<CheckInPendenteEntity>

    @Delete
    suspend fun deletarCheckIn(checkIn: CheckInPendenteEntity)
}
