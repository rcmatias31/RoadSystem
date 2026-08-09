package com.raphael.roadsystem.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raphael.roadsystem.api.CheckInRequest
import com.raphael.roadsystem.api.RoadSystemApi
import com.raphael.roadsystem.data.AuthRepository
import com.raphael.roadsystem.data.CheckInDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncCheckInWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkInDao: CheckInDao,
    private val api: RoadSystemApi,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val checkIns = checkInDao.listarTodosPendentes()
        if (checkIns.isEmpty()) return Result.success()

        val token = authRepository.getFreshToken()
        if (token == null) {
            Log.e("SyncWorker", "Não foi possível obter um token válido. Tentando novamente mais tarde.")
            return Result.retry()
        }

        Log.d("SyncWorker", "Iniciando sincronização de ${checkIns.size} check-ins.")

        var allSuccess = true

        for (checkIn in checkIns) {
            try {
                val response = api.registerCheckIn(
                    token = "Bearer $token",
                    request = CheckInRequest(
                        routeId = checkIn.clienteId,
                        timestamp = checkIn.dataHora,
                        type = checkIn.tipo
                    )
                )

                if (response.success) {
                    checkInDao.deletarCheckIn(checkIn)
                    Log.d("SyncWorker", "Check-in sincronizado com sucesso: ${checkIn.clienteId}")
                } else {
                    allSuccess = false
                    Log.e("SyncWorker", "Erro ao sincronizar check-in: ${response.message}")
                }
            } catch (e: Exception) {
                allSuccess = false
                Log.e("SyncWorker", "Falha de rede ao sincronizar: ${e.message}")
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }
}
