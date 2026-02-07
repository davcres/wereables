package com.davidcrespo.wereables.healthConnect.models

sealed class HealthConnectAvailability {
    data object Available : HealthConnectAvailability()
    data object ProviderUpdateRequired : HealthConnectAvailability()
    data object Unavailable : HealthConnectAvailability()
}