package com.raphael.roadsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raphael.roadsystem.viewmodel.MapaViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaHistorico(viewModel: MapaViewModel) {
    val historico by viewModel.historicoFiltrado.collectAsState()
    val dataSelecionadaIso by viewModel.dataSelecionadaIso.collectAsState()
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.selecionarData(Date(it))
                    }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabeçalho de Filtro
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Data da Consulta", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatarDataParaExibicao(dataSelecionadaIso),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Selecionar Data")
                }
            }
        }

        if (historico.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Nenhum registro encontrado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historico) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        ListItem(
                            headlineContent = { Text(item.nomeCliente, fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                Column {
                                    Text("Horário: ${item.dataHora.substringAfter(" ")}")
                                    Text("Tipo: ${item.tipo}", color = if (item.tipo == "PRESENCIAL") Color(0xFF4CAF50) else Color(0xFFFFA500))
                                }
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (item.tipo == "PRESENCIAL") Color(0xFF4CAF50) else Color(0xFFFFA500)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatarDataParaExibicao(dataIso: String): String {
    return try {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dataIso)
        java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy", java.util.Locale.getDefault()).format(date!!)
    } catch (e: Exception) {
        dataIso
    }
}
