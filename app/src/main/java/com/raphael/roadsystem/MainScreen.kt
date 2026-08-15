package com.raphael.roadsystem

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import com.raphael.roadsystem.model.Route
import com.raphael.roadsystem.navigation.Screen
import com.raphael.roadsystem.ui.components.NavHeader
import com.raphael.roadsystem.utils.LocationUtils
import com.raphael.roadsystem.viewmodel.MapaViewModel
import com.raphael.roadsystem.viewmodel.flows.MainViewModel
import kotlinx.coroutines.launch

private fun hueFromColor(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
        hsv
    )
    return hsv[0]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    mapaViewModel: MapaViewModel
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val user = remember { FirebaseAuth.getInstance().currentUser }
    
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    // Estados para Cadastro de Cliente (Movidos para a TopBar)
    val context = LocalContext.current
    val grupos by mapaViewModel.gruposDisponiveis.collectAsStateWithLifecycle()
    var showAddClientDialog by remember { mutableStateOf(false) }
    var newClientName by remember { mutableStateOf("") }
    var selectedGrupo by remember { mutableStateOf("Sem Categoria") }
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    if (showAddClientDialog) {
        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = { Text("Cadastrar Novo Cliente") },
            text = {
                Column {
                    Text("O cliente será cadastrado na sua localização atual.")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newClientName,
                        onValueChange = { newClientName = it },
                        label = { Text("Nome do Cliente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Categoria:")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        var expanded by remember { mutableStateOf(false) }
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedGrupo)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            grupos.filter { it != "Todos" }.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g) },
                                    onClick = { selectedGrupo = g; expanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    mapaViewModel.cadastrarClienteViaGPS(newClientName, selectedGrupo, fusedLocationClient, context) { result ->
                        if (result.isSuccess) {
                            Toast.makeText(context, "Cliente salvo localmente! Sincronização em fila.", Toast.LENGTH_SHORT).show()
                            showAddClientDialog = false
                            newClientName = ""
                        } else {
                            Toast.makeText(context, "Erro: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }, enabled = newClientName.isNotBlank()) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Permissões de GPS
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        mainViewModel.hasLocationPermission = granted
        if (granted) {
            mainViewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                NavHeader(user = user)
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("Rotas") },
                    selected = currentRoute == Screen.Selecao.route,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Selecao.route) {
                            popUpTo(Screen.Mapa.route)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.List, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    selected = currentRoute == Screen.Perfil.route,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Perfil.route) {
                            popUpTo(Screen.Mapa.route)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Histórico") },
                    selected = currentRoute == Screen.Historico.route,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Historico.route) {
                            popUpTo(Screen.Mapa.route)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.History, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        val title = when (currentRoute) {
                            Screen.Mapa.route -> "Mapa Geral"
                            Screen.Selecao.route -> "Rota do Dia"
                            Screen.Perfil.route -> "Meu Perfil"
                            Screen.Historico.route -> "Histórico"
                            else -> "RoadSystem"
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (currentRoute == Screen.Mapa.route) {
                                scope.launch { drawerState.open() }
                            } else {
                                navController.popBackStack()
                            }
                        }) {
                            val icon = if (currentRoute == Screen.Mapa.route) {
                                Icons.Default.Menu
                            } else {
                                Icons.AutoMirrored.Filled.ArrowBack
                            }
                            Icon(icon, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (currentRoute == Screen.Mapa.route) {
                            IconButton(onClick = { showAddClientDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Novo Cliente")
                            }
                        }
                        AsyncImage(
                            model = user?.photoUrl,
                            contentDescription = "Foto de Perfil",
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    },
                    windowInsets = WindowInsets.statusBars
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize()
            ) {
                NavHost(navController = navController, startDestination = Screen.Mapa.route) {
                    composable(Screen.Mapa.route) {
                        MapScreen(mainViewModel, mapaViewModel)
                    }
                    composable(Screen.Selecao.route) {
                        TelaSelecaoClientes(
                            viewModel = mapaViewModel,
                            onStartRouteClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(Screen.Perfil.route) {
                        TelaPerfil(onSaveSuccess = {
                            navController.popBackStack()
                        })
                    }
                    composable(Screen.Historico.route) {
                        TelaHistorico(mapaViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MapScreen(mainViewModel: MainViewModel, mapaViewModel: MapaViewModel) {
    val context = LocalContext.current
    val userLocation by mainViewModel.userLocation.collectAsStateWithLifecycle()
    val polylinePoints by mapaViewModel.roadPolylinePoints.collectAsStateWithLifecycle()
    val navInfo by mapaViewModel.navInfo.collectAsStateWithLifecycle()
    val clienteAtual by mapaViewModel.clienteAtual.collectAsStateWithLifecycle()
    val clientesRotaAtiva by mapaViewModel.clientesRotaAtiva.collectAsStateWithLifecycle()
    val totalAtendidos by mapaViewModel.totalAtendidosHoje.collectAsStateWithLifecycle()
    val isLoading by mapaViewModel.isLoading.collectAsStateWithLifecycle()
    val userProfile by mapaViewModel.userProfile.collectAsStateWithLifecycle()
    
    val cameraPositionState = rememberCameraPositionState()
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            if (cameraPositionState.position.target.latitude == 0.0) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        key(mainViewModel.hasLocationPermission) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = mainViewModel.hasLocationPermission,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true
                )
            ) {
                if (polylinePoints.isNotEmpty()) {
                    Polyline(points = polylinePoints, color = Color.Blue, width = 12f)
                }

                clientesRotaAtiva.forEach { route ->
                    val colorHex = if (userProfile?.isGeomarkingEnabled == true) {
                        mapaViewModel.getCorDoFiltro(route.grupoFiltro) 
                    } else {
                        "#2196F3"
                    }
                    
                    key(route.id) {
                        Marker(
                            state = rememberMarkerState(position = LatLng(route.latitude, route.longitude)),
                            title = route.clientName,
                            snippet = "Grupo: ${route.grupoFiltro ?: "Sem Categoria"}",
                            icon = BitmapDescriptorFactory.defaultMarker(hueFromColor(Color(colorHex.toColorInt())))
                        )
                    }
                }
            }
        }

        if ((userLocation == null && mainViewModel.hasLocationPermission) || isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (navInfo.isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                PainelControleViagem(
                    cliente = clienteAtual,
                    userLocation = userLocation,
                    totalConcluido = totalAtendidos,
                    mapaViewModel = mapaViewModel,
                    onNavegar = {
                        clienteAtual?.let {
                            val uri = Uri.parse("google.navigation:q=${it.latitude},${it.longitude}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    },
                    onCheckIn = { tipo ->
                        clienteAtual?.let {
                            mapaViewModel.registrarCheckIn(it.id, tipo, userLocation)
                            Toast.makeText(context, "Check-in $tipo realizado!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFinalizar = {
                        mapaViewModel.navigateToBase(fusedLocationClient, context)
                    }
                )
            }
        }
    }
}

@Composable
fun PainelControleViagem(
    cliente: Route?,
    userLocation: LatLng?,
    totalConcluido: Int,
    mapaViewModel: MapaViewModel,
    onNavegar: () -> Unit,
    onCheckIn: (String) -> Unit,
    onFinalizar: () -> Unit
) {
    var showRemotoDialog by remember { mutableStateOf(false) }
    var lastDistanciaCalculada by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    if (showRemotoDialog) {
        AlertDialog(
            onDismissRequest = { showRemotoDialog = false },
            title = { Text("Aviso de Distância") },
            text = { Text("Você está a ${LocationUtils.formatarDistancia(lastDistanciaCalculada)} do destino. Deseja registrar este atendimento como REMOTO?") },
            confirmButton = {
                Button(onClick = {
                    onCheckIn("REMOTO")
                    showRemotoDialog = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemotoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val isSmallScreen = maxWidth < 360.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            AnimatedContent(
                targetState = cliente,
                transitionSpec = {
                    if (targetState != null && initialState != null) {
                        (slideInHorizontally { width -> width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        fadeIn().togetherWith(fadeOut())
                    }
                },
                label = "PainelViagemAnimation"
            ) { targetCliente ->
                val distancia = remember(targetCliente, userLocation) {
                    if (targetCliente != null && userLocation != null) {
                        LocationUtils.calcularDistancia(userLocation, LatLng(targetCliente.latitude, targetCliente.longitude))
                    } else {
                        Float.MAX_VALUE
                    }
                }
                
                SideEffect { lastDistanciaCalculada = distancia }

                Column(modifier = Modifier.padding(if (isSmallScreen) 12.dp else 16.dp)) {
                    if (targetCliente != null) {
                        val filterName = targetCliente.grupoFiltro ?: "Sem Categoria"
                        val filterColor = Color(mapaViewModel.getCorDoFiltro(filterName).toColorInt())

                        Text(
                            text = "Próximo Cliente:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = targetCliente.clientName, 
                            style = if (isSmallScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = filterName,
                            style = MaterialTheme.typography.labelMedium,
                            color = filterColor,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        Text(
                            text = targetCliente.address, 
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Distância: ${LocationUtils.formatarDistancia(distancia)}",
                            style = if (isSmallScreen) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            color = if (distancia <= 150) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavegar,
                                modifier = Modifier.weight(1f),
                                contentPadding = if (isSmallScreen) PaddingValues(horizontal = 8.dp) else ButtonDefaults.ContentPadding
                            ) {
                                Text("Navegar", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    if (distancia <= 150) {
                                        onCheckIn("PRESENCIAL")
                                    } else {
                                        showRemotoDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (distancia <= 150) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = if (isSmallScreen) PaddingValues(horizontal = 8.dp) else ButtonDefaults.ContentPadding
                            ) {
                                Text("Check-in", maxLines = 1)
                            }
                        }
                    } else {
                        Text(
                            text = "Jornada Concluída!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Total de atendimentos hoje: $totalConcluido",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onFinalizar,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Home, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retornar para a Base")
                        }
                    }
                }
            }
        }
    }
}
