package com.davidcrespo.wereables.ble.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class ThermometerBleScanner(private val context: Context) {

    companion object {
        // Standard Health Service UUIDs
        val HEALTH_THERMOMETER_SERVICE_UUID: UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val GLUCOSE_SERVICE_UUID: UUID = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb")
        val BLOOD_PRESSURE_SERVICE_UUID: UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")
        val PULSE_OXIMETER_SERVICE_UUID: UUID = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb")

        val ALL_HEALTH_SERVICES = listOf(
            HEALTH_THERMOMETER_SERVICE_UUID,
            HEART_RATE_SERVICE_UUID,
            GLUCOSE_SERVICE_UUID,
            BLOOD_PRESSURE_SERVICE_UUID,
            PULSE_OXIMETER_SERVICE_UUID
        )
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            // <31 suele requerir location para escanear
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun scanPeripherals(
        scanMode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY
    ): Flow<BleScanResult> = callbackFlow {
        // validamos permisos antes
        if (!hasScanPermission()) {
            close(SecurityException("Missing BLUETOOTH_SCAN / location permission"))
            return@callbackFlow
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            close(IllegalStateException("Bluetooth disabled/unavailable"))
            return@callbackFlow
        }

        val scanner = adapter.bluetoothLeScanner
            ?: run {
                close(IllegalStateException("BLE scanner unavailable"))
                return@callbackFlow
            }

        // Filter for all supported health services
        val filters = ALL_HEALTH_SERVICES.map { 
            ScanFilter.Builder().setServiceUuid(ParcelUuid(it)).build()
        }

        val settings = ScanSettings.Builder()
            .setScanMode(scanMode)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord
                val uuids = record?.serviceUuids?.map { it.uuid }.orEmpty()

                trySend(
                    BleScanResult(
                        address = result.device.address,
                        name = result.device.name ?: record?.deviceName,
                        rssi = result.rssi,
                        serviceUuids = uuids
                    )
                )
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("Scan failed: $errorCode"))
            }
        }

        scanner.startScan(filters, settings, callback)

        awaitClose {
            runCatching { scanner.stopScan(callback) }
        }
    }
}