package com.raphael.roadsystem.data

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

class CheckInRepository @Inject constructor() {
    
    /**
     * Calcula se o motorista está a menos de 150 metros do cliente.
     */
    fun isWithinRange(userLocation: LatLng, clientLocation: LatLng): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLocation.latitude, userLocation.longitude,
            clientLocation.latitude, clientLocation.longitude,
            results
        )
        return results[0] <= 150f
    }

    suspend fun registerCheckIn(routeId: String) {
        // TODO: Persistir no banco com Data/Hora atual
        // Por enquanto, apenas log ou mock
    }
}
