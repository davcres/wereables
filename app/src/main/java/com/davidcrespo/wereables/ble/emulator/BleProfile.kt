package com.davidcrespo.wereables.ble.emulator

import java.util.UUID

// Enum defining supported profiles and their UUIDs
enum class BleProfile(
    val displayName: String,
    val serviceUuid: UUID,
    val charUuid: UUID
) {
    THERMOMETER("Termómetro",
        UUID.fromString("00001809-0000-1000-8000-00805f9b34fb"), // Health Thermometer
        UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb")  // Temp Measurement
    ),
    HEART_RATE("Ritmo Cardíaco",
        UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb"), // Heart Rate
        UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")  // HR Measurement
    ),
    GLUCOSE("Glucómetro",
        UUID.fromString("00001808-0000-1000-8000-00805f9b34fb"), // Glucose
        UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb")  // Glucose Measurement
    ),
    BLOOD_PRESSURE("Tensiómetro",
        UUID.fromString("00001810-0000-1000-8000-00805f9b34fb"), // Blood Pressure
        UUID.fromString("00002A35-0000-1000-8000-00805f9b34fb")  // BP Measurement
    ),
    PULSE_OXIMETER("Oxímetro",
        UUID.fromString("00001822-0000-1000-8000-00805f9b34fb"), // Pulse Oximeter
        UUID.fromString("00002A5F-0000-1000-8000-00805f9b34fb")  // PLX Continuous Measurement
    )
}