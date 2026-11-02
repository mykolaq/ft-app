package app.ft.iccoa

import app.ft.core.NavigationInfo
import app.ft.core.ProtoReader
import org.junit.Assert.*
import org.junit.Test

class IccoaNavigationTest {
    @Test fun activeNavigationTellsCarLinkSensorManagerToStart() {
        val p = ProtoReader(IccoaNavigation.encode(NavigationInfo(
            active = true, road = "Tverskaya", distanceMeters = 850)))
        assertTrue(p.bool(1))
        assertEquals("850", p.string(3))
        assertEquals("m", p.string(4))
        assertEquals("Tverskaya", p.string(6))
        assertEquals(11, IccoaNavigation.CONTROL_METHOD)
    }
    @Test fun inactiveNavigationTellsCarLinkSensorManagerToStop() {
        val p = ProtoReader(IccoaNavigation.encode(NavigationInfo(active = false)))
        assertFalse(p.bool(1))
    }
}
