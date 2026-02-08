package com.davidcrespo.wereables.ble.emulator

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
class MultiProfileBleServer(private val context: Context) {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising = _isAdvertising.asStateFlow()

    private val connectedDevices = mutableListOf<BluetoothDevice>()
    private var currentProfile: BleProfile = BleProfile.THERMOMETER

    fun startServer(profile: BleProfile) {
        if (_isAdvertising.value) return
        
        currentProfile = profile
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) return

        setupGattServer(profile)
        startAdvertising(profile)
    }

    fun stopServer() {
        if (!_isAdvertising.value) return
        stopAdvertising()
        stopGattServer()
    }

    // Generic update function depending on profile
    fun updateData(
        value1: Float, // Temp / HR / Systolic / Glucose / SpO2
        value2: Float = 0f, // Diastolic / PulseRate
        value3: Float = 0f  // Mean Arterial Pressure (MAP)
    ) {
        val characteristic = gattServer
            ?.getService(currentProfile.serviceUuid)
            ?.getCharacteristic(currentProfile.charUuid)
            ?: return

        val data = when (currentProfile) {
            BleProfile.THERMOMETER -> createThermometerData(value1)
            BleProfile.HEART_RATE -> createHeartRateData(value1.toInt())
            BleProfile.BLOOD_PRESSURE -> createBloodPressureData(value1, value2, value3)
            BleProfile.GLUCOSE -> createGlucoseData(value1)
            BleProfile.PULSE_OXIMETER -> createPulseOxData(value1, value2)
        }

        characteristic.value = data
        // Notify devices
        for (device in connectedDevices) {
            gattServer?.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    private fun setupGattServer(profile: BleProfile) {
        val callback = object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevices.add(device)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connectedDevices.remove(device)
                }
            }
            // Basic Read/Descriptor responses needed for bonding/discovery
            override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
                 if (profile.charUuid == characteristic.uuid) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
                } else {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                }
            }
            override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
                if (responseNeeded) gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        gattServer = bluetoothManager?.openGattServer(context, callback)
        val service = BluetoothGattService(profile.serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        
        val props = BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE
        val perms = BluetoothGattCharacteristic.PERMISSION_READ
        
        val characteristic = BluetoothGattCharacteristic(profile.charUuid, props, perms)
        
        // CCCD Descriptor for notifications
        val cccd = BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        characteristic.addDescriptor(cccd)

        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
    }

    private fun startAdvertising(profile: BleProfile) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(profile.serviceUuid))
            .build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private fun stopAdvertising() {
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        _isAdvertising.value = false
    }

    private fun stopGattServer() {
        gattServer?.close()
        gattServer = null
        connectedDevices.clear()
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) { _isAdvertising.value = true }
        override fun onStartFailure(errorCode: Int) { _isAdvertising.value = false }
    }

    // --- Data Generators ---

    private fun createThermometerData(temp: Float): ByteArray {
        // Flags (1 byte) + Float (4 bytes). Flags: 0x00 (Celsius)
        val mantissa = (temp * 10).toInt()
        val exponent = -1 
        return byteArrayOf(0x00, (mantissa and 0xFF).toByte(), ((mantissa shr 8) and 0xFF).toByte(), ((mantissa shr 16) and 0xFF).toByte(), exponent.toByte())
    }

    private fun createHeartRateData(bpm: Int): ByteArray {
        // Flags: 0 (UINT8 fmt) -> Byte 0. BPM -> Byte 1.
        return byteArrayOf(0x00, bpm.toByte())
    }

    private fun createBloodPressureData(systolic: Float, diastolic: Float, map: Float): ByteArray {
        // Flags (1 byte) - 0x00 (mmHg)
        // Systolic (SFloat - 2 bytes)
        // Diastolic (SFloat - 2 bytes)
        // MAP (SFloat - 2 bytes)
        val flags: Byte = 0x00
        return byteArrayOf(flags) + toSFloat(systolic) + toSFloat(diastolic) + toSFloat(map)
    }
    
    private fun createGlucoseData(mgDl: Float): ByteArray {
        // Flags (1 byte): 0x00
        // Seq Num (2 bytes): 0x0001
        // Base Time (7 bytes): Ignored/Zeroed for simplicity (usually required)
        // Time Offset (2 bytes): 0
        // Conc (SFloat - 2 bytes): value
        // Type-Sample Loc (1 byte): 0
        // Sensor Status (2 bytes): 0
        
        // Simplified: Flags + Seq + Time(mock) + Conc
        val flags: Byte = 0x03 // Time offset present, conc type/loc present
        val seq = byteArrayOf(0x01, 0x00) // Seq 1
        // Mocking time bytes (Year 2 bytes, M, D, H, M, S) - 7 bytes
        val time = byteArrayOf(0xE6.toByte(), 0x07, 0x02, 0x08, 0x0A, 0x00, 0x00) // 2022...
        val timeOffset = byteArrayOf(0x00, 0x00)
        
        // Using kg/L or mol/L is standard, but often mg/dL used with different flags. 
        // We'll trust the SFloat generator for basic raw value simulation.
        return byteArrayOf(flags) + seq + time + timeOffset + toSFloat(mgDl) + byteArrayOf(0x11) // type/loc
    }

    private fun createPulseOxData(spo2: Float, pulseRate: Float): ByteArray {
        // Flags: 0x00 (Normal)
        // SpO2 (SFloat)
        // PR (SFloat)
        val flags: Byte = 0x00
        return byteArrayOf(flags) + toSFloat(spo2) + toSFloat(pulseRate)
    }

    // IEEE-11073 16-bit SFLOAT
    private fun toSFloat(value: Float): ByteArray {
        // SFloat has a 12-bit signed mantissa. Max positive value is 2047.
        // If we use exponent -1 (value * 10), max value is 204.7 before overflow/sign-flip.
        
        val mantissa: Int
        val exponent: Int

        // Check if value fits in 12-bit mantissa with exp -1 (limit ~204.7)
        // AND check if we really need precision (value has decimals)
        if (value < 204.7f) {
            mantissa = (value * 10).toInt()
            exponent = -1
        } else {
            // For values >= 205, we must use exponent 0 to stay within 2047 limit.
            // This sacrifices one decimal place of precision for high values, which is standard.
            mantissa = value.roundToInt()
            exponent = 0
        }
        
        val expNibble = (exponent and 0x0F)
        val man = mantissa
        
        val byte0 = (man and 0xFF).toByte()
        val byte1 = (((man shr 8) and 0x0F) or (expNibble shl 4)).toByte()
        
        return byteArrayOf(byte0, byte1)
    }
}