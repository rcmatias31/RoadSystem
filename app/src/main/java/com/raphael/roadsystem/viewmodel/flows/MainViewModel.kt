package com.raphael.roadsystem.viewmodel.flows

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.raphael.roadsystem.data.CheckInRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val checkInRepository: CheckInRepository
) : ViewModel() {

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation = _userLocation.asStateFlow()

    var hasLocationPermission by mutableStateOf(false)

    private val _checkInStatus = MutableStateFlow<String?>(null)
    val checkInStatus = _checkInStatus.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    Log.d("RoadSystem_GPS", "Localização recebida: ${it.latitude}, ${it.longitude}")
                    _userLocation.value = LatLng(it.latitude, it.longitude)
                }
            }
        }
        Log.d("RoadSystem_GPS", "Solicitando atualizações de localização...")
        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    fun performCheckIn(routeId: String, clientLocation: LatLng) {
        val currentLoc = _userLocation.value
        if (currentLoc == null) {
            _checkInStatus.value = "Aguardando sinal GPS..."
            return
        }

        if (checkInRepository.isWithinRange(currentLoc, clientLocation)) {
            viewModelScope.launch {
                checkInRepository.registerCheckIn(routeId)
                _checkInStatus.value = "Check-in realizado com sucesso!"
            }
        } else {
            _checkInStatus.value = "Você está muito longe do cliente (fora do raio de 150m)"
        }
    }

    fun clearCheckInStatus() {
        _checkInStatus.value = null
    }
}
