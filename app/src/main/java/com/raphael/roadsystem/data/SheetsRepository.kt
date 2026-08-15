package com.raphael.roadsystem.data

import android.util.Log
import com.raphael.roadsystem.api.AddClientRequest
import com.raphael.roadsystem.api.RoadSystemApi
import com.raphael.roadsystem.api.RouteDto
import com.raphael.roadsystem.utils.GeoUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para gerenciar dados de clientes vindos da Google Sheets (via API/Cloud Functions).
 * Implementa parsing seguro e tratamento de erros de dados incompletos.
 */
@Singleton
class SheetsRepository @Inject constructor(
    private val clienteDao: ClienteDao,
    private val api: RoadSystemApi
) {

    /**
     * Retorna todos os clientes do cache local (Room).
     */
    fun listarTodosClientes(): Flow<List<ClienteEntity>> = clienteDao.listarTodos()

    /**
     * Retorna os grupos únicos disponíveis para filtro.
     */
    fun listarGrupos(): Flow<List<String>> = clienteDao.listarGrupos()

    /**
     * Cadastra um novo cliente via API e salva localmente.
     */
    suspend fun cadastrarNovoCliente(token: String, cliente: ClienteEntity): Result<Unit> {
        return try {
            val response = api.addClient(
                token = "Bearer $token",
                request = AddClientRequest(
                    id = cliente.id,
                    clientName = cliente.nomeCliente,
                    address = cliente.endereco,
                    latitude = cliente.latitude,
                    longitude = cliente.longitude,
                    grupoFiltro = cliente.grupoFiltro
                )
            )

            if (response.success) {
                clienteDao.salvarTodos(listOf(cliente))
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message))
            }
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
            Log.d("SheetsRepository", "Iniciando sincronização (Clientes!A2:G)...")
            
            val response = api.getRoutes("Bearer $token")
            
            // Filtragem e Parsing Seguro com GeoUtils
            val entidades = response.mapNotNull { route ->
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

                ClienteEntity(
                    id = route.id,
                    nomeCliente = route.clientName,
                    endereco = route.address,
                    latitude = lat,
                    longitude = lng,
                    status = route.status?.ifBlank { "PENDENTE" } ?: "PENDENTE",
                    grupoFiltro = route.grupoFiltro?.ifBlank { "Sem Categoria" } ?: "Sem Categoria"
                )
            }

            if (entidades.isNotEmpty()) {
                // Sincronização inteligente: Não limpa tudo para não perder rota ativa
                clienteDao.salvarTodos(entidades)
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
