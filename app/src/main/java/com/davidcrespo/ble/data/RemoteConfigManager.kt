package com.davidcrespo.ble.data

import com.davidcrespo.ble.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.davidcrespo.ble.R

class RemoteConfigManager {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Valores por defecto locales
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun isBannerEnabled(): Boolean = remoteConfig.getBoolean("showBanners")
    fun isFullScreenAdsEnabled(): Boolean = remoteConfig.getBoolean("showFullScreenAds")
}