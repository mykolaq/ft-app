package app.ft.iccoa

import app.ft.projection.VideoPlan
import org.junit.Assert.*
import org.junit.Test

class IccoaVideoPlanTest {
    @Test fun xa60KeepsGeometryButCapsDuplicateFramesAndUsesCbr() {
        val plan = IccoaVideoPlan.from(IccoaVideoConfig("127.0.0.1", 2422, 1272, 60))
        assertEquals(2422, plan.contentWidth)
        assertEquals(1272, plan.contentHeight)
        assertEquals(plan.contentWidth, plan.streamWidth)
        assertEquals(plan.contentHeight, plan.streamHeight)
        assertEquals(30, plan.fps)
        assertEquals(8_000_000, plan.bitrate)
        assertTrue(plan.constantBitrate)
    }

    @Test fun lowerReceiverRatesAreNotRaised() {
        for (fps in listOf(15, 24, 25, 30)) {
            assertEquals(fps, IccoaVideoPlan.from(IccoaVideoConfig("127.0.0.1", fps = fps)).fps)
        }
    }

    @Test fun existingTransportPlansRetainTheirBitrateMode() {
        assertFalse(VideoPlan(1920, 1080, 1280, 720, 60, 3_000_000).constantBitrate)
    }
}
