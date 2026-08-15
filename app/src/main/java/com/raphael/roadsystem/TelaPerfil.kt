package com.raphael.roadsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.raphael.roadsystem.viewmodel.flows.ProfileViewModel

@Composable
fun TelaPerfil(
    viewModel: ProfileViewModel = hiltViewModel(),
    onSaveSuccess: () -> Unit = {}
) {
    val profile by viewModel.uiState.collectAsState()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(firebaseUser?.email ?: "") }
    var homeAddress by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("SISTEMA") }
    var isGeomarkingEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            homeAddress = it.address
            theme = it.theme
            isGeomarkingEnabled = it.isGeomarkingEnabled
        } ?: run {
            name = firebaseUser?.displayName ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Meu Perfil", style = MaterialTheme.typography.headlineMedium)
        Text("Gerencie suas informações e preferências do aplicativo.", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome Completo") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Preferências", style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(16.dp))

        // Seleção de Tema
        Text("Tema do Aplicativo", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("LIGHT", "DARK", "SISTEMA").forEach { t ->
                FilterChip(
                    selected = theme == t,
                    onClick = { theme = t },
                    label = { Text(t) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Geomarcação
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Habilitar geomarcação por filtro", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Exibe marcadores coloridos no mapa de acordo com a categoria/grupo.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = isGeomarkingEnabled,
                onCheckedChange = { isGeomarkingEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Endereço de Casa (Destino Final)", style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = homeAddress,
            onValueChange = { homeAddress = it },
            label = { Text("Endereço residencial") },
            placeholder = { Text("Rua, número, cidade e estado") },
            leadingIcon = { Icon(Icons.Default.Home, null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.saveProfile(name, homeAddress, theme, isGeomarkingEnabled) { success ->
                    if (success) onSaveSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salvar Perfil")
        }
    }
}
