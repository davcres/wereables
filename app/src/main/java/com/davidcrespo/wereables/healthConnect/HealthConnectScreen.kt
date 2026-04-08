package com.davidcrespo.wereables.healthConnect

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidcrespo.wereables.R
import com.davidcrespo.wereables.healthConnect.models.HealthConnectAvailability
import com.davidcrespo.wereables.healthConnect.models.HeartRateLatestResult
import com.davidcrespo.wereables.healthConnect.models.StepsTodayResult
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.health_connect_demo)) }) }
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
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.error_prefix, msg)) })
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
            Text(stringResource(R.string.availability), style = MaterialTheme.typography.titleMedium)

            val text = when (availability) {
                HealthConnectAvailability.Available -> stringResource(R.string.hc_available)
                HealthConnectAvailability.ProviderUpdateRequired -> stringResource(R.string.hc_update_required)
                HealthConnectAvailability.Unavailable -> stringResource(R.string.hc_unavailable)
            }
            Text(text)

            if (availability is HealthConnectAvailability.ProviderUpdateRequired) {
                Button(onClick = onOpenHealthConnect) {
                    Text(stringResource(R.string.open_install_hc))
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
            Text(stringResource(R.string.permissions), style = MaterialTheme.typography.titleMedium)
            Text(if (hasPermissions) stringResource(R.string.permissions_ok) else stringResource(R.string.missing_permissions))

            if (!hasPermissions) {
                Button(onClick = onRequestPermissions) {
                    Text(stringResource(R.string.grant_permissions))
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
                Text(stringResource(R.string.dashboard), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onRefresh, enabled = enabled) { Text(stringResource(R.string.refresh)) }
            }

            if (!enabled) {
                Text(stringResource(R.string.hc_dashboard_hint))
                return@Column
            }

            Text(stringResource(R.string.steps_today, steps?.totalSteps ?: "—"))
            steps?.dataOrigins?.takeIf { it.isNotEmpty() }?.let { origins ->
                Text(stringResource(R.string.steps_sources, origins.joinToString { it.packageName }))
            }

            val formatter = remember {
                DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
            }
            Text(
                hr?.let { 
                    stringResource(R.string.latest_hr_format, it.sample.bpm.toString(), formatter.format(it.sample.time))
                } ?: stringResource(R.string.latest_hr_format, "—", "—")
            )
            hr?.dataOrigin?.let { origin ->
                Text(stringResource(R.string.hr_source, origin.packageName))
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