package com.davidcrespo.wereables.data

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

class RemoteConfigManager {

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 hora en producción
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Opcional: Valores por defecto locales
        // remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
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