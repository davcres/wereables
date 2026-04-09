package com.davidcrespo.ble.ble.scanner

data class BleUiState(
    val scanning: Boolean = false,
    val devices: List<BleScanResult> = emptyList(),
    val error: String? = null,
    val connectedDeviceAddress: String? = null,
    val deviceValues: Map<String, String> = emptyMap()
)