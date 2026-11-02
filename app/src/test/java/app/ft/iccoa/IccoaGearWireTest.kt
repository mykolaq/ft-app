package app.ft.iccoa

import app.ft.core.ProtoWriter
import app.ft.core.VehicleTelemetry
import org.junit.Assert.*
import org.junit.Test

class IccoaGearWireTest {
    @Test fun acceptsDirectGearInfo() {
        val bytes = ProtoWriter().int32(1, 2).uint32(2, 61).toByteArray()
        assertTrue(IccoaGearWire.accept(bytes))
        assertEquals("ICCOA", VehicleTelemetry.current.value?.source)
        assertEquals(61L, VehicleTelemetry.current.value?.speedRaw)
        assertEquals(0L, VehicleTelemetry.current.value?.positionAtMs)
        assertNull(VehicleTelemetry.current.value?.speedKmh)
        VehicleTelemetry.clear("ICCOA")
    }
    @Test fun rejectsUnexpectedOrMalformedSensorPackets() {
        assertFalse(IccoaGearWire.accept(byteArrayOf()))
        assertFalse(IccoaGearWire.accept(ProtoWriter().int32(1, 5).uint32(2, 50).toByteArray()))
        assertFalse(IccoaGearWire.accept(ProtoWriter().uint32(2, 50).uint32(3, 1).toByteArray()))
        assertFalse(IccoaGearWire.accept(ProtoWriter().uint32(2, 100_001).toByteArray()))
    }
}
