package app.ft.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Shared, protocol-neutral vehicle sensors for FT widgets and later AA location forwarding.
 * No coordinate datum, speed multiplier or heading unit is guessed.
 */
data class VehicleLocation(
    val source: String,
    val receivedAtMs: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rawLatitude: Long? = null,
    val rawLongitude: Long? = null,
    val speedRaw: Long? = null,
    val headingRaw: Long? = null,
    val speedKmh: Float? = null,
    val headingDegrees: Float? = null,
    val satellitesUsed: Long? = null,
    val satellitesVisible: Long? = null,
    val deviceTimestampRaw: Long? = null,
    val coordinatesUnit: String = "UNKNOWN",
    val speedUnit: String = "UNKNOWN",
    val headingUnit: String = "UNKNOWN"
) {
    fun fresh(nowMs: Long = System.currentTimeMillis()) =
        nowMs >= receivedAtMs && nowMs - receivedAtMs <= 3_000L

    val displayLatitude: Double? get() = latitude?.takeIf { it.isFinite() && it in -90.0..90.0 }
    val displayLongitude: Double? get() = longitude?.takeIf { it.isFinite() && it in -180.0..180.0 }
}

/** Both Baidu CarLife and ICCOA publish here. All consumers observe one stable state. */
object VehicleTelemetry {
    private val _current = MutableStateFlow<VehicleLocation?>(null)
    val current: StateFlow<VehicleLocation?> = _current

    @Synchronized fun publish(location: VehicleLocation) {
        require(location.source == "ICCOA" || location.source == "BAIDU")
        _current.value = location
    }

    @Synchronized fun updateBaiduSpeed(rawSpeed: Long, deviceTimestamp: Long? = null) {
        if (rawSpeed < 0L || rawSpeed > 100_000L) return
        val old = _current.value?.takeIf { it.source == "BAIDU" && it.fresh() }
        _current.value = (old ?: VehicleLocation("BAIDU", System.currentTimeMillis())).copy(
            receivedAtMs = System.currentTimeMillis(), speedRaw = rawSpeed,
            deviceTimestampRaw = deviceTimestamp ?: old?.deviceTimestampRaw
        )
    }

    @Synchronized fun clear(source: String) {
        if (_current.value?.source == source) _current.value = null
    }
}
