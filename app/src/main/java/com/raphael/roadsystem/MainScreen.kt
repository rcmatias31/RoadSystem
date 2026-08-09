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
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        val title = when (currentRoute) {
                            Screen.Mapa.route -> "Mapa Geral"
                            Screen.Selecao.route -> "Rota do Dia"
                            Screen.Perfil.route -> "Meu Perfil"
                            else -> "RoadSystem"
                        }
                        Text(title) 
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
                                .padding(end = 16.dp)
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
    val cameraPositionState = rememberCameraPositionState()

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
                    Polyline(
                        points = polylinePoints,
                        color = Color.Blue,
                        width = 12f
                    )
                    
                    mapaViewModel.getFilteredRoutes().forEach { route ->
                        Marker(
                            state = rememberMarkerState(position = LatLng(route.latitude, route.longitude)),
                            title = route.clientName
                        )
                    }
                }
            }
        }

        if (userLocation == null && mainViewModel.hasLocationPermission) {
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
                    onNavegar = {
                        clienteAtual?.let {
                            val uri = Uri.parse("google.navigation:q=${it.latitude},${it.longitude}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    },
                    onCheckIn = { tipo ->
                        clienteAtual?.let {
                            mapaViewModel.registrarCheckIn(it.id, tipo)
                            Toast.makeText(context, "Check-in $tipo realizado!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFinalizar = {
                        // Lógica de Retorno à Base (Passo 5/7/8)
                        val profile = mapaViewModel.userProfile.value
                        if (profile != null) {
                            val uri = Uri.parse("google.navigation:q=${profile.latitude},${profile.longitude}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                            Toast.makeText(context, "Navegando para a Base...", Toast.LENGTH_LONG).show()
                        }
                        mapaViewModel.stopNavigation()
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AnimatedContent(
            targetState = cliente,
            transitionSpec = {
                if (targetState != null && initialState != null) {
                    // Transição entre clientes: Slide horizontal (Entra pela direita, sai pela esquerda)
                    (slideInHorizontally { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    // Transição para o fim da rota ou início: Fade simples
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
            
            // Atualiza o estado para o Dialog ter o valor correto no momento do clique
            SideEffect { lastDistanciaCalculada = distancia }

            Column(modifier = Modifier.padding(16.dp)) {
                if (targetCliente != null) {
                    Text(
                        text = "Próximo Cliente:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = targetCliente.clientName, style = MaterialTheme.typography.titleLarge)
                    Text(text = targetCliente.address, style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Distância: ${distancia.toInt()} metros",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (distancia <= 150) Color.Green else MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavegar,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Navegar")
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
                            )
                        ) {
                            Text("Check-in")
                        }
                    }
                } else {
                    Text(
                        text = "Todas as entregas concluídas",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = onFinalizar,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retornar para a Base")
                    }
                }
            }
        }
    }
}
