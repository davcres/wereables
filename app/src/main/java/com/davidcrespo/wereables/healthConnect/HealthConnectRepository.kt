package com.davidcrespo.wereables.healthConnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.davidcrespo.wereables.healthConnect.models.HealthConnectAvailability
import com.davidcrespo.wereables.healthConnect.models.HeartRateLatestResult
import com.davidcrespo.wereables.healthConnect.models.HeartRateSamplePoint
import com.davidcrespo.wereables.healthConnect.models.StepsTodayResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectRepository(private val context: Context) {

    private val client by lazy { HealthConnectClient.Companion.getOrCreate(context) }

    /**
     * Permisos que quieres pedir. Ajusta según tus necesidades.
     * Se recogen los datos de manera diferente para cada tipo de dato (n datos, n funciones).
     */
    val requiredPermissions: Set<String> = setOf(
        HealthPermission.Companion.getReadPermission(StepsRecord::class),
        HealthPermission.Companion.getReadPermission(HeartRateRecord::class)
    )

    /** Comprueba si el provider está disponible (instalado y usable). */
    fun availability(): HealthConnectAvailability {
        val status = HealthConnectClient.Companion.getSdkStatus(context)
        return when (status) {
            HealthConnectClient.Companion.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.Companion.SDK_UNAVAILABLE -> HealthConnectAvailability.Unavailable
            HealthConnectClient.Companion.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.ProviderUpdateRequired
            else -> HealthConnectAvailability.Unavailable
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    /**
     * Pasos totales de HOY (zona horaria del dispositivo).
     * Devuelve también los DataOrigin para que puedas indicar la fuente.
     */
    suspend fun getTodaySteps(): StepsTodayResult {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()

        val request = AggregateRequest(
            metrics = setOf(StepsRecord.Companion.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.Companion.between(start, end)
        )

        val result: AggregationResult = client.aggregate(request)
        val total = result[StepsRecord.Companion.COUNT_TOTAL] ?: 0L

        // Orígenes que contribuyeron a la agregación (útil para “fuente: Garmin/Strava/etc.”)
        val origins: Set<DataOrigin> = result.dataOrigins

        return StepsTodayResult(totalSteps = total, dataOrigins = origins)
    }

    /**
     * Última muestra de frecuencia cardíaca (bpm) en un rango (ej. últimas 24h).
     * HeartRateRecord tiene Samples dentro; aquí devolvemos el último sample.
     */
    suspend fun getLatestHeartRate(lastHours: Long = 24): HeartRateLatestResult? {
        val end = Instant.now()
        val start = end.minusSeconds(lastHours * 3600)

        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.Companion.between(start, end),
                pageSize = 50
            )
        )

        // Encontrar el último sample global entre registros
        var best: HeartRateSamplePoint? = null
        var bestOrigin: DataOrigin? = null

        for (record in response.records) {
            record.metadata.dataOrigin
            val lastSample = record.samples.maxByOrNull { it.time } ?: continue
            if (best == null || lastSample.time.isAfter(best.time)) {
                best = HeartRateSamplePoint(time = lastSample.time, bpm = lastSample.beatsPerMinute)
                bestOrigin = record.metadata.dataOrigin
            }
        }

        return best?.let { HeartRateLatestResult(sample = it, dataOrigin = bestOrigin) }
    }
}