package com.davidcrespo.wereables.models

sealed class HealthConnectAvailability {
    data object Available : HealthConnectAvailability()
    data object ProviderUpdateRequired : HealthConnectAvailability()
    data object Unavailable : HealthConnectAvailability()
}