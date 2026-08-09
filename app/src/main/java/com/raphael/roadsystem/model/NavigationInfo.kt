package com.raphael.roadsystem.model

data class NavigationInfo(
    val totalDistance: String = "",
    val totalDuration: String = "",
    val nextClientName: String = "",
    val remainingStops: Int = 0,
    val isActive: Boolean = false,
    val distanceToNextManeuver: String = "",
    val nextManeuverDescription: String = "",
    val currentRoadName: String = ""
)
