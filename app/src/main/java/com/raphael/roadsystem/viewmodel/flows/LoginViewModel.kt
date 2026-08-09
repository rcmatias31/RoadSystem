package com.raphael.roadsystem.viewmodel.flows

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.raphael.roadsystem.BuildConfig
import com.raphael.roadsystem.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(authRepository.getCurrentUser() != null)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun signIn(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signInWithGoogle(
                context = context,
                webClientId = BuildConfig.ClienteID
            )
            if (result.isSuccess) {
                _isLoggedIn.value = true
            }
            _isLoading.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isLoggedIn.value = false
    }
}
