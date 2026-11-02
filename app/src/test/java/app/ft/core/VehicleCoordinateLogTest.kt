package app.ft.core

import org.junit.Assert.*
import org.junit.Test

class VehicleCoordinateLogTest {
    private val sample = VehicleLocation(
        source = "ICCOA",
        receivedAtMs = 13_000L,
        latitude = 55.755826,
        longitude = 37.617300,
        rawLatitude = 55755826,
        rawLongitude = 37617300,
        speedRaw = 34L
    )
    private val decision = VehicleLogSampler.Decision("FIX_REACQUIRED", true)

    @Test fun defaultOffIncludesHealthButNotCoordinates() {
        val log = VehicleCoordinateLog.format(sample, decision, false)
        assertTrue(log.contains("coordinates=OMITTED"))
        assertTrue(log.contains("reason=FIX_REACQUIRED"))
        assertTrue(log.contains("mode=EVENT_1HZ"))
        assertFalse(log.contains("55.755826"))
        assertFalse(log.contains("37.617300"))
        assertFalse(log.contains("55755826"))
        assertFalse(log.contains("37617300"))
        assertFalse(log.contains("latitude="))
        assertFalse(log.contains("rawLat="))
    }

    @Test fun ownerOptInIncludesExactCoordinates() {
        val log = VehicleCoordinateLog.format(sample, decision, true)
        assertTrue(log.contains("latitude=55.7558260"))
        assertTrue(log.contains("longitude=37.6173000"))
        assertTrue(log.contains("rawLat=55755826"))
        assertTrue(log.contains("rawLon=37617300"))
    }

    @Test fun oldVehicleGpsLinesAreRedactedAtExport() {
        val old = "11:11 I VehicleGPS: latitude=55.7558260 longitude=37.6173000 rawLat=55755826 rawLon=37617300 speedRaw=10"
        val redacted = VehicleCoordinateLog.scrubStoredLine(old)
        assertFalse(redacted.contains("55.755826"))
        assertFalse(redacted.contains("37.6173"))
        assertFalse(redacted.contains("55755826"))
        assertTrue(redacted.contains("speedRaw=10"))
        assertEquals("OTHER: latitude=55.7558260", VehicleCoordinateLog.scrubStoredLine("OTHER: latitude=55.7558260"))
    }
}
