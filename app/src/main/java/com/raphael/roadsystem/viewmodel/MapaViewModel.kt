package com.raphael.roadsystem.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.raphael.roadsystem.BuildConfig
import com.raphael.roadsystem.api.RetrofitClient
import com.raphael.roadsystem.data.CheckInDao
import com.raphael.roadsystem.data.CheckInHistoryDao
import com.raphael.roadsystem.data.CheckInHistoryEntity
import com.raphael.roadsystem.data.CheckInPendenteEntity
import com.raphael.roadsystem.data.ProfileRepository
import com.raphael.roadsystem.data.ClienteEntity
import com.raphael.roadsystem.data.FiltroCustomDao
import com.raphael.roadsystem.data.FiltroCustomEntity
import com.raphael.roadsystem.data.RotaAtivaDao
import com.raphael.roadsystem.data.RotaAtivaEntity
import com.raphael.roadsystem.data.SheetsRepository
import com.raphael.roadsystem.data.UserProfileEntity
import com.raphael.roadsystem.model.NavigationInfo
import com.raphael.roadsystem.model.Route
import com.raphael.roadsystem.sync.SyncCheckInWorker
import com.raphael.roadsystem.utils.PolylineDecoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MapaViewModel @Inject constructor(
    private val repository: SheetsRepository,
    private val profileRepository: ProfileRepository,
    private val checkInDao: CheckInDao,
    private val rotaAtivaDao: RotaAtivaDao,
    private val checkInHistoryDao: CheckInHistoryDao,
    private val filtroCustomDao: FiltroCustomDao,
    private val workManager: WorkManager
) : ViewModel() {

    // 1. Lista bruta do Room
    private val clientes: StateFlow<List<ClienteEntity>> = repository.listarTodosClientes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Estatísticas do Dashboard e Filtro de Histórico
    private val _dataSelecionadaIso = MutableStateFlow(
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    )
    val dataSelecionadaIso: StateFlow<String> = _dataSelecionadaIso.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totalAtendidosHoje: StateFlow<Int> = _dataSelecionadaIso.flatMapLatest { data ->
        checkInHistoryDao.countAtendimentosPorData(data)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historicoFiltrado: StateFlow<List<CheckInHistoryEntity>> = _dataSelecionadaIso.flatMapLatest { data ->
        checkInHistoryDao.getHistoricoPorData(data)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Filtros e Estados de Seleção
    private val _filtroGrupo = MutableStateFlow("Todos")
    val filtroGrupo: StateFlow<String> = _filtroGrupo.asStateFlow()

    private val _selecionados = MutableStateFlow<Set<String>>(emptySet())
    val selecionados: StateFlow<Set<String>> = _selecionados.asStateFlow()

    // 3. Grupos dinâmicos (Planilha + Custom)
    private val filtrosCustom: StateFlow<List<FiltroCustomEntity>> = filtroCustomDao.listarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gruposDisponiveis: StateFlow<List<String>> = combine(repository.listarGrupos(), filtrosCustom) { daPlanilha, custom ->
        val todos = listOf("Todos") + daPlanilha + custom.map { it.nome }
        todos.distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Todos")
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 4. Emissão da lista filtrada combinando os estados
    val clientesFiltrados: StateFlow<List<Route>> = combine(clientes, _searchQuery, _filtroGrupo, filtrosCustom) { lista, query, grupo, customFilters ->
        val customFilter = customFilters.find { it.nome == grupo }
        
        lista.map { 
            Route(it.id, it.nomeCliente, it.endereco, it.latitude, it.longitude, it.status, it.grupoFiltro) 
        }.filter { route ->
            val matchesQuery = query.isBlank() || 
                    route.clientName.contains(query, ignoreCase = true) || 
                    route.address.contains(query, ignoreCase = true)
            
            val matchesGrupo = when {
                grupo == "Todos" -> true
                customFilter != null -> {
                    val ids = customFilter.idsClientes.split(",").toSet()
                    ids.contains(route.id)
                }
                else -> route.grupoFiltro == grupo
            }
            
            matchesQuery && matchesGrupo
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Funções de Suporte Solicitadas
    fun selecionarGrupo(grupo: String) {
        _filtroGrupo.value = grupo
    }

    fun criarFiltroPersonalizado(nome: String, corHex: String) {
        val selecionadosIds = _selecionados.value
        if (selecionadosIds.isEmpty()) return
        
        viewModelScope.launch {
            val entity = FiltroCustomEntity(
                nome = nome,
                corHex = corHex,
                idsClientes = selecionadosIds.joinToString(",")
            )
            filtroCustomDao.salvar(entity)
            limparSelecao()
            selecionarGrupo(nome)
        }
    }

    fun toggleCliente(id: String) {
        _selecionados.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun toggleSelectAllVisiveis() {
        val visiveis = clientesFiltrados.value
        val visiveisIds = visiveis.map { it.id }.toSet()
        
        _selecionados.update { current ->
            if (visiveisIds.all { current.contains(it) }) {
                current - visiveisIds
            } else {
                current + visiveisIds
            }
        }
    }

    fun limparSelecao() {
        _selecionados.value = emptySet()
    }

    fun recarregarPlanilha() {
        refreshRoutes()
    }

    /**
     * Cadastro de cliente via GPS (Dashboard)
     */
    @SuppressLint("MissingPermission")
    fun cadastrarClienteViaGPS(
        nome: String,
        grupo: String,
        fusedLocationClient: FusedLocationProviderClient,
        context: android.content.Context,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val token = lastIdToken ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                if (location == null) {
                    onComplete(Result.failure(Exception("GPS indisponível")))
                    return@launch
                }

                // Geocodificação reversa para obter endereço
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val endereco = if (!addresses.isNullOrEmpty()) {
                    addresses[0].getAddressLine(0)
                } else {
                    "Endereço não identificado (${location.latitude}, ${location.longitude})"
                }

                val novoCliente = ClienteEntity(
                    id = "NEW_${System.currentTimeMillis()}",
                    nomeCliente = nome,
                    endereco = endereco,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    grupoFiltro = grupo
                )

                val result = repository.cadastrarNovoCliente(token, novoCliente)
                onComplete(result)
            } catch (e: Exception) {
                onComplete(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Outros estados para navegação e perfil ---

    private val _userProfile = MutableStateFlow<UserProfileEntity?>(null)
    val userProfile: StateFlow<UserProfileEntity?> = _userProfile.asStateFlow()

    private val _roadPolylinePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val roadPolylinePoints: StateFlow<List<LatLng>> = _roadPolylinePoints.asStateFlow()

    // IDs dos clientes que compõem a rota ativa, observando o banco de dados para persistência
    private val _listaIdsRotaAtiva = rotaAtivaDao.getIdsRotaAtiva()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Converte os IDs da rota ativa em objetos Route completos buscando da lista mestre (clientes)
    val clientesRotaAtiva: StateFlow<List<Route>> = combine(_listaIdsRotaAtiva, clientes) { ids, listaMestre ->
        ids.mapNotNull { id ->
            listaMestre.find { it.id == id }?.let {
                Route(it.id, it.nomeCliente, it.endereco, it.latitude, it.longitude, it.status, it.grupoFiltro)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clienteAtual: StateFlow<Route?> = clientesRotaAtiva
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _navInfo = MutableStateFlow(NavigationInfo())
    val navInfo: StateFlow<NavigationInfo> = _navInfo.asStateFlow()

    private val _navigationDetails = MutableStateFlow<String>("Selecione os clientes e inicie a rota")
    val navigationDetails: StateFlow<String> = _navigationDetails.asStateFlow()

    fun registrarCheckIn(routeId: String, tipo: String, latLng: LatLng? = null) {
        val now = java.util.Calendar.getInstance().time
        val dataHora = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(now)
        val dataIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(now)
        
        viewModelScope.launch {
            // Busca o cliente para salvar no histórico antes de remover da rota
            val cliente = clientes.value.find { it.id == routeId }
            cliente?.let {
                checkInHistoryDao.inserir(
                    CheckInHistoryEntity(
                        clienteId = it.id,
                        nomeCliente = it.nomeCliente,
                        dataHora = dataHora,
                        dataIso = dataIso,
                        tipo = tipo,
                        latitude = latLng?.latitude,
                        longitude = latLng?.longitude
                    )
                )
            }

            // Remove do banco de dados (a lista reativa _listaIdsRotaAtiva se atualizará sozinha)
            rotaAtivaDao.removerCliente(routeId)
            
            checkInDao.inserirCheckIn(
                CheckInPendenteEntity(
                    clienteId = routeId, 
                    dataHora = dataHora, 
                    tipo = tipo,
                    auditLat = latLng?.latitude,
                    auditLng = latLng?.longitude
                )
            )
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncCheckInWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
                
            workManager.enqueue(syncRequest)
        }
    }

    fun reordenarClientes(novaOrdemIndices: List<Int>) {
        val listaAtual = _listaIdsRotaAtiva.value
        if (listaAtual.size < 2 || novaOrdemIndices.size != listaAtual.size) return
        viewModelScope.launch {
            try {
                val listaReordenada = novaOrdemIndices.map { listaAtual[it] }
                val entidades = listaReordenada.mapIndexed { index, id -> RotaAtivaEntity(id, index) }
                rotaAtivaDao.limparRota()
                rotaAtivaDao.salvarRota(entidades)
            } catch (e: Exception) { Log.e("MapaViewModel", "Erro reordenar: ${e.message}") }
        }
    }

    fun stopNavigation() {
        _navInfo.value = NavigationInfo(isActive = false)
        _roadPolylinePoints.value = emptyList()
        _navigationDetails.value = "Navegação encerrada"
        viewModelScope.launch {
            rotaAtivaDao.limparRota()
        }
        _selecionados.value = emptySet()
    }

    private var lastIdToken: String? = null

    init {
        viewModelScope.launch {
            profileRepository.getUserProfile().collect { _userProfile.value = it }
        }
        
        // Limpeza automática de histórico com mais de 30 dias
        viewModelScope.launch {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -30)
            val dataLimite = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)
            checkInHistoryDao.limparHistoricoAntigo(dataLimite)
        }
    }

    /**
     * Sincronização com parsing seguro (A2:G)
     */
    fun refreshRoutes() {
        val token = lastIdToken ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.sincronizarDados(token)
            _isLoading.value = false
        }
    }

    fun testFetchRealRoutes(idToken: String) {
        lastIdToken = idToken
        refreshRoutes()
    }

    fun selecionarData(data: java.util.Date) {
        _dataSelecionadaIso.value = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(data)
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun getCorDoFiltro(grupo: String?): String {
        return filtrosCustom.value.find { it.nome == grupo }?.corHex ?: "#2196F3"
    }

    fun getFilteredRoutes(): List<Route> {
        val todos = clientes.value.map { 
            Route(it.id, it.nomeCliente, it.endereco, it.latitude, it.longitude, it.status, it.grupoFiltro) 
        }
        return todos.filter { _selecionados.value.contains(it.id) }
    }

    @SuppressLint("MissingPermission")
    fun calculateRoadRoute(fusedLocationClient: FusedLocationProviderClient, context: android.content.Context) {
        val selectedRoutes = getFilteredRoutes()
        val profile = _userProfile.value
        
        Log.d("MapaViewModel", "Iniciando cálculo de rota para ${selectedRoutes.size} clientes")
        
        if (profile == null || profile.address.isEmpty()) {
            val msg = "Erro: Configure seu endereço residencial no Perfil primeiro!"
            Log.w("MapaViewModel", msg)
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        
        if (selectedRoutes.isEmpty()) {
            Log.w("MapaViewModel", "Nenhuma rota selecionada")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                if (location == null) {
                    val msg = "Falha ao obter sua localização de GPS"
                    Log.e("MapaViewModel", msg)
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val origin = "${location.latitude},${location.longitude}"
                val destination = "${profile.latitude},${profile.longitude}"
                
                val waypointsList = selectedRoutes.map { "${it.latitude},${it.longitude}" }
                val waypointsParam = "optimize:true|" + waypointsList.joinToString("|")

                Log.d("MapaViewModel", "Solicitando Rota: Origin=$origin, Dest=$destination, Waypoints=${selectedRoutes.size}")
                
                val response = RetrofitClient.googleMapsInstance.getDirections(
                    origin = origin,
                    destination = destination,
                    waypoints = waypointsParam,
                    apiKey = BuildConfig.MAPS_API_KEY
                )

                if (response.status == "OK" && response.routes.isNotEmpty()) {
                    val firstRoute = response.routes[0]
                    _roadPolylinePoints.value = PolylineDecoder.decodePolyline(firstRoute.overviewPolyline.points)
                    
                    // Salvamos os IDs no BANCO DE DADOS para persistência
                    val entidades = selectedRoutes.mapIndexed { index, route -> RotaAtivaEntity(route.id, index) }
                    rotaAtivaDao.limparRota()
                    rotaAtivaDao.salvarRota(entidades)
                    
                    // Se o Google otimizou, reordenamos a lista no banco
                    firstRoute.waypointOrder?.let { reordenarClientes(it) }
                    
                    var d = 0; var t = 0
                    firstRoute.legs.forEach { d += it.distance.value; t += it.duration.value }
                    
                    val primeiroCliente = clientesRotaAtiva.value.firstOrNull()
                    _navInfo.value = NavigationInfo("%.1f km".format(d/1000.0), "%d min".format(t/60), primeiroCliente?.clientName ?: "", selectedRoutes.size, true)
                    _navigationDetails.value = "Rota: ${_navInfo.value.totalDistance} | Est.: ${_navInfo.value.totalDuration}"
                    android.widget.Toast.makeText(context, "Rota calculada!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("MapaViewModel", "Google Status: ${response.status}. Mostrando apenas marcadores.")
                    
                    // FALLBACK: Salvamos no banco para garantir que os marcadores fiquem visíveis
                    val entidades = selectedRoutes.mapIndexed { index, route -> RotaAtivaEntity(route.id, index) }
                    rotaAtivaDao.limparRota()
                    rotaAtivaDao.salvarRota(entidades)
                    
                    _roadPolylinePoints.value = emptyList()
                    _navInfo.value = NavigationInfo(isActive = true)
                    
                    val msg = if (response.status == "ZERO_RESULTS") 
                        "Não foi possível traçar rota terrestre. Mostrando locais diretos."
                        else "Erro Google: ${response.status}"
                        
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) { 
                Log.e("MapaViewModel", "Erro inesperado ao calcular rota", e) 
                android.widget.Toast.makeText(context, "Erro ao calcular rota: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                _navInfo.value = NavigationInfo(isActive = false)
            }
            finally { _isLoading.value = false }
        }
    }

    @SuppressLint("MissingPermission")
    fun navigateToBase(fusedLocationClient: FusedLocationProviderClient, context: android.content.Context) {
        val profile = _userProfile.value
        if (profile == null || profile.address.isEmpty()) {
            android.widget.Toast.makeText(context, "Endereço da base não configurado!", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                val origin = if (location != null) "${location.latitude},${location.longitude}" else ""
                val destination = "${profile.latitude},${profile.longitude}"

                // Abre o Google Maps para navegação direta à base
                val uri = android.net.Uri.parse("google.navigation:q=$destination")
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                context.startActivity(intent)
                
                // Finaliza a rota ativa no app (Mas mantém o histórico para consulta)
                stopNavigation()
                
                android.widget.Toast.makeText(context, "Navegando para a Base...", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("MapaViewModel", "Erro ao navegar para base: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
