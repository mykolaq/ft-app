package app.ft.iccoa

import app.ft.core.ProtoReader
import app.ft.core.ProtoWriter
import org.junit.Assert.*
import org.junit.Test

class IccoaPortNegotiationTest {
    @Test fun knownSensorRequestReturnsBoundPort() {
        val request = ProtoWriter().enum(1, 1).toByteArray()
        val response = IccoaPortNegotiation.responseFor(request)!!
        val parsed = ProtoReader(response)
        assertEquals(1, parsed.int(1))
        assertEquals(57239, parsed.int(2))
    }
    @Test fun unknownTypesAreNotAssignedUnverifiedPorts() {
        assertNull(IccoaPortNegotiation.responseFor(ProtoWriter().enum(1, 2).toByteArray()))
        assertNull(IccoaPortNegotiation.responseFor(ProtoWriter().enum(1, 3).toByteArray()))
        assertNull(IccoaPortNegotiation.responseFor(ByteArray(0)))
        assertNull(IccoaPortNegotiation.responseFor(ProtoWriter().enum(1, 1).bool(2, true).toByteArray()))
    }
}
