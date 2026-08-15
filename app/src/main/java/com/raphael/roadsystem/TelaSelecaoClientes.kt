package com.raphael.roadsystem

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.raphael.roadsystem.ui.components.ClientItemShimmer
import com.raphael.roadsystem.viewmodel.MapaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaSelecaoClientes(
    viewModel: MapaViewModel,
    onStartRouteClick: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Estados do ViewModel conforme solicitado
    val clientesFiltrados by viewModel.clientesFiltrados.collectAsState()
    val selecionados by viewModel.selecionados.collectAsState()
    val filtroGrupo by viewModel.filtroGrupo.collectAsState()
    val gruposDisponiveis by viewModel.gruposDisponiveis.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val selecionadosCount = selecionados.size
    val isAllFilteredSelected = clientesFiltrados.isNotEmpty() && clientesFiltrados.all { selecionados.contains(it.id) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Clientes ($selecionadosCount marcados)", style = MaterialTheme.typography.titleMedium)
                            if (selecionadosCount > 23) {
                                Text(
                                    "Limite Google Maps (23) excedido!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.recarregarPlanilha() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recarregar")
                        }
                        IconButton(onClick = { viewModel.limparSelecao() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Limpar Seleção")
                        }
                        Checkbox(
                            checked = isAllFilteredSelected,
                            onCheckedChange = { viewModel.toggleSelectAllVisiveis() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                )
                
                // Barra de Pesquisa
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Buscar cliente ou endereço...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                // ITEM 1.3: FilterChips Dinâmicos
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gruposDisponiveis) { grupo ->
                        FilterChip(
                            selected = filtroGrupo == grupo,
                            onClick = { viewModel.selecionarGrupo(grupo) },
                            label = { Text(grupo) },
                            leadingIcon = if (filtroGrupo == grupo) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (selecionadosCount > 0) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            if (profile == null || profile!!.address.isEmpty()) {
                                Toast.makeText(context, "Configure seu endereço residencial no Perfil!", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.calculateRoadRoute(fusedLocationClient, context)
                                onStartRouteClick()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        enabled = selecionadosCount in 1..23,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Route, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gerar Rota com $selecionadosCount Paradas")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                Column { repeat(8) { ClientItemShimmer() } }
            } else if (clientesFiltrados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isEmpty()) "Nenhum cliente disponível." else "Nenhum resultado para \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(clientesFiltrados, key = { it.id }) { route ->
                        val isSelected = selecionados.contains(route.id)
                        
                        ListItem(
                            headlineContent = { Text(route.clientName, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { 
                                Column {
                                    Text(route.address, style = MaterialTheme.typography.bodySmall)
                                    route.grupoFiltro?.let {
                                        Text(
                                            text = "Grupo: $it",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            },
                            leadingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleCliente(route.id) }
                                )
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
