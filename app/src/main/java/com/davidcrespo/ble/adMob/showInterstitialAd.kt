package com.davidcrespo.ble.adMob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.davidcrespo.ble.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

fun showInterstitialAd(context: Context) {
    val adRequest = AdRequest.Builder().build()
    
    InterstitialAd.load(context, BuildConfig.ADMOB_INTERSTITIALS, adRequest,
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                // Se muestra cuando esté listo
                interstitialAd.show(context as Activity)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d("AdMob", adError.message)
            }
        }
    )
}