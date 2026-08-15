package com.raphael.roadsystem

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.raphael.roadsystem.data.AppDatabase
import com.raphael.roadsystem.data.AuthRepository
import com.raphael.roadsystem.data.SheetsRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.raphael.roadsystem.ui.theme.RoadSystemTheme
import com.raphael.roadsystem.viewmodel.MapaViewModel
import com.raphael.roadsystem.viewmodel.flows.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RoadSystemTheme {
                val loginViewModel: LoginViewModel = hiltViewModel()
                val mapaViewModel: MapaViewModel = hiltViewModel()
                val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()
                
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        Log.d("RoadSystem_Auth", "Usuário logado. Buscando ID Token...")
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.getIdToken(true)?.addOnSuccessListener { result ->
                            val token = result.token
                            if (token != null) {
                                Log.d("RoadSystem_Auth", "ID Token obtido. Disparando MapaViewModel...")
                                Toast.makeText(this@MainActivity, "Sessão Ativa. Sincronizando...", Toast.LENGTH_SHORT).show()
                                mapaViewModel.testFetchRealRoutes(token)
                            } else {
                                Log.e("RoadSystem_Auth", "Falha: Token JWT veio nulo.")
                            }
                        }?.addOnFailureListener {
                            Log.e("RoadSystem_Auth", "Erro ao obter ID Token: ${it.message}")
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (isLoggedIn) {
                            MainScreen(mapaViewModel = mapaViewModel)
                        } else {
                            TelaLogin(loginViewModel)
                        }
                    }
                }
            }
        }
    }
}
