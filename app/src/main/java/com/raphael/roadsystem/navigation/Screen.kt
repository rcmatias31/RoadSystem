package com.raphael.roadsystem.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Mapa : Screen("mapa", "Mapa Geral", Icons.Default.Map)
    object Selecao : Screen("selecao", "Rota do Dia", Icons.Default.List)
    object Perfil : Screen("perfil", "Meu Perfil", Icons.Default.Person)
    
    object Detalhe : Screen("detalhe/{routeId}", "Detalhes da Entrega", Icons.Default.Map) {
        fun createRoute(routeId: String) = "detalhe/$routeId"
    }

    object Navegacao : Screen("navegacao", "Navegação GPS", Icons.Default.Place)
}
