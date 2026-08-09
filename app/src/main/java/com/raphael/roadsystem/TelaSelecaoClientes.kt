package com.raphael.roadsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raphael.roadsystem.ui.components.ClientItemShimmer
import com.raphael.roadsystem.viewmodel.MapaViewModel

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaSelecaoClientes(
    viewModel: MapaViewModel,
    onStartRouteClick: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val allRoutes by viewModel.apiRotas.collectAsState()
    val selectedIds by viewModel.selectedRouteIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Column { repeat(10) { ClientItemShimmer() } }
        } else {
            if (allRoutes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma rota pendente. Clique em atualizar.")
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Selecione os clientes para a rota de hoje:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(allRoutes) { route ->
                        val isSelected = selectedIds.contains(route.id)
                        
                        ListItem(
                            headlineContent = { Text(route.clientName) },
                            supportingContent = { Text(route.address) },
                            leadingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleRouteSelection(route.id) }
                                )
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Botão flutuante para Refresh manual
            FloatingActionButton(
                onClick = { viewModel.refreshRoutes() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
            }

            // Botão para iniciar a rota
            if (selectedIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (profile == null || profile!!.address.isEmpty()) {
                            Toast.makeText(context, "Por favor, informe seu endereço residencial no Perfil!", Toast.LENGTH_LONG).show()
                        } else {
                            android.util.Log.d("RoadSystem_UI", "Iniciando Rota. Origem: GPS | Destino: ${profile!!.address}")
                            viewModel.calculateRoadRoute(fusedLocationClient)
                            onStartRouteClick()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    icon = { Icon(Icons.Default.Route, null) },
                    text = { Text("Iniciar Rota (${selectedIds.size})") }
                )
            }
        }
    }
}