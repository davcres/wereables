package com.davidcrespo.wereables

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidcrespo.wereables.models.HealthConnectAvailability
import com.davidcrespo.wereables.models.HeartRateLatestResult
import com.davidcrespo.wereables.models.StepsTodayResult
import com.davidcrespo.wereables.ui.theme.WereablesTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private val vm: HealthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            WereablesTheme {
                HealthConnectScreen(vm = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectScreen(vm: HealthViewModel) {
    val state = vm.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current

    // Launcher oficial de permisos de Health Connect
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        // granted = set de permisos concedidos
        vm.refreshPermissionState()
        if (granted.containsAll(vm.requiredPermissions)) {
            vm.loadDashboard()
        }
    }

    LaunchedEffect(Unit) {
        vm.refreshPermissionState()
        if (state.hasPermissions) vm.loadDashboard()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Health Connect Demo") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvailabilityCard(
                availability = state.availability,
                onOpenHealthConnect = { openHealthConnectOrPlayStore(context) }
            )

            PermissionsCard(
                hasPermissions = state.hasPermissions,
                onRequestPermissions = {
                    permissionLauncher.launch(vm.requiredPermissions)
                }
            )

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.error?.let { msg ->
                AssistChip(onClick = {}, label = { Text("Error: $msg") })
            }

            DashboardCard(
                enabled = state.hasPermissions && state.availability is HealthConnectAvailability.Available,
                steps = state.todaySteps,
                hr = state.latestHeartRate,
                onRefresh = { vm.loadDashboard() }
            )
        }
    }
}

@Composable
private fun AvailabilityCard(
    availability: HealthConnectAvailability,
    onOpenHealthConnect: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Disponibilidad", style = MaterialTheme.typography.titleMedium)

            val text = when (availability) {
                HealthConnectAvailability.Available -> "Health Connect disponible"
                HealthConnectAvailability.ProviderUpdateRequired -> "Requiere instalar/actualizar Health Connect"
                HealthConnectAvailability.Unavailable -> "Health Connect no disponible en este dispositivo"
            }
            Text(text)

            if (availability is HealthConnectAvailability.ProviderUpdateRequired) {
                Button(onClick = onOpenHealthConnect) {
                    Text("Abrir / Instalar Health Connect")
                }
            }
        }
    }
}

@Composable
private fun PermissionsCard(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Permisos", style = MaterialTheme.typography.titleMedium)
            Text(if (hasPermissions) "Permisos concedidos" else "Faltan permisos")

            if (!hasPermissions) {
                Button(onClick = onRequestPermissions) {
                    Text("Conceder permisos")
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    enabled: Boolean,
    steps: StepsTodayResult?,
    hr: HeartRateLatestResult?,
    onRefresh: () -> Unit
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dashboard", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onRefresh, enabled = enabled) { Text("Refrescar") }
            }

            if (!enabled) {
                Text("Concede permisos y asegúrate de que Health Connect esté disponible.")
                return@Column
            }

            Text("Pasos hoy: ${steps?.totalSteps ?: "—"}")
            steps?.dataOrigins?.takeIf { it.isNotEmpty() }?.let { origins ->
                Text("Fuentes pasos: " + origins.joinToString { it.packageName })
            }

            val formatter = remember {
                DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
            }
            Text(
                "Última FC: " + (hr?.let { "${it.sample.bpm} bpm (${formatter.format(it.sample.time)})" } ?: "—")
            )
            hr?.dataOrigin?.let { origin ->
                Text("Fuente FC: ${origin.packageName}")
            }
        }
    }
}

/**
 * Abre Health Connect si está instalado o la ficha de Play Store si no.
 * (Aquí usamos la URI recomendada del provider).
 */
private fun openHealthConnectOrPlayStore(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = "market://details?id=com.google.android.apps.healthdata".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        // fallback web
        val web = Intent(Intent.ACTION_VIEW).apply {
            data = "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(web)
    }
}