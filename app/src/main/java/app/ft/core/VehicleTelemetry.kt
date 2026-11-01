package app.ft.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Shared, protocol-neutral vehicle sensors for FT widgets and later AA location forwarding.
 * No coordinate datum, speed multiplier or heading unit is guessed.
 */
data class VehicleLocation(
    val source: String,
    val receivedAtMs: Long,
    val positionAtMs: Long = receivedAtMs,
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

    fun positionFresh(nowMs: Long = System.currentTimeMillis()) =
        positionAtMs > 0L && nowMs >= positionAtMs && nowMs - positionAtMs <= 3_000L

    val displayLatitude: Double? get() = latitude?.takeIf { it.isFinite() && it in -90.0..90.0 }
    val displayLongitude: Double? get() = longitude?.takeIf { it.isFinite() && it in -180.0..180.0 }
}

/** Both Baidu CarLife and ICCOA publish here. All consumers observe one stable state. */
object VehicleTelemetry {
    private val _current = MutableStateFlow<VehicleLocation?>(null)
    val current: StateFlow<VehicleLocation?> = _current
    private var lastLogAtMs = 0L
    private var lastLogSource = ""

    /** One time-correlated sample per second, embedded in FT's normal manual ZIP report. */
    private fun note(sample: VehicleLocation) {
        val now = sample.receivedAtMs
        if (sample.source == lastLogSource && now - lastLogAtMs < 1_000L) return
        lastLogAtMs = now
        lastLogSource = sample.source
        fun dec(value: Double?) = value?.let { String.format(java.util.Locale.US, "%.7f", it) } ?: "NA"
        runCatching { DiagLog.i("VehicleGPS", "protocol=${sample.source} timeMs=$now " +
            "latitude=${dec(sample.latitude)} longitude=${dec(sample.longitude)} " +
            "rawLat=${sample.rawLatitude ?: "NA"} rawLon=${sample.rawLongitude ?: "NA"} " +
            "speedRaw=${sample.speedRaw ?: "NA"} headingRaw=${sample.headingRaw ?: "NA"} " +
            "satUsed=${sample.satellitesUsed ?: "NA"} satVisible=${sample.satellitesVisible ?: "NA"} " +
            "carTimestamp=${sample.deviceTimestampRaw ?: "NA"} " +
            "positionAgeMs=${if (sample.positionAtMs == 0L) -1 else now - sample.positionAtMs} " +
            "units=${sample.coordinatesUnit}/${sample.speedUnit}/${sample.headingUnit}") }
    }

    @Synchronized fun publish(location: VehicleLocation) {
        require(location.source == "ICCOA" || location.source == "BAIDU")
        _current.value = location
        note(location)
    }

    @Synchronized fun updateBaiduSpeed(rawSpeed: Long, deviceTimestamp: Long? = null) {
        if (rawSpeed < 0L || rawSpeed > 100_000L) return
        val old = _current.value?.takeIf { it.source == "BAIDU" && it.fresh() }
        _current.value = (old ?: VehicleLocation("BAIDU", System.currentTimeMillis(), positionAtMs = 0L)).copy(
            receivedAtMs = System.currentTimeMillis(), speedRaw = rawSpeed,
            deviceTimestampRaw = deviceTimestamp ?: old?.deviceTimestampRaw
        )
        _current.value?.let(::note)
    }

    @Synchronized fun updateIccoaSpeed(rawSpeed: Long) {
        if (rawSpeed < 0L || rawSpeed > 100_000L) return
        val now = System.currentTimeMillis()
        val old = _current.value?.takeIf { it.source == "ICCOA" && it.fresh(now) }
        _current.value = (old ?: VehicleLocation("ICCOA", now, positionAtMs = 0L)).copy(
            receivedAtMs = now, speedRaw = rawSpeed
        )
        _current.value?.let(::note)
    }

    @Synchronized fun clear(source: String) {
        if (_current.value?.source == source) _current.value = null
    }
}
