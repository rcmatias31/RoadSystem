package com.raphael.roadsystem.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raphael.roadsystem.data.ClienteDao
import com.raphael.roadsystem.data.SheetsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncNovoClienteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val clienteDao: ClienteDao,
    private val repository: SheetsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendentes = clienteDao.listarClientesPendentesEnvio()
        if (pendentes.isEmpty()) return Result.success()

        var allSuccess = true

        for (cliente in pendentes) {
            try {
                val result = repository.appendNovoClientePlanilha(cliente)

                if (result.isSuccess) {
                    clienteDao.marcarComoSincronizado(cliente.id)
                    Log.d("SyncNovoClienteWorker", "Cliente sincronizado na planilha: ${cliente.id}")
                } else {
                    allSuccess = false
                    Log.e("SyncNovoClienteWorker", "Erro ao gravar na planilha (${cliente.id}): ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                allSuccess = false
                Log.e("SyncNovoClienteWorker", "Falha de execução para ${cliente.id}: ${e.message}")
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }
}
