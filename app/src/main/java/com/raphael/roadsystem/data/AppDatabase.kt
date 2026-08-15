package com.raphael.roadsystem.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ClienteEntity::class, UserProfileEntity::class, CheckInPendenteEntity::class, RotaAtivaEntity::class, CheckInHistoryEntity::class, FiltroCustomEntity::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clienteDao(): ClienteDao
    abstract fun profileDao(): ProfileDao
    abstract fun checkInDao(): CheckInDao
    abstract fun rotaAtivaDao(): RotaAtivaDao
    abstract fun checkInHistoryDao(): CheckInHistoryDao
    abstract fun filtroCustomDao(): FiltroCustomDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Se a INSTANCE não for nula, então a retorna,
            // se for, então cria o banco de dados
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "road_system_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                // retorna a instância
                instance
            }
        }
    }
}
