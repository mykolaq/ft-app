package app.ft.aa

import app.ft.core.ProtoReader
import app.ft.core.VehicleLocation
import org.junit.Assert.*
import org.junit.Test

class AaCarGpsTest {
    private fun car(latitude: Double, longitude: Double, receivedAtMs: Long = 1_000L) =
        VehicleLocation(source = "ICCOA", receivedAtMs = receivedAtMs,
            latitude = latitude, longitude = longitude,
            coordinatesUnit = "ANDROID_LOCATION_WGS84")

    @Test fun iccoaToAndroidAutoUsesMicrodegreesAndRequiredFields() {
        val packet = AaCarGps.event(car(55.755826, 37.617299), 1_500L)!!
        val gps = ProtoReader(packet).message(1)!!
        assertEquals(55_755_826, gps.int(2))
        assertEquals(37_617_299, gps.int(3))
        assertFalse(gps.has(1))
        assertEquals(setOf(2, 3), gps.fields.keys)
    }

    @Test fun rejectMissingStaleAndUnverifiedCoordinateReference() {
        assertNull(AaCarGps.event(null, 1_500L))
        assertNull(AaCarGps.event(car(55.75, 37.62), 4_100L))
        assertNull(AaCarGps.event(car(91.0, 20.0), 1_500L))
        assertNull(AaCarGps.event(car(Double.NaN, 20.0), 1_500L))
        assertNull(AaCarGps.event(car(55.0, 180.1), 1_500L))
        assertNull(AaCarGps.event(car(55.0, 20.0).copy(coordinatesUnit = "UNKNOWN"), 1_500L))
        assertNull(AaCarGps.event(car(55.0, 20.0).copy(source = "BAIDU"), 1_500L))
    }

    @Test fun serviceDiscoveryAdvertisesCarLocationWithOtherSensors() {
        val response = AaMessages.serviceDiscoveryResponse(
            1280, 720, 30, 160, true, "RAV4", true)
        val config = ProtoReader(response).messages(1)
            .first { it.int(1) == AaProtocol.CH_SENSOR }.message(2)!!
        val types = config.messages(1).map { it.int(1) }.toSet()
        assertTrue(AaProtocol.SENSOR_LOCATION in types)
        assertTrue(AaProtocol.SENSOR_NIGHT_MODE in types)
        assertTrue(AaProtocol.SENSOR_DRIVING_STATUS in types)
    }

    @Test fun gpsSensorErrorMatchesAndroidAutoWireSchema() {
        val p = ProtoReader(AaCarGps.sensorError(2))
        assertEquals(1, p.int(1))
        assertEquals(2, p.int(2))
    }
}
