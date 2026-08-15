package com.raphael.roadsystem.core.di

import android.content.Context
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.raphael.roadsystem.api.RoadSystemApi
import com.raphael.roadsystem.api.RetrofitClient
import com.raphael.roadsystem.data.AppDatabase
import com.raphael.roadsystem.data.AuthRepository
import com.raphael.roadsystem.data.CheckInDao
import com.raphael.roadsystem.data.ProfileDao
import com.raphael.roadsystem.data.ClienteDao
import com.raphael.roadsystem.data.CheckInHistoryDao
import com.raphael.roadsystem.data.FiltroCustomDao
import com.raphael.roadsystem.data.RotaAtivaDao
import com.raphael.roadsystem.data.SheetsRepository
import com.google.api.services.sheets.v4.Sheets
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository {
        return AuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideClienteDao(database: AppDatabase): ClienteDao = database.clienteDao()

    @Provides
    fun provideProfileDao(database: AppDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideCheckInDao(database: AppDatabase): CheckInDao = database.checkInDao()

    @Provides
    fun provideRotaAtivaDao(database: AppDatabase): RotaAtivaDao = database.rotaAtivaDao()

    @Provides
    fun provideCheckInHistoryDao(database: AppDatabase): CheckInHistoryDao = database.checkInHistoryDao()

    @Provides
    fun provideFiltroCustomDao(database: AppDatabase): FiltroCustomDao = database.filtroCustomDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideRoadSystemApi(): RoadSystemApi = RetrofitClient.instance

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideSheetsRepository(
        clienteDao: ClienteDao, 
        api: RoadSystemApi,
        workManager: WorkManager,
        sheetsService: Sheets,
        @Named("spreadsheetId") spreadsheetId: String
    ): SheetsRepository {
        return SheetsRepository(clienteDao, api, workManager, sheetsService, spreadsheetId)
    }
}
