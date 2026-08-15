package com.raphael.roadsystem

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
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
                if (polylinePoints.isNotEmpty() || clientesRotaAtiva.isNotEmpty()) {
                    if (polylinePoints.isNotEmpty()) {
                        Polyline(
                            points = polylinePoints,
                            color = Color.Blue,
                            width = 12f
                        )
                    }
                    
                    clientesRotaAtiva.forEach { route ->
                        key(route.id) {
                            Marker(
                                state = rememberMarkerState(position = LatLng(route.latitude, route.longitude)),
                                title = route.clientName,
                                snippet = route.address
                            )
                        }
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
            text = { Text("Você está a ${lastDistanciaCalculada.toInt()} metros do destino. Deseja registrar este atendimento como REMOTO?") },
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
                            text = targetCliente.address, 
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Distância: ${distancia.toInt()} metros",
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
