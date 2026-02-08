package com.davidcrespo.wereables

import android.os.Bundle
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
import com.davidcrespo.wereables.ble.scanner.BleScanViewModel
import com.davidcrespo.wereables.ble.scanner.ThermometerBleScanScreen
import com.davidcrespo.wereables.ble.emulator.ThermometerBleServerScreen
import com.davidcrespo.wereables.healthConnect.HealthConnectScreen
import com.davidcrespo.wereables.healthConnect.HealthViewModel
import com.davidcrespo.wereables.ui.theme.WereablesTheme

class MainActivity : ComponentActivity() {

    private val healthViewModel: HealthViewModel by viewModels()
    private val bleScanViewModel: BleScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            WereablesTheme {
                var selectedScreen by remember { mutableStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedScreen == 0,
                                onClick = { selectedScreen = 0 },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Scanner") },
                                label = { Text("Scanner") }
                            )
                            NavigationBarItem(
                                selected = selectedScreen == 1,
                                onClick = { selectedScreen = 1 },
                                icon = { Icon(Icons.Default.Edit, contentDescription = "Peripheral") },
                                label = { Text("Emulador") }
                            )
                            NavigationBarItem(
                                selected = selectedScreen == 2,
                                onClick = { selectedScreen = 2 },
                                icon = { Icon(Icons.Default.Favorite, contentDescription = "Health Connect") },
                                label = { Text("Health Connect") }
                            )
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