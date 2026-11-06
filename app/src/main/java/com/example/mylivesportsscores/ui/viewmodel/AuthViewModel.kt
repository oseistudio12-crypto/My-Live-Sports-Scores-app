package com.example.mylivesportsscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylivesportsscores.data.repository.AuthManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authManager: AuthManager) : ViewModel() {
    private val _user = MutableStateFlow(Firebase.auth.currentUser)
    val user: StateFlow<com.google.firebase.auth.FirebaseUser?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Firebase.auth.addAuthStateListener {
            _user.value = it.currentUser
        }
    }

    fun attemptAutoSignIn() {
        _isLoading.value = true
        authManager.attemptAutoSignIn(
            scope = viewModelScope,
            onAuthSuccess = { _isLoading.value = false },
            onUnauthenticated = { _isLoading.value = false }
        )
    }

    fun signIn(activity: android.app.Activity) {
        _isLoading.value = true
        _error.value = null
        authManager.onGoogleSignInClicked(
            activity = activity,
            scope = viewModelScope,
            onAuthSuccess = { _isLoading.value = false },
            onAuthError = {
                _isLoading.value = false
                _error.value = it
            },
            onAuthCancelled = { _isLoading.value = false }
        )
    }

    fun signOut() {
        authManager.signOut(viewModelScope) {
            _user.value = null
        }
    }
}
