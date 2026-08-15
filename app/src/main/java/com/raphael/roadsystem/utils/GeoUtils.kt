package com.raphael.roadsystem.utils

import android.util.Log
import java.math.BigDecimal
import kotlin.math.abs

object GeoUtils {

    /**
     * Converte um valor (String, Double, etc) vindo da Sheets para uma coordenada válida.
     * Trata notação científica, troca vírgula por ponto e ajusta escala se necessário.
     */
    fun parseCoordenadaSegura(valor: Any?): Double? {
        if (valor == null) return null

        try {
            // 1. Limpeza básica
            val rawStr = valor.toString().trim().replace(",", ".")
            if (rawStr.isEmpty()) return null

            // 2. Parse com BigDecimal para suportar notação científica (ex: -4.79E7)
            val bd = BigDecimal(rawStr)
            var doubleVal = bd.toDouble()

            // 3. Heurística de Ajuste de Escala (Missing Decimal Point / Microdegrees)
            // Se o valor for > 1000 ou < -1000, é impossível ser Lat/Lng.
            // Exemplos do usuário: -1268022 e -4.79E7 (-47904847) indicam escala de 10^6.
            if (abs(doubleVal) > 180.0) {
                // Tentativa 1: Escala 10^6 (Muito comum em storage de GPS como inteiros)
                val tentativa10e6 = doubleVal / 1_000_000.0
                if (isPlausivelBrasil(tentativa10e6)) {
                    return tentativa10e6
                }

                // Tentativa 2: Escala 10^7 (Outro padrão comum)
                val tentativa10e7 = doubleVal / 10_000_000.0
                if (isPlausivelBrasil(tentativa10e7)) {
                    return tentativa10e7
                }

                // Fallback: Dividir por 10 até caber no limite global [-180, 180]
                while (abs(doubleVal) > 180.0) {
                    doubleVal /= 10.0
                }
            }

            return if (abs(doubleVal) <= 180.0) doubleVal else null
        } catch (e: Exception) {
            Log.e("GeoUtils", "Erro no parse da coordenada: $valor", e)
            return null
        }
    }

    /**
     * Verifica se a coordenada está dentro de um retângulo aproximado do Brasil.
     * Lat: [-35, 6], Lng: [-75, -30]
     */
    private fun isPlausivelBrasil(v: Double): Boolean {
        val a = abs(v)
        // Lat ou Lng do Brasil em valor absoluto caem entre 0 e 75.
        return a > 0.0 && a <= 75.0
    }
}
