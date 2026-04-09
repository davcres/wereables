package com.davidcrespo.ble

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davidcrespo.ble.ble.scanner.BleScanViewModel
import com.davidcrespo.ble.ble.scanner.ThermometerBleScanScreen
import com.davidcrespo.ble.ble.emulator.ThermometerBleServerScreen
import com.davidcrespo.ble.healthConnect.HealthConnectScreen
import com.davidcrespo.ble.healthConnect.HealthViewModel
import com.davidcrespo.ble.ui.theme.BleTheme
import com.google.android.gms.ads.MobileAds
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.davidcrespo.ble.adMob.BannerAd
import com.davidcrespo.ble.adMob.showInterstitialAd
import android.os.Bundle

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val healthViewModel: HealthViewModel by viewModels()
    private val bleScanViewModel: BleScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        enableEdgeToEdge()
        setContent {
            BleTheme {
                val context = LocalContext.current
                var selectedScreen by remember { mutableStateOf(0) }
                // Obtenemos el estado de Remote Config aunque por ahora solo lo recuperamos al inicio
                val isConfigReady by mainViewModel.isConfigReady.collectAsStateWithLifecycle()

                LaunchedEffect(isConfigReady) {
                    if (isConfigReady && mainViewModel.isFullScreenAdsEnabled()) {
                        showInterstitialAd(context)
                    }
                }

                Scaffold(
                    bottomBar = {
                        Column {
                            if (isConfigReady && mainViewModel.isBannerEnabled()) {
                                BannerAd()
                            }
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedScreen == 0,
                                    onClick = { selectedScreen = 0 },
                                    icon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.List,
                                            contentDescription = stringResource(R.string.nav_scanner)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_scanner)) }
                                )
                                NavigationBarItem(
                                    selected = selectedScreen == 1,
                                    onClick = { selectedScreen = 1 },
                                    icon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.nav_emulator)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_emulator)) }
                                )
                                NavigationBarItem(
                                    selected = selectedScreen == 2,
                                    onClick = { selectedScreen = 2 },
                                    icon = {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = stringResource(R.string.nav_health_connect)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_health_connect)) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        if (selectedScreen == 0) {
                            ThermometerBleScanScreen(vm = bleScanViewModel)
                        } else if (selectedScreen == 1) {
                            ThermometerBleServerScreen()
                        } else {
                            HealthConnectScreen(vm = healthViewModel)
                        }
                    }
                }
            }
        }
    }
}