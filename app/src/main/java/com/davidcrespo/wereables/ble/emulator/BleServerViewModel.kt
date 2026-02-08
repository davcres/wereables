package com.davidcrespo.wereables.ble.emulator

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BleServerViewModel(private val server: MultiProfileBleServer) : ViewModel() {
    val isAdvertising = server.isAdvertising
    
    // States for various device types
    val selectedProfile = mutableStateOf(BleProfile.THERMOMETER)
    
    val temperature = mutableStateOf(36.5f)
    val heartRate = mutableStateOf(70f)
    val systolic = mutableStateOf(120f)
    val diastolic = mutableStateOf(80f)
    val glucose = mutableStateOf(100f)
    val spo2 = mutableStateOf(98f)
    val pulseRate = mutableStateOf(70f)

    fun toggleServer(enable: Boolean) {
        if (enable) server.startServer(selectedProfile.value) else server.stopServer()
    }

    fun updateValues() {
        if (!isAdvertising.value) return
        
        when (selectedProfile.value) {
            BleProfile.THERMOMETER -> server.updateData(temperature.value)
            BleProfile.HEART_RATE -> server.updateData(heartRate.value)
            BleProfile.BLOOD_PRESSURE -> server.updateData(systolic.value, diastolic.value, 90f) // MAP approx
            BleProfile.GLUCOSE -> server.updateData(glucose.value)
            BleProfile.PULSE_OXIMETER -> server.updateData(spo2.value, pulseRate.value)
        }
    }
    
    fun setProfile(p: BleProfile) {
        if (isAdvertising.value) toggleServer(false)
        selectedProfile.value = p
    }
    
    override fun onCleared() {
        super.onCleared()
        server.stopServer()
    }
}

class BleServerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BleServerViewModel(MultiProfileBleServer(context)) as T
    }
}