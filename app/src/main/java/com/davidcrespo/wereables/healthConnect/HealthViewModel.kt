package com.davidcrespo.wereables.healthConnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HealthConnectRepository(app.applicationContext)

    private val _uiState = MutableStateFlow(
        HealthUiState(availability = repo.availability())
    )
    val uiState: StateFlow<HealthUiState> = _uiState

    val requiredPermissions: Set<String> = repo.requiredPermissions

    fun refreshPermissionState() {
        viewModelScope.launch {
            runCatching {
                repo.hasAllPermissions()
            }.onSuccess { has ->
                _uiState.value = _uiState.value.copy(hasPermissions = has, error = null)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val steps = repo.getTodaySteps()
                val hr = repo.getLatestHeartRate(24)
                steps to hr
            }.onSuccess { (steps, hr) ->
                _uiState.value = _uiState.value.copy(
                    todaySteps = steps,
                    latestHeartRate = hr,
                    isLoading = false,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error leyendo Health Connect"
                )
            }
        }
    }
}
