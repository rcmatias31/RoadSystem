package com.raphael.roadsystem

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RoadSystemApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        // Inicialização crítica do Maps SDK. Deve ser feito antes de qualquer Composable carregar.
        // O renderer LATEST resolve muitos problemas de tela em branco no Android 14+
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, object : OnMapsSdkInitializedCallback {
            override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
                when (renderer) {
                    MapsInitializer.Renderer.LATEST -> Log.d("RoadSystem_Maps", "Renderer LATEST carregado com sucesso.")
                    MapsInitializer.Renderer.LEGACY -> Log.w("RoadSystem_Maps", "Renderer LEGACY carregado (LATEST falhou).")
                }
            }
        })
    }
}
