package com.raphael.roadsystem.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng

object LocationUtils {

    /**
     * Calcula a distância em metros entre duas coordenadas.
     */
    fun calcularDistancia(pontoA: LatLng, pontoB: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            pontoA.latitude, pontoA.longitude,
            pontoB.latitude, pontoB.longitude,
            results
        )
        return results[0]
    }
}
