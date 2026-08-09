package com.raphael.roadsystem.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val homeLatitude: Double? = null,
    val homeLongitude: Double? = null,
    val homeAddress: String = ""
)
