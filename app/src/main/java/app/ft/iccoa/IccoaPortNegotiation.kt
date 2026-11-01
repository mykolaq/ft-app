package app.ft.iccoa

import app.ft.core.ProtoReader
import app.ft.core.ProtoWriter

/**
 * RAV4 OEM Carlink.apk ucar.proto ControlIndex methods 2/3.
 * Requests carry ServerType enum: SENSOR=1, AUDIO=2, UIBC=3.
 * Only the SENSOR port is mapped: FT binds this authenticated lane explicitly.
 */
internal object IccoaPortNegotiation {
    const val REQUEST = 2
    const val RESPONSE = 3
    const val SENSOR = 1
    const val SENSOR_PORT = 57239

    fun responseFor(request: ByteArray): ByteArray? {
        if (request.size !in 2..64) return null
        val r = ProtoReader(request)
        if (r.fields.keys != setOf(1)) return null
        val type = r.int(1, 0)
        if (type != SENSOR) return null
        return ProtoWriter().enum(1, 1).int32(2, SENSOR_PORT).toByteArray()
    }
}
