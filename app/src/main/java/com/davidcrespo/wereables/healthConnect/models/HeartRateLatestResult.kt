package com.davidcrespo.wereables.healthConnect.models

import androidx.health.connect.client.records.metadata.DataOrigin

data class HeartRateLatestResult(
    val sample: HeartRateSamplePoint,
    val dataOrigin: DataOrigin?
)