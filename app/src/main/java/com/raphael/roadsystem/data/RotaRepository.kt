package com.raphael.roadsystem.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repositório responsável por mediar o acesso aos dados de rotas.
 * Segue o padrão Single Source of Truth (SSoT), utilizando o Room como cache local.
 */
class RotaRepository(private val rotaDao: RotaDao) {

    /**
     * Retorna um Flow com a lista de todas as rotas ordenadas.
     * O Flow notificará automaticamente qualquer mudança no banco de dados.
     */
    fun listarTodasRotas(): Flow<List<RotaEntity>> {
        return rotaDao.listarTodasRotas()
    }

    /**
     * Popula o banco de dados com dados fictícios para fins de teste/demonstração.
     * Utiliza coordenadas reais de pontos conhecidos no Brasil.
     */
    suspend fun popularBancoMock() {
        val rotasMock = listOf(
            RotaEntity(
                uid = UUID.randomUUID().toString(),
                nomeCliente = "Posto Ipiranga - Matriz",
                endereco = "Av. Paulista, 1000, São Paulo - SP",
                latitude = -23.561476,
                longitude = -46.655881,
                ordemVisita = 1,
                status = "PENDENTE"
            ),
            RotaEntity(
                uid = UUID.randomUUID().toString(),
                nomeCliente = "Supermercado Extra",
                endereco = "Rua da Consolação, 2500, São Paulo - SP",
                latitude = -23.555543,
                longitude = -46.662283,
                ordemVisita = 2,
                status = "PENDENTE"
            ),
            RotaEntity(
                uid = UUID.randomUUID().toString(),
                nomeCliente = "Logística Central",
                endereco = "Av. Rebouças, 500, São Paulo - SP",
                latitude = -23.567345,
                longitude = -46.678912,
                ordemVisita = 3,
                status = "PENDENTE"
            )
        )
        
        rotaDao.inserirRotas(rotasMock)
    }
}
