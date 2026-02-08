package com.davidcrespo.wereables

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.davidcrespo.wereables.healthConnect.HealthConnectScreen
import com.davidcrespo.wereables.healthConnect.HealthViewModel
import com.davidcrespo.wereables.ui.theme.WereablesTheme

class MainActivity : ComponentActivity() {

    private val healthViewModel: HealthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            WereablesTheme {
                HealthConnectScreen(vm = healthViewModel)
            }
        }
    }
}