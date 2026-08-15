package com.raphael.roadsystem.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raphael.roadsystem.data.SheetsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncDeleteClienteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SheetsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val clienteIdsStr = inputData.getString("CLIENTE_IDS") 
            ?: inputData.getString("CLIENTE_ID") // Retrocompatibilidade
            ?: return Result.failure()

        val ids = clienteIdsStr.split(",")
        Log.d("SyncDeleteWorker", "Iniciando exclusão remota de ${ids.size} clientes.")
        
        val result = repository.excluirClientesPlanilha(ids)
        
        return if (result.isSuccess) {
            Log.d("SyncDeleteWorker", "Exclusão concluída com sucesso.")
            Result.success()
        } else {
            Log.e("SyncDeleteWorker", "Erro na exclusão: ${result.exceptionOrNull()?.message}")
            Result.retry()
        }
    }
}
