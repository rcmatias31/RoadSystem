package com.raphael.roadsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.raphael.roadsystem.navigation.Screen
import com.raphael.roadsystem.viewmodel.MapaViewModel

@Composable
fun TelaDetalheRota(
    routeId: String,
    viewModel: MapaViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val apiRotas by viewModel.clientesFiltrados.collectAsState()
    val route = apiRotas.find { it.id == routeId }
    
    // Estados do Checklist
    var checkedEntrega by remember { mutableStateOf(false) }
    var checkedCanhoto by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    if (route == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cliente não encontrado")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Card de Informações do Cliente
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = route.clientName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = route.address, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Coordenadas: ${route.latitude}, ${route.longitude}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ITEM 1: Iniciar Navegação GPS Real
        Button(
            onClick = { 
                navController.navigate(Screen.Navegacao.route)
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar Navegação GPS")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ITEM 4: Checklist Interativo
        Text(text = "Checklist de Atendimento", style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        ListItem(
            headlineContent = { Text("Entrega Realizada") },
            trailingContent = { Checkbox(checked = checkedEntrega, onCheckedChange = { checkedEntrega = it }) }
        )
        ListItem(
            headlineContent = { Text("Canhoto Assinado") },
            trailingContent = { Checkbox(checked = checkedCanhoto, onCheckedChange = { checkedCanhoto = it }) }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Botões de Check-in
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showSuccessDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Check-in Remoto")
            }
            Button(
                onClick = { showSuccessDialog = true },
                modifier = Modifier.weight(1f),
                enabled = checkedEntrega && checkedCanhoto // Só habilita se checklist estiver ok
            ) {
                Text("Check-in Presencial")
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = { Button(onClick = { onBack() }) { Text("OK") } },
            title = { Text("Sucesso!") },
            text = { Text("Check-in realizado para ${route.clientName}") },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green) }
        )
    }
}
