package com.davidcrespo.ble.ble.emulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermometerBleServerScreen() {
    val context = LocalContext.current
    val vm: BleServerViewModel = viewModel(factory = BleServerViewModelFactory(context))
    
    val isAdvertising by vm.isAdvertising.collectAsState(initial = false)
    val profile by vm.selectedProfile

    // Permissions check
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
    }
    
    var hasPermissions by remember {
        mutableStateOf(requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasPermissions = requiredPermissions.all { perm -> ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(stringResource(com.davidcrespo.ble.R.string.emulator_title), style = MaterialTheme.typography.headlineSmall)

        if (!hasPermissions) {
            Button(onClick = { launcher.launch(requiredPermissions) }) { Text(stringResource(com.davidcrespo.ble.R.string.grant_permissions)) }
        } else {
            // Profile Selector
            if (!isAdvertising) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = stringResource(profile.displayNameResId),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(com.davidcrespo.ble.R.string.device_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BleProfile.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(stringResource(p.displayNameResId)) },
                                onClick = { vm.setProfile(p); expanded = false }
                            )
                        }
                    }
                }
            } else {
                 Text(stringResource(com.davidcrespo.ble.R.string.emulating, stringResource(profile.displayNameResId)), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isAdvertising) stringResource(com.davidcrespo.ble.R.string.advertising) else stringResource(com.davidcrespo.ble.R.string.stopped))
                    Switch(checked = isAdvertising, onCheckedChange = { vm.toggleServer(it) })
                }
            }
            
            Spacer(Modifier.height(10.dp))
            
            // Dynamic Controls
            if (isAdvertising || true) { // Show controls always for preview
                Text(stringResource(com.davidcrespo.ble.R.string.controls_label), style = MaterialTheme.typography.labelLarge)
                
                when (profile) {
                    BleProfile.THERMOMETER -> {
                        SliderControl(stringResource(com.davidcrespo.ble.R.string.temperature), vm.temperature, 30f..45f, "%.1f °C") { vm.updateValues() }
                    }
                    BleProfile.HEART_RATE -> {
                        SliderControl("BPM", vm.heartRate, 40f..200f, "%.0f bpm") { vm.updateValues() }
                    }
                    BleProfile.BLOOD_PRESSURE -> {
                        SliderControl(stringResource(com.davidcrespo.ble.R.string.systolic), vm.systolic, 80f..180f, "%.0f mmHg") { vm.updateValues() }
                        SliderControl(stringResource(com.davidcrespo.ble.R.string.diastolic), vm.diastolic, 50f..120f, "%.0f mmHg") { vm.updateValues() }
                    }
                    BleProfile.GLUCOSE -> {
                        SliderControl(stringResource(com.davidcrespo.ble.R.string.glucose), vm.glucose, 50f..300f, "%.0f mg/dL") { vm.updateValues() }
                    }
                    BleProfile.PULSE_OXIMETER -> {
                        SliderControl("SpO2", vm.spo2, 80f..100f, "%.0f %%") { vm.updateValues() }
                        SliderControl("Pulso (PR)", vm.pulseRate, 40f..150f, "%.0f bpm") { vm.updateValues() }
                    }
                }
                
                Button(onClick = { vm.updateValues() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(com.davidcrespo.ble.R.string.force_send_data))
                }
            }
        }
    }
}

@Composable
fun SliderControl(
    label: String, 
    state: MutableState<Float>, 
    range: ClosedFloatingPointRange<Float>, 
    fmt: String,
    onValueChangeFinished: () -> Unit
) {
    Column {
        Text("$label: ${fmt.format(state.value)}")
        Slider(
            value = state.value,
            onValueChange = { state.value = it },
            valueRange = range,
            onValueChangeFinished = onValueChangeFinished
        )
    }
}