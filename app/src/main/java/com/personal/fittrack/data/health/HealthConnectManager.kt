package com.personal.fittrack.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.personal.fittrack.domain.calc.DateUtils
import java.time.LocalDate
import java.time.ZoneId

/**
 * Thin wrapper around the Health Connect SDK, scoped to exactly what this app needs: how many
 * calories did activity (walking, badminton, whatever - we deliberately don't care which) burn
 * on a given day. Every method fails soft - if Health Connect isn't installed or permission
 * hasn't been granted, callers just get back "no activity data" instead of an exception, since
 * this whole feature is additive on top of an app that already works fine without it.
 */
class HealthConnectManager(private val context: Context) {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    /** Pass this to `rememberLauncherForActivityResult` to build the permission-request launcher. */
    val permissionRequestContract = PermissionController.createRequestPermissionResultContract()

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient? by lazy {
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null
    }

    suspend fun hasPermissions(): Boolean {
        val connectedClient = client ?: return false
        return try {
            connectedClient.permissionController.getGrantedPermissions().containsAll(requiredPermissions)
        } catch (e: Exception) {
            // Health Connect throws (rather than returning false) for a handful of device/setup
            // issues outside this app's control - e.g. the manifest rationale activity being
            // misconfigured on a given OEM skin. Treat all of them as "not connected".
            false
        }
    }

    /** Sums active-calorie records per local calendar day over [startEpochDay]..[endEpochDay]
     * inclusive. Empty map if Health Connect is unavailable, not yet permitted, or the read
     * itself fails for any reason - this feature is additive, so a Health Connect hiccup
     * should never take the rest of the app down with it. */
    suspend fun activeCaloriesBurnedByDay(startEpochDay: Long, endEpochDay: Long): Map<Long, Int> {
        val connectedClient = client ?: return emptyMap()
        if (!hasPermissions()) return emptyMap()

        return try {
            val zone = ZoneId.systemDefault()
            val rangeStart = LocalDate.ofEpochDay(startEpochDay).atStartOfDay(zone).toInstant()
            val rangeEndExclusive = LocalDate.ofEpochDay(endEpochDay + 1).atStartOfDay(zone).toInstant()

            val response = connectedClient.readRecords(
                ReadRecordsRequest(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(rangeStart, rangeEndExclusive)
                )
            )

            response.records
                .groupBy { DateUtils.epochDayFor(it.startTime.toEpochMilli(), zone) }
                .mapValues { (_, records) -> records.sumOf { it.energy.inKilocalories }.toInt() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun activeCaloriesBurnedOn(epochDay: Long): Int =
        activeCaloriesBurnedByDay(epochDay, epochDay)[epochDay] ?: 0
}
