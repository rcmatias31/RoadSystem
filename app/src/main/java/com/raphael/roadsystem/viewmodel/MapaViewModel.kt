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
import com.raphael.roadsystem.api.CheckInRequest
import com.raphael.roadsystem.data.CheckInDao
import com.raphael.roadsystem.data.CheckInPendenteEntity
import com.raphael.roadsystem.data.ProfileRepository
import com.raphael.roadsystem.data.RotaEntity
import com.raphael.roadsystem.data.RotaRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MapaViewModel @Inject constructor(
    private val repository: RotaRepository,
    private val profileRepository: ProfileRepository,
    private val checkInDao: CheckInDao,
    private val workManager: WorkManager
) : ViewModel() {

    /**
     * StateFlow que expõe a lista de rotas para a UI.
     * O uso de stateIn converte o Flow do repositório em um StateFlow,
     * garantindo que os dados sejam mantidos durante mudanças de configuração.
     */
    val rotas: StateFlow<List<RotaEntity>> = repository.listarTodasRotas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Estado para armazenar as rotas vindas da API Cloud Function
    private val _apiRotas = MutableStateFlow<List<Route>>(emptyList())
    val apiRotas: StateFlow<List<Route>> = _apiRotas.asStateFlow()

    // ITEM 3: IDs dos clientes selecionados para a Rota do Dia
    private val _selectedRouteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedRouteIds: StateFlow<Set<String>> = _selectedRouteIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ITEM: Perfil do Motorista (Casa como Origem/Destino)
    private val _userProfile = MutableStateFlow<UserProfileEntity?>(null)
    val userProfile: StateFlow<UserProfileEntity?> = _userProfile.asStateFlow()

    // ITEM: Estado de Navegação Real (Directions API)
    private val _roadPolylinePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val roadPolylinePoints: StateFlow<List<LatLng>> = _roadPolylinePoints.asStateFlow()

    // Fila de clientes pendentes para a viagem atual
    private val _clientesPendentes = MutableStateFlow<List<Route>>(emptyList())
    val clientesPendentes: StateFlow<List<Route>> = _clientesPendentes.asStateFlow()

    // Variável computada para o cliente que deve ser atendido agora
    val clienteAtual: StateFlow<Route?> = _clientesPendentes
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _navInfo = MutableStateFlow(NavigationInfo())
    val navInfo: StateFlow<NavigationInfo> = _navInfo.asStateFlow()

    private val _navigationDetails = MutableStateFlow<String>("Selecione os clientes e inicie a rota")
    val navigationDetails: StateFlow<String> = _navigationDetails.asStateFlow()

    /**
     * Registra o check-in do cliente atual localmente e agenda a sincronização.
     */
    fun registrarCheckIn(routeId: String, tipo: String) {
        val dataHora = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        
        // Atualização Otimista da UI
        _clientesPendentes.update { lista ->
            lista.filter { it.id != routeId }
        }

        // Salva no banco de dados local (Offline-First)
        viewModelScope.launch {
            checkInDao.inserirCheckIn(
                CheckInPendenteEntity(
                    clienteId = routeId,
                    dataHora = dataHora,
                    tipo = tipo
                )
            )

            // Agenda o Worker para sincronizar quando houver internet
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncCheckInWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueue(syncRequest)
            
            Log.d("RoadSystem_CheckIn", "Check-in $tipo salvo localmente. Sincronização agendada.")
        }
    }

    /**
     * Reordena a fila de clientes baseada na otimização da Directions API.
     */
    fun reordenarClientes(novaOrdemIndices: List<Int>) {
        val listaAtual = _clientesPendentes.value
        if (listaAtual.size < 2 || novaOrdemIndices.size != listaAtual.size) return

        try {
            val listaReordenada = novaOrdemIndices.map { index ->
                listaAtual[index]
            }
            _clientesPendentes.value = listaReordenada
            Log.d("RoadSystem_Nav", "Fila de clientes sincronizada com a rota otimizada.")
        } catch (e: Exception) {
            Log.e("RoadSystem_Nav", "Erro ao reordenar clientes: ${e.message}")
        }
    }

    /**
     * Encerra a sessão de navegação no ViewModel.
     */
    fun stopNavigation() {
        _navInfo.value = NavigationInfo(isActive = false)
        _roadPolylinePoints.value = emptyList()
        _navigationDetails.value = "Navegação encerrada"
        _selectedRouteIds.value = emptySet() // Limpa seleção ao encerrar
    }

    // Token armazenado para permitir o Refresh automático
    private var lastIdToken: String? = null

    init {
        // Ao iniciar o ViewModel, populamos o banco com dados mock
        // para facilitar o desenvolvimento e testes.
        viewModelScope.launch {
            repository.popularBancoMock()
        }
        viewModelScope.launch {
            profileRepository.getUserProfile().collect {
                _userProfile.value = it
            }
        }
    }

    /**
     * Função Sênior de teste para validar o consumo real dos dados da planilha via API.
     */
    fun testFetchRealRoutes(idToken: String) {
        lastIdToken = idToken // Salva para o Pull-to-Refresh
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val tokenHeader = "Bearer $idToken"
                Log.d("RoadSystem_API", "Iniciando chamada Retrofit para buscar dados reais da planilha...")
                
                val routes = RetrofitClient.instance.getRoutes(tokenHeader)

                // Log de sucesso com contagem total
                Log.d("RoadSystem_API", "Planilha lida com sucesso! Recebidas ${routes.size} rotas.")
                routes.forEach { Log.d("RoadSystem_API", "Rota: ${it.clientName}") }

                // Atualiza o estado para que o mapa reflita os dados reais
                _apiRotas.value = routes

            } catch (e: Exception) {
                Log.e("RoadSystem_API", "ERRO AO BUSCAR ROTAS: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Alterna a seleção de um cliente para a rota do dia.
     */
    fun toggleRouteSelection(routeId: String) {
        val currentSelected = _selectedRouteIds.value.toMutableSet()
        if (currentSelected.contains(routeId)) {
            currentSelected.remove(routeId)
        } else {
            currentSelected.add(routeId)
        }
        _selectedRouteIds.value = currentSelected
    }

    /**
     * ITEM 6: Função de Refresh (disparada pelo gesto de puxar).
     */
    fun refreshRoutes() {
        lastIdToken?.let { testFetchRealRoutes(it) }
    }

    /**
     * Retorna apenas os clientes selecionados pelo motorista.
     */
    fun getFilteredRoutes(): List<Route> {
        return _apiRotas.value.filter { _selectedRouteIds.value.contains(it.id) }
    }

    /**
     * Calcula a rota real por ruas usando a Directions API.
     * Agora usa a localização atual como ORIGEM e o endereço de casa como DESTINO.
     */
    @SuppressLint("MissingPermission")
    fun calculateRoadRoute(fusedLocationClient: FusedLocationProviderClient) {
        val selectedRoutes = getFilteredRoutes()
        val profile = _userProfile.value
        
        Log.d("RoadSystem_Map", "Iniciando calculateRoadRoute real...")

        if (profile == null || profile.address.isEmpty()) {
            Log.e("RoadSystem_Map", "Erro: Endereço de casa não configurado no Perfil.")
            _navigationDetails.value = "Configure seu endereço de casa no Perfil!"
            return
        }

        if (selectedRoutes.isEmpty()) {
            Log.e("RoadSystem_Map", "Erro: Nenhum cliente selecionado.")
            _navigationDetails.value = "Selecione clientes na Rota do Dia!"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _navigationDetails.value = "Buscando sua localização e calculando trajeto..."
                
                // 1. Obter Localização Atual (Origem)
                val locationTask = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                )
                val location = locationTask.await()
                
                if (location == null) {
                    Log.e("RoadSystem_Map", "Não foi possível obter a localização GPS.")
                    _navigationDetails.value = "Falha ao obter sinal de GPS."
                    return@launch
                }
                
                val origin = "${location.latitude},${location.longitude}"

                // 2. Geocodificar Endereço de Casa (Destino)
                // Já temos as coordenadas no perfil! Passo 5 cumprido.
                val destination = "${profile.latitude},${profile.longitude}"
                
                // 3. Formatar Waypoints (Clientes)
                val waypoints = "optimize:true|" + selectedRoutes.joinToString("|") { 
                    "${it.latitude},${it.longitude}" 
                }

                Log.d("RoadSystem_Map", "Chamando Directions API...")
                val response = RetrofitClient.googleMapsInstance.getDirections(
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    apiKey = BuildConfig.MAPS_API_KEY
                )

                Log.d("RoadSystem_Map", "Resposta Directions: ${response.status}")

                if (response.status == "OK" && response.routes.isNotEmpty()) {
                    val firstRoute = response.routes[0]
                    
                    // Decodifica os pontos para desenhar no mapa
                    val points = PolylineDecoder.decodePolyline(firstRoute.overviewPolyline.points)
                    _roadPolylinePoints.value = points

                    // Inicializa a fila de clientes pendentes
                    _clientesPendentes.value = selectedRoutes

                    // Sincroniza a ordem lógica com a otimização visual da API
                    firstRoute.waypointOrder?.let { reordenarClientes(it) }
                    
                    // Calcula total de distância e tempo
                    var totalDistValue = 0
                    var totalTimeValue = 0
                    firstRoute.legs.forEach { leg ->
                        totalDistValue += leg.distance.value
                        totalTimeValue += leg.duration.value
                    }
                    
                    val distanceText = "%.1f km".format(totalDistValue / 1000.0)
                    val durationText = "%d min".format(totalTimeValue / 60)
                    
                    // Pega o nome do primeiro cliente da rota otimizada
                    // (O Google otimiza a ordem, então pegamos do primeiro 'leg' que não seja a saída da casa)
                    val nextStop = if (selectedRoutes.isNotEmpty()) selectedRoutes[0].clientName else "Destino Final"

                    _navInfo.value = NavigationInfo(
                        totalDistance = distanceText,
                        totalDuration = durationText,
                        nextClientName = nextStop,
                        remainingStops = selectedRoutes.size,
                        isActive = true
                    )
                    
                    _navigationDetails.value = "Rota: $distanceText | Est.: $durationText"
                    Log.d("RoadSystem_Map", "Sucesso: Rota de $distanceText calculada.")
                } else {
                    _navigationDetails.value = "Google negou a rota: ${response.status}"
                }

            } catch (e: Exception) {
                Log.e("RoadSystem_Map", "Erro fatal: ${e.message}")
                _navigationDetails.value = "Falha técnica na navegação."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
