package app.ft.iccoa

import app.ft.core.ProtoReader
import org.junit.Assert.*
import org.junit.Test

class IccoaMicrophoneTest {
    @Test fun microphoneRequestUsesReferencePcmFormatAndStopsOnClose() {
        val states = mutableListOf<Int>()
        val mic = IccoaMicrophone({ method, bytes ->
            assertEquals(6, method)
            val fields = ProtoReader(bytes)
            states += fields.int(1)
            assertEquals(16000, fields.int(2))
            assertEquals(1, fields.int(3))
            assertEquals(2, fields.int(4))
            true
        }, {})
        mic.setWanted(true)
        mic.setWanted(true)
        mic.close()
        mic.close()
        mic.setWanted(true)
        assertEquals(listOf(1, 0), states)
    }

    @Test fun onlyMicrophonePcmWhileListeningReachesAndroidAuto() {
        val received = mutableListOf<ByteArray>()
        val mic = IccoaMicrophone({ _, _ -> true }, { received += it })
        val pcm = byteArrayOf(1, 0, 0, -128)
        mic.receive(9, pcm)
        mic.setWanted(true)
        mic.receive(8, pcm)
        mic.receive(9, byteArrayOf())
        mic.receive(9, byteArrayOf(1))
        mic.receive(9, pcm)
        mic.setWanted(false)
        mic.receive(9, pcm)
        mic.close()
        mic.receive(9, pcm)
        assertEquals(1, received.size)
        assertArrayEquals(pcm, received.single())
    }

    @Test fun unavailableControlLaneRetriesLatestStateAndCloseCancelsRetries() {
        var available = false
        val states = mutableListOf<Int>()
        val mic = IccoaMicrophone({ _, body ->
            if (available) states += ProtoReader(body).int(1)
            available
        }, {})
        mic.setWanted(true)
        assertTrue(mic.flush())
        available = true
        assertFalse(mic.flush())
        assertEquals(listOf(1), states)
        available = false
        mic.setWanted(false)
        assertTrue(mic.flush())
        mic.close()
        available = true
        assertFalse(mic.flush())
        assertEquals(listOf(1), states)
    }

    @Test fun closingAnUnusedMicrophoneDoesNotSendAnyRequests() {
        val mic = IccoaMicrophone({ _, _ -> fail("Unused microphone"); true }, {})
        mic.close()
    }

    @Test fun cancelledUnqueuedStartIsNotReplayedWhenLaneAppears() {
        var available = false
        val states = mutableListOf<Int>()
        val mic = IccoaMicrophone({ _, body ->
            if (available) states += ProtoReader(body).int(1)
            available
        }, {})
        mic.setWanted(true)
        mic.setWanted(false)
        available = true
        assertFalse(mic.flush())
        assertTrue(states.isEmpty())
        mic.setWanted(true)
        assertEquals(listOf(1), states)
    }

    @Test fun inputDiagnosticsDistinguishSilenceFromPcmSignal() {
        val logs = mutableListOf<String>()
        val mic = IccoaMicrophone({ _, _ -> true }, {}, logs::add)
        mic.setWanted(true)
        mic.receive(9, byteArrayOf(0, 0))
        mic.receive(9, byteArrayOf(0, -128, -1, 127))
        mic.setWanted(false)
        assertTrue(logs.any { it.contains("first PCM packet: 2 bytes, peak=0") })
        assertTrue(logs.any { it.contains("packets=2 bytes=6 peak=32768") })
        mic.setWanted(true)
        mic.setWanted(false)
        assertTrue(logs.last().contains("off queued"))
        assertTrue(logs.any { it.contains("packets=0 bytes=0 peak=0") })
    }
}
