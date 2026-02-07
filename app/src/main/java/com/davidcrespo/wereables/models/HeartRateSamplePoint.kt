package com.davidcrespo.wereables.models

import java.time.Instant

data class HeartRateSamplePoint(
    val time: Instant,
    val bpm: Long
)