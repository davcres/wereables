package com.davidcrespo.wereables

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davidcrespo.wereables.healthConnect.HealthConnectRepository
import com.davidcrespo.wereables.healthConnect.models.HealthConnectAvailability
import com.davidcrespo.wereables.healthConnect.models.HeartRateLatestResult
import com.davidcrespo.wereables.healthConnect.models.StepsTodayResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HealthUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.Unavailable,
    val hasPermissions: Boolean = false,
    val todaySteps: StepsTodayResult? = null,
    val latestHeartRate: HeartRateLatestResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

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
