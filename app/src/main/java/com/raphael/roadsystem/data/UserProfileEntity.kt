package com.raphael.roadsystem.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val theme: String = "SISTEMA", // "LIGHT", "DARK", "SISTEMA"
    val isGeomarkingEnabled: Boolean = false
)
