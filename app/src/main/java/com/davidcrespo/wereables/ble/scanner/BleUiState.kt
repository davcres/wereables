package com.davidcrespo.wereables.ble.scanner

import com.davidcrespo.wereables.ble.scanner.BleScanResult

data class BleUiState(
    val scanning: Boolean = false,
    val devices: List<BleScanResult> = emptyList(),
    val error: String? = null,
    val connectedDeviceAddress: String? = null,
    val deviceValues: Map<String, String> = emptyMap()
)