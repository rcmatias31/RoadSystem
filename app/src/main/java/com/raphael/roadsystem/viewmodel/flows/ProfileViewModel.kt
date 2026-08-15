package com.raphael.roadsystem.viewmodel.flows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raphael.roadsystem.data.ProfileRepository
import com.raphael.roadsystem.data.UserProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserProfileEntity?>(null)
    val uiState: StateFlow<UserProfileEntity?> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getUserProfile().collect { 
                _uiState.value = it 
            }
        }
    }

    fun saveProfile(name: String, address: String, theme: String, isGeomarkingEnabled: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.saveProfile(name, address, theme, isGeomarkingEnabled)
            onComplete(success)
        }
    }
}
