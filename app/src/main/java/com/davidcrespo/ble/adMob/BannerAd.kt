package com.davidcrespo.ble.adMob

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.davidcrespo.ble.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // ID de test para banners
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNERS
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}