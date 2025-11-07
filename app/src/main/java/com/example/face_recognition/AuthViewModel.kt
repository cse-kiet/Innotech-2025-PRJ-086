package com.example.face_recognition

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> get() = _currentUser

    private val _authResult = MutableLiveData<Result<Boolean>>()
    val authResult: LiveData<Result<Boolean>> get() = _authResult

    init {
        // 🔥 When ViewModel initializes (app opens), restore user if already logged in
        restoreUserIfLoggedIn()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun restoreUserIfLoggedIn() {
        viewModelScope.launch {
            when (val result = userRepository.getCurrentUser()) {
                is Result.Success -> _currentUser.value = result.data
                is Result.Error -> _currentUser.value = null
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authResult.value = userRepository.signUp(email, password, name)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun login(email: String, password: String) {
        when (val result = userRepository.login(email, password)) {
            is Result.Success -> {
                _authResult.value = result
                fetchCurrentUser()
            }
            is Result.Error -> throw result.exception // ✅ now it will throw
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchCurrentUser() {
        viewModelScope.launch {
            when (val result = userRepository.getCurrentUser()) {
                is Result.Success -> _currentUser.value = result.data
                is Result.Error -> Log.e("AuthViewModel", "Error fetching user", result.exception)
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            userRepository.logOut()
            _currentUser.value = null
        }
    }
}
