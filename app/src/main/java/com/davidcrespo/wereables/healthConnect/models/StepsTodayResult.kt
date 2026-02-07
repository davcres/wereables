package com.davidcrespo.wereables.healthConnect.models

import androidx.health.connect.client.records.metadata.DataOrigin

data class StepsTodayResult(
    val totalSteps: Long,
    val dataOrigins: Set<DataOrigin>
)