package com.davidcrespo.ble.healthConnect

import com.davidcrespo.ble.healthConnect.models.HealthConnectAvailability
import com.davidcrespo.ble.healthConnect.models.HeartRateLatestResult
import com.davidcrespo.ble.healthConnect.models.StepsTodayResult

data class HealthUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.Unavailable,
    val hasPermissions: Boolean = false,
    val todaySteps: StepsTodayResult? = null,
    val latestHeartRate: HeartRateLatestResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)