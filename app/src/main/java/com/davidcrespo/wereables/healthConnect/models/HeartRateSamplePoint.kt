package com.davidcrespo.wereables.healthConnect.models

import java.time.Instant

data class HeartRateSamplePoint(
    val time: Instant,
    val bpm: Long
)