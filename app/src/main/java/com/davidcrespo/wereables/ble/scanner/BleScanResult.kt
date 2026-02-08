package com.davidcrespo.wereables.ble.scanner

import java.util.UUID

data class BleScanResult(
    val address: String,
    val name: String?,
    val rssi: Int,
    val serviceUuids: List<UUID>
)