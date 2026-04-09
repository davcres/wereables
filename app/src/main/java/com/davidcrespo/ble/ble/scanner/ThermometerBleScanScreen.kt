package com.davidcrespo.ble.ble.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermometerBleScanScreen(
    vm: BleScanViewModel
) {
    val context = LocalContext.current
    val state = vm.ui.collectAsStateWithLifecycle().value

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasAllPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) ==
                PackageManager.PERMISSION_GRANTED
        }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (hasAllPermissions()) {
            vm.startScan()
        }
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (isBluetoothEnabled(context) && hasAllPermissions()) vm.startScan()
    }

    val btEnabled = remember { mutableStateOf(isBluetoothEnabled(context)) }

    LaunchedEffect(Unit) {
        btEnabled.value = isBluetoothEnabled(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.davidcrespo.ble.R.string.scanner_title)) },
                actions = {
                    IconButton(
                        onClick = {
                            btEnabled.value = isBluetoothEnabled(context)
                            if (!hasAllPermissions()) {
                                permissionLauncher.launch(requiredPermissions)
                                return@IconButton
                            }
                            if (!btEnabled.value) {
                                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                return@IconButton
                            }
                            if (state.scanning) vm.stopScan() else vm.startScan()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Start/Stop"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            StatusCard(
                hasPermissions = hasAllPermissions(),
                btEnabled = btEnabled.value,
                scanning = state.scanning,
                onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
                onEnableBluetooth = {
                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                },
                onToggleScan = {
                    btEnabled.value = isBluetoothEnabled(context)
                    when {
                        !hasAllPermissions() -> permissionLauncher.launch(requiredPermissions)
                        !btEnabled.value -> enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        state.scanning -> vm.stopScan()
                        else -> vm.startScan()
                    }
                }
            )

            state.error?.let { msg ->
                AssistChip(onClick = { /* noop */ }, label = { Text(stringResource(com.davidcrespo.ble.R.string.error_prefix, msg)) })
            }

            if (state.scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            DevicesList(
                devices = state.devices,
                connectedAddress = state.connectedDeviceAddress,
                values = state.deviceValues,
                onDeviceClick = { vm.toggleConnection(it) }
            )
        }
    }
}

@Composable
private fun StatusCard(
    hasPermissions: Boolean,
    btEnabled: Boolean,
    scanning: Boolean,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onToggleScan: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(com.davidcrespo.ble.R.string.status_label), style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (hasPermissions) stringResource(com.davidcrespo.ble.R.string.permissions_ok) else stringResource(com.davidcrespo.ble.R.string.missing_permissions)) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(if (btEnabled) stringResource(com.davidcrespo.ble.R.string.bluetooth_on) else stringResource(com.davidcrespo.ble.R.string.bluetooth_off)) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!hasPermissions) {
                    Button(onClick = onRequestPermissions) { Text(stringResource(com.davidcrespo.ble.R.string.request_permissions)) }
                }
                if (!btEnabled) {
                    Button(onClick = onEnableBluetooth) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(com.davidcrespo.ble.R.string.activate_bluetooth))
                    }
                }
                Button(onClick = onToggleScan, enabled = hasPermissions && btEnabled) {
                    Text(if (scanning) stringResource(com.davidcrespo.ble.R.string.stop_scan) else stringResource(com.davidcrespo.ble.R.string.start_scan))
                }
            }
            
            Text(
                text = stringResource(com.davidcrespo.ble.R.string.scanner_hint),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DevicesList(
    devices: List<BleScanResult>,
    connectedAddress: String?,
    values: Map<String, String>,
    onDeviceClick: (BleScanResult) -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(com.davidcrespo.ble.R.string.detected_peripherals, devices.size),
                style = MaterialTheme.typography.titleMedium
            )

            if (devices.isEmpty()) {
                Text(stringResource(com.davidcrespo.ble.R.string.no_devices_found))
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices, key = { it.address }) { d ->
                    DeviceRow(
                        d = d, 
                        isConnected = d.address == connectedAddress,
                        currentValue = values[d.address],
                        onClick = { onDeviceClick(d) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    d: BleScanResult, 
    isConnected: Boolean, 
    currentValue: String?, 
    onClick: () -> Unit
) {
    val deviceType = when {
        d.serviceUuids.any { it.toString().contains("1809", ignoreCase = true) } -> stringResource(com.davidcrespo.ble.R.string.thermometer)
        d.serviceUuids.any { it.toString().contains("180D", ignoreCase = true) } -> stringResource(com.davidcrespo.ble.R.string.heart_rate)
        d.serviceUuids.any { it.toString().contains("1808", ignoreCase = true) } -> stringResource(com.davidcrespo.ble.R.string.glucose_meter)
        d.serviceUuids.any { it.toString().contains("1810", ignoreCase = true) } -> stringResource(com.davidcrespo.ble.R.string.blood_pressure)
        d.serviceUuids.any { it.toString().contains("1822", ignoreCase = true) } -> stringResource(com.davidcrespo.ble.R.string.pulse_oximeter)
        else -> stringResource(com.davidcrespo.ble.R.string.device_unknown)
    }

    val cardColors = if (isConnected) {
        CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.elevatedCardColors()
    }

    ElevatedCard(onClick = onClick, colors = cardColors) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = d.name ?: stringResource(com.davidcrespo.ble.R.string.no_name),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Badge { Text(deviceType) }
            }
            Text(stringResource(com.davidcrespo.ble.R.string.mac_label, d.address), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(com.davidcrespo.ble.R.string.rssi_label, d.rssi), style = MaterialTheme.typography.bodySmall)
            
            if (isConnected) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = currentValue ?: stringResource(com.davidcrespo.ble.R.string.waiting_data),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (currentValue != null) {
                Text(stringResource(com.davidcrespo.ble.R.string.last_value_label, currentValue), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun isBluetoothEnabled(context: Context): Boolean {
    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bm.adapter ?: return false

    return try {
        adapter.isEnabled
    } catch (se: SecurityException) {
        false
    }
}