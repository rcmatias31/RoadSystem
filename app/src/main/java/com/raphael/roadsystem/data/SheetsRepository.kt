package com.raphael.roadsystem.data

import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.raphael.roadsystem.api.AddClientRequest
import com.raphael.roadsystem.api.RoadSystemApi
import com.raphael.roadsystem.api.RouteDto
import com.raphael.roadsystem.sync.SyncNovoClienteWorker
import com.raphael.roadsystem.sync.SyncDeleteClienteWorker
import com.raphael.roadsystem.utils.GeoUtils
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Repositório para gerenciar dados de clientes vindos da Google Sheets (via API/Cloud Functions).
 * Implementa parsing seguro e tratamento de erros de dados incompletos.
 */
@Singleton
class SheetsRepository @Inject constructor(
    private val clienteDao: ClienteDao,
    private val api: RoadSystemApi,
    private val workManager: WorkManager,
    private val sheetsService: Sheets,
    @Named("spreadsheetId") private val spreadsheetId: String
) {

    /**
     * Insere um novo cliente diretamente na planilha via Google Sheets API.
     */
    suspend fun appendNovoClientePlanilha(cliente: ClienteEntity): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val values = listOf(
                listOf(
                    cliente.id,
                    cliente.nomeCliente,
                    cliente.endereco,
                    cliente.latitude,
                    cliente.longitude
                )
            )
            val body = ValueRange().setValues(values)
            
            sheetsService.spreadsheets().values()
                .append(spreadsheetId, "Clientes!A:E", body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute()
            
            Unit
        }
    }

    /**
     * Remove múltiplos clientes da planilha de uma vez.
     */
    suspend fun excluirClientesPlanilha(clienteIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "Clientes!A:A")
                .execute()
            
            val values = response.getValues() ?: return@runCatching Unit
            val indicesToDelete = mutableListOf<Int>()
            
            for (i in values.indices) {
                val row = values[i]
                if (row.isNotEmpty() && clienteIds.contains(row[0].toString())) {
                    indicesToDelete.add(i)
                }
            }

            if (indicesToDelete.isNotEmpty()) {
                val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
                val sheetId = spreadsheet.sheets.find { it.properties.title == "Clientes" }?.properties?.sheetId
                    ?: throw Exception("Aba 'Clientes' não encontrada")

                // Deletar de baixo para cima para não invalidar os índices das linhas acima
                val requests = indicesToDelete.sortedDescending().map { rowIndex ->
                    Request().setDeleteDimension(
                        DeleteDimensionRequest().setRange(
                            DimensionRange()
                                .setSheetId(sheetId)
                                .setDimension("ROWS")
                                .setStartIndex(rowIndex)
                                .setEndIndex(rowIndex + 1)
                        )
                    )
                }

                val batchUpdate = BatchUpdateSpreadsheetRequest().setRequests(requests)
                sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdate).execute()
                Log.d("SheetsRepository", "${indicesToDelete.size} clientes removidos da planilha.")
            }
            Unit
        }
    }

    /**
     * Agenda a exclusão de múltiplos clientes.
     */
    suspend fun agendarExclusaoClientes(clienteIds: List<String>): Result<Unit> {
        return try {
            // 1. Remove do Room
            clienteIds.forEach { clienteDao.deletarPorId(it) }

            // 2. Agenda o Worker passando os IDs separados por vírgula
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val deleteRequest = OneTimeWorkRequestBuilder<SyncDeleteClienteWorker>()
                .setConstraints(constraints)
                .setInputData(androidx.work.workDataOf("CLIENTE_IDS" to clienteIds.joinToString(",")))
                .build()

            workManager.enqueue(deleteRequest)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retorna todos os clientes do cache local (Room).
     */
    fun listarTodosClientes(): Flow<List<ClienteEntity>> = clienteDao.listarTodos()

    /**
     * Retorna os grupos únicos disponíveis para filtro.
     */
    fun listarGrupos(): Flow<List<String>> = clienteDao.listarGrupos()

    /**
     * Salva o cliente localmente e agenda a sincronização via WorkManager.
     */
    suspend fun agendarCadastroNovoCliente(cliente: ClienteEntity): Result<Unit> {
        return try {
            // 1. Salva imediatamente no Room (Offline-First)
            clienteDao.salvarTodos(listOf(cliente.copy(sincronizado = false)))

            // 2. Agenda o Worker com restrição de rede
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncNovoClienteWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueue(syncRequest)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sincroniza dados da API com o Room.
     * Mapeamento A2:G (backend) com validação local.
     */
    suspend fun sincronizarDados(token: String): Result<Unit> {
        return try {
            Log.d("SheetsRepository", "Iniciando sincronização (Clientes!A2:E)...")
            
            val response = api.getRoutes("Bearer $token")
            val clientesLocais = clienteDao.getAllCurrentSyncState().associateBy { it.id }
            
            // Filtragem e Parsing Seguro com GeoUtils
            val entidadesMescladas = response.mapNotNull { route ->
                val lat = GeoUtils.parseCoordenadaSegura(route.latitude)
                val lng = GeoUtils.parseCoordenadaSegura(route.longitude)

                // Validação de campos essenciais (ID, Nome, Coordenadas Válidas)
                if (route.id.isBlank() || route.clientName.isBlank() || lat == null || lng == null) {
                    Log.w("SheetsRepository", "Linha descartada por dados incompletos ou coordenadas inválidas: ID=${route.id}")
                    return@mapNotNull null
                }

                // Validação específica para Latitude [-90, 90]
                if (lat < -90.0 || lat > 90.0) {
                    Log.w("SheetsRepository", "Latitude inválida ignorada: $lat (ID=${route.id})")
                    return@mapNotNull null
                }

                val localExistente = clientesLocais[route.id]

                ClienteEntity(
                    id = route.id,
                    nomeCliente = route.clientName,
                    endereco = route.address,
                    latitude = lat,
                    longitude = lng,
                    status = localExistente?.status ?: "PENDENTE",
                    grupoFiltro = localExistente?.grupoFiltro ?: "Sem Categoria"
                )
            }

            if (entidadesMescladas.isNotEmpty()) {
                // Sincronização inteligente: Não limpa tudo para não perder rota ativa
                clienteDao.salvarTodos(entidadesMescladas)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Nenhum dado válido recebido da planilha."))
            }
        } catch (e: Exception) {
            Log.e("SheetsRepository", "Erro na sincronização", e)
            Result.failure(e)
        }
    }
}
