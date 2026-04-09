package com.davidcrespo.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davidcrespo.ble.data.RemoteConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val remoteConfigManager = RemoteConfigManager()

    private val _isConfigReady = MutableStateFlow(false)
    val isConfigReady: StateFlow<Boolean> = _isConfigReady

    init {
        fetchRemoteConfig()
    }

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            remoteConfigManager.fetchAndActivate { success ->
                _isConfigReady.value = success
            }
        }
    }

    fun isBannerEnabled(): Boolean = remoteConfigManager.isBannerEnabled()
    fun isFullScreenAdsEnabled(): Boolean = remoteConfigManager.isFullScreenAdsEnabled()
}
