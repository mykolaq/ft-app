package app.ft.iccoa

import app.ft.core.ProtoReader
import app.ft.core.VehicleTelemetry

/** OEM ucar.proto GearInfo: enum gear=1, uint32 speed=2, method SENSOR/6.
 * Car speed unit is not declared; preserve the wire integer unscaled.
 */
internal object IccoaGearWire {
    const val SENSOR_METHOD = 6
    fun accept(payload: ByteArray): Boolean {
        if (payload.isEmpty() || payload.size > 128) return false
        val fields = ProtoReader(payload)
        if (!fields.has(2) || !fields.fields.keys.all { it == 1 || it == 2 }) return false
        if (fields.has(1) && fields.long(1) !in 0L..4L) return false
        val raw = fields.long(2)
        if (raw !in 0L..100_000L) return false
        VehicleTelemetry.updateIccoaSpeed(raw)
        return true
    }
}
