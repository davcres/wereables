package com.davidcrespo.wereables.ble.scanner

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class BleScanViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val scanner: ThermometerBleScanner = ThermometerBleScanner(app.applicationContext)
    private val _ui = MutableStateFlow(BleUiState())
    val ui: StateFlow<BleUiState> = _ui.asStateFlow()

    private var scanJob: Job? = null
    private var activeGatt: BluetoothGatt? = null

    fun startScan() {
        if (scanJob != null) return

        _ui.value = _ui.value.copy(scanning = true, error = null)
        scanJob = viewModelScope.launch {
            runCatching {
                scanner.scanPeripherals()
                    // dedupe por address + orden por RSSI
                    .scan(emptyMap<String, BleScanResult>()) { acc, item ->
                        acc + (item.address to item)
                    }
                    .collect { map ->
                        val sorted = map.values.sortedByDescending { it.rssi }
                        _ui.value = _ui.value.copy(devices = sorted, scanning = true, error = null)
                    }
            }.onFailure { e ->
                _ui.value = _ui.value.copy(scanning = false, error = e.message ?: "Scan error")
                scanJob = null
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _ui.value = _ui.value.copy(scanning = false)
    }

    fun toggleConnection(result: BleScanResult) {
        if (_ui.value.connectedDeviceAddress == result.address) {
            disconnect()
        } else {
            connect(result)
        }
    }

    private fun disconnect() {
        activeGatt?.disconnect()
        activeGatt?.close()
        activeGatt = null
        _ui.value = _ui.value.copy(connectedDeviceAddress = null)
    }

    private fun connect(result: BleScanResult) {
        disconnect() // Close previous
        stopScan() // Stop scanning when connecting

        val bluetoothManager = getApplication<Application>().getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        val device = adapter.getRemoteDevice(result.address)

        _ui.value = _ui.value.copy(connectedDeviceAddress = result.address, scanning = false)

        activeGatt = device.connectGatt(getApplication(), false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BleClient", "Connected to ${gatt.device.address}")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BleClient", "Disconnected")
                _ui.value = _ui.value.copy(connectedDeviceAddress = null)
                activeGatt?.close()
                activeGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            // Find interesting characteristics
            val services = gatt.services
            for (service in services) {
                for (char in service.characteristics) {
                    if (isHealthCharacteristic(char.uuid)) {
                        enableNotifications(gatt, char)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
           parseValue(gatt.device.address, characteristic)
        }
        
        // Android 13+ support
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            parseValue(gatt.device.address, characteristic, value)
        }
    }

    private fun isHealthCharacteristic(uuid: UUID): Boolean {
        // Simple check against known CHAR UUIDs or just checking if it belongs to known Services
        // Ideally we check standard 16-bit UUIDs
        val u = uuid.toString().uppercase()
        return u.contains("2A1C") || // Temp
               u.contains("2A37") || // HR
               u.contains("2A18") || // Glucose
               u.contains("2A35") || // BP
               u.contains("2A5F")    // PLX
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun parseValue(address: String, characteristic: BluetoothGattCharacteristic, rawValue: ByteArray? = null) {
        val value = rawValue ?: characteristic.value
        val uuid = characteristic.uuid.toString().uppercase()
        
        var parsedText = "Data..."

        try {
            when {
                uuid.contains("2A1C") -> { // Temp
                    // * Byte 0: Flags (Indica si es Celsius o Fahrenheit).
                    // * Byte 1-4: Temperatura (El valor que nos interesa).
                    val temp = parseFloat32(value, 1)
                    parsedText = "%.1f °C".format(temp)
                }
                uuid.contains("2A37") -> { // HR
                    // * Byte 0: Flags (Indica formato UINT8 o UINT16).
                    // * Byte 1: BPM (Latidos por minuto).
                    val flags = value[0].toInt()
                    val format = if ((flags and 1) != 0) BluetoothGattCharacteristic.FORMAT_UINT16 else BluetoothGattCharacteristic.FORMAT_UINT8
                    val hr = characteristic.getIntValue(format, 1)
                    parsedText = "$hr bpm"
                }
                uuid.contains("2A35") -> { // BP
                    // * Byte 0: Flags (Unidades kPa/mmHg, etc.).
                    // * Byte 1-2: Sistólica (SFloat 16-bit). -> Empieza en offset 1.
                    // * Byte 3-4: Diastólica (SFloat 16-bit). -> Empieza en offset 3 (1 byte de flags + 2 bytes de sistólica).
                    // * Byte 5-6: Presión Arterial Media (MAP).
                    val sys = parseSFloat(value, 1)
                    val dia = parseSFloat(value, 3) // 1byte flags + 2byte SFloat
                    parsedText = "BP: %.0f/%.0f".format(sys, dia)
                }
                uuid.contains("2A18") -> { // Glucose
                    // * Byte 0: Flags (1 byte).
                    // * Byte 1-2: Número de Secuencia (2 bytes).
                    // * Byte 3-9: Fecha y Hora Base (7 bytes).
                    // * Byte 10-11: Offset de Tiempo (2 bytes).
                    // * Byte 12-13: Concentración de Glucosa (El valor que queremos).
                    val conc = parseSFloat(value, 12)
                    parsedText = "Gluc: %.0f mg/dL".format(conc)
                }
                uuid.contains("2A5F") -> { // PulseOx
                    // * Byte 0: Flags.
                    // * Byte 1-2: SpO2 (Oxígeno %). -> Empieza en offset 1.
                    // * Byte 3-4: Pulso (PR). -> Empieza en offset 3 (1 byte de flags + 2 bytes de SpO2).
                    val spo2 = parseSFloat(value, 1)
                    val pr = parseSFloat(value, 3)
                    parsedText = "SpO2: %.0f%% | PR: %.0f".format(spo2, pr)
                }
            }
        } catch (e: Exception) {
            parsedText = "Parse Err: ${value.joinToString { "%02x".format(it) }}"
        }

        val newMap = _ui.value.deviceValues.toMutableMap()
        newMap[address] = parsedText
        _ui.value = _ui.value.copy(deviceValues = newMap)
    }

    // Helper for 16-bit SFloat (IEEE-11073)
    // 4-bit exponent, 12-bit mantissa
    private fun parseSFloat(data: ByteArray, offset: Int): Float {
        // Byte 0: Mantissa Low
        // Byte 1: Exp (4 bits high) | Mantissa High (4 bits low)
        if (offset + 1 >= data.size) return 0f
        
        val b0 = data[offset].toInt() and 0xFF
        val b1 = data[offset+1].toInt() and 0xFF
        
        val mantissaHigh = b1 and 0x0F
        val exponent = (b1 shr 4)
        
        // Sign extension for 12-bit mantissa
        var mantissa = (mantissaHigh shl 8) or b0
        if ((mantissa and 0x0800) != 0) {
            mantissa = mantissa or 0xFFFFF000.toInt()
        }
        
        // Sign extension for 4-bit exponent
        var exp = exponent
        if ((exp and 0x08) != 0) {
             exp = exp or 0xFFFFFFF0.toInt()
        }
        
        return (mantissa * Math.pow(10.0, exp.toDouble())).toFloat()
    }
    
    // IEEE-11073 32-bit Float (used for Temp)
    // 8-bit exp, 24-bit mantissa
    // BUT our emulator used a simplified byte array manually constructed.
    // Let's rely on standard characteristic.getFloatValue if possible, or manual parse.
    // Our emulator: [Flags, M_LSB, M, M_MSB, Exp] -> 5 bytes total.
    // This matches FORMAT_FLOAT (32-bit) which is 4 bytes + flags? No.
    // Android `getFloatValue(FORMAT_FLOAT, offset)` expects IEEE 11073 32-bit: 4 bytes.
    // Our emulator sent: 1 byte flags + 4 bytes Float.
    // So we read from offset 1.
    // However, our emulator construction was:
    // mantissa (int), exponent (-1). 
    // byteArrayOf(flags, m0, m1, m2, exp)
    // This is NOT standard IEEE-11073 32-bit layout strictly speaking if we just dump bytes, 
    // but let's try manual parse matching our emitter.
    // Standard 32-bit SFLOAT: 24-bit mantissa, 8-bit exponent.
    // Our Emitter: mantissa (24), exponent (8). Little endian.
    // Byte 0: M LSB
    // Byte 1: M
    // Byte 2: M MSB
    // Byte 3: Exponent
    // So reading 4 bytes at offset 1 should work if we parse it as:
    // val mantissa = (b0) | (b1<<8) | (b2<<16)
    // val exp = b3
    private fun parseFloat32(data: ByteArray, offset: Int): Float {
         // This is for 32-bit IEEE 11073
         // But wait, our emulator code:
         /*
            return byteArrayOf(
            flags,
            (mantissa and 0xFF).toByte(),
            ((mantissa shr 8) and 0xFF).toByte(),
            ((mantissa shr 16) and 0xFF).toByte(),
            exponent.toByte()
        )
        */
        // So yes: b0, b1, b2 are Mantissa. b3 is Exponent.
        val b0 = data[offset].toInt() and 0xFF
        val b1 = data[offset+1].toInt() and 0xFF
        val b2 = data[offset+2].toInt() and 0xFF
        val b3 = data[offset+3].toInt() // Exponent (signed 8 bit)
        
        var mantissa = b0 or (b1 shl 8) or (b2 shl 16)
        // Sign extend 24-bit
        if ((mantissa and 0x00800000) != 0) {
            mantissa = mantissa or -0x1000000 // 0xFF000000
        }
        
        return (mantissa * Math.pow(10.0, b3.toDouble())).toFloat()
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}