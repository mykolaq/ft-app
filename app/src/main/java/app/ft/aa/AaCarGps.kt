package app.ft.aa

import app.ft.core.ProtoWriter
import app.ft.core.VehicleLocation
import kotlin.math.roundToInt

/**
 * Android Auto SENSOR_LOCATION bridge for verified decimal-degree car fixes.
 *
 * The source must explicitly declare Android Location API / WGS84 provenance.
 * Baidu integer GPS is NOT forwarded until its scale/CRS is verified.
 * No guessed speed, bearing, accuracy, altitude or Dead Reckoning flags are sent.
 */
internal object AaCarGps {
    const val LOCATION = AaProtocol.SENSOR_LOCATION
    private const val CRS = "ANDROID_LOCATION_WGS84"

    fun event(location: VehicleLocation?, nowMs: Long = System.currentTimeMillis()): ByteArray? {
        if (location == null || location.source != "ICCOA" || location.coordinatesUnit != CRS ||
            !location.positionFresh(nowMs)) return null
        val latitude = location.displayLatitude ?: return null
        val longitude = location.displayLongitude ?: return null
        // Android Auto GPSLocationData: repeated field 1; sub-fields 2 & 3 are required
        // int32 microdegrees, NOT ICCOA's double or Baidu's raw int32.
        val latMicro = (latitude * 1_000_000.0).roundToInt()
        val lonMicro = (longitude * 1_000_000.0).roundToInt()
        return ProtoWriter().message(1, ProtoWriter()
            .int32(2, latMicro)
            .int32(3, lonMicro)
        ).toByteArray()
    }

    fun sensorError(status: Int): ByteArray =
        ProtoWriter().enum(1, LOCATION).enum(2, status).toByteArray()
}
