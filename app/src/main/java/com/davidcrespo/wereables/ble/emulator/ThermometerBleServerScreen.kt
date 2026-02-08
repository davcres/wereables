package com.davidcrespo.wereables.ble.emulator

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
        Text("Emulador Multi-Dispositivo", style = MaterialTheme.typography.headlineSmall)

        if (!hasPermissions) {
            Button(onClick = { launcher.launch(requiredPermissions) }) { Text("Conceder Permisos") }
        } else {
            // Profile Selector
            if (!isAdvertising) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = profile.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Dispositivo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BleProfile.values().forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.displayName) },
                                onClick = { vm.setProfile(p); expanded = false }
                            )
                        }
                    }
                }
            } else {
                 Text("Emulando: ${profile.displayName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isAdvertising) "ANUNCIANDO" else "DETENIDO")
                    Switch(checked = isAdvertising, onCheckedChange = { vm.toggleServer(it) })
                }
            }
            
            Spacer(Modifier.height(10.dp))
            
            // Dynamic Controls
            if (isAdvertising || true) { // Show controls always for preview
                Text("Controles (Ajustar y notificar)", style = MaterialTheme.typography.labelLarge)
                
                when (profile) {
                    BleProfile.THERMOMETER -> {
                        SliderControl("Temperatura", vm.temperature, 30f..45f, "%.1f °C") { vm.updateValues() }
                    }
                    BleProfile.HEART_RATE -> {
                        SliderControl("BPM", vm.heartRate, 40f..200f, "%.0f bpm") { vm.updateValues() }
                    }
                    BleProfile.BLOOD_PRESSURE -> {
                        SliderControl("Sistólica", vm.systolic, 80f..180f, "%.0f mmHg") { vm.updateValues() }
                        SliderControl("Diastólica", vm.diastolic, 50f..120f, "%.0f mmHg") { vm.updateValues() }
                    }
                    BleProfile.GLUCOSE -> {
                        SliderControl("Glucosa", vm.glucose, 50f..300f, "%.0f mg/dL") { vm.updateValues() }
                    }
                    BleProfile.PULSE_OXIMETER -> {
                        SliderControl("SpO2", vm.spo2, 80f..100f, "%.0f %%") { vm.updateValues() }
                        SliderControl("Pulso (PR)", vm.pulseRate, 40f..150f, "%.0f bpm") { vm.updateValues() }
                    }
                }
                
                Button(onClick = { vm.updateValues() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Forzar Envío de Datos")
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