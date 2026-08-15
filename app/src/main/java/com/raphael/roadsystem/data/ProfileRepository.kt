package com.raphael.roadsystem.data

import android.content.Context
import android.location.Geocoder
import com.raphael.roadsystem.data.ProfileDao
import com.raphael.roadsystem.data.UserProfileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    @ApplicationContext private val context: Context
) {
    fun getUserProfile(): Flow<UserProfileEntity?> = profileDao.getProfile()

    suspend fun saveProfile(
        name: String,
        address: String,
        theme: String? = null,
        isGeomarkingEnabled: Boolean? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(address, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                val entity = UserProfileEntity(
                    id = 1,
                    name = name,
                    address = address,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    theme = theme ?: "SISTEMA",
                    isGeomarkingEnabled = isGeomarkingEnabled ?: false
                )
                profileDao.insertProfile(entity)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateConfig(theme: String, isGeomarkingEnabled: Boolean, currentProfile: UserProfileEntity?) = withContext(Dispatchers.IO) {
        if (currentProfile != null) {
            val updated = currentProfile.copy(theme = theme, isGeomarkingEnabled = isGeomarkingEnabled)
            profileDao.insertProfile(updated)
        }
    }
}
