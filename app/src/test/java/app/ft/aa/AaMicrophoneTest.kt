package app.ft.aa

import android.media.AudioRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class AaMicrophoneTest {
    @Test
    fun restartingDoesNotReviveAnOldRecorderOrDeliverItsPendingRead() {
        val oldReadStarted = CountDownLatch(1)
        val newReadStarted = CountDownLatch(1)
        val releaseOldRead = CountDownLatch(1)
        val releaseNewRead = CountDownLatch(1)
        val oldThread = AtomicReference<Thread>()
        val oldReads = AtomicInteger()
        val delivered = AtomicInteger()
        mockConstruction(AudioRecord::class.java) { recorder, construction ->
            val old = construction.count == 1
            `when`(recorder.state).thenReturn(AudioRecord.STATE_INITIALIZED)
            `when`(recorder.read(org.mockito.ArgumentMatchers.any(ByteArray::class.java), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer {
                if (old) {
                    oldThread.set(Thread.currentThread())
                    oldReads.incrementAndGet()
                    oldReadStarted.countDown()
                    check(releaseOldRead.await(5, TimeUnit.SECONDS))
                    2
                } else {
                    newReadStarted.countDown()
                    check(releaseNewRead.await(5, TimeUnit.SECONDS))
                    -1
                }
            }
        }.use {
            val microphone = AaMicrophone { delivered.incrementAndGet() }
            try {
                assertTrue(microphone.start())
                assertTrue(oldReadStarted.await(5, TimeUnit.SECONDS))
                microphone.stop()
                assertTrue(microphone.start())
                assertTrue(newReadStarted.await(5, TimeUnit.SECONDS))
                releaseOldRead.countDown()
                oldThread.get().join(5000)
                assertFalse(oldThread.get().isAlive)
                assertEquals(1, oldReads.get())
                assertEquals(0, delivered.get())
            } finally {
                microphone.stop()
                releaseOldRead.countDown()
                releaseNewRead.countDown()
            }
        }
    }

    @Test
    fun aRevokedPermissionReleasesTheRecorderAndLeavesTheMicrophoneInactive() {
        mockConstruction(AudioRecord::class.java) { recorder, _ ->
            `when`(recorder.state).thenReturn(AudioRecord.STATE_INITIALIZED)
            doThrow(SecurityException("permission revoked")).`when`(recorder).startRecording()
        }.use { records ->
            val microphone = AaMicrophone { error("no audio should be delivered") }
            assertFalse(microphone.start())
            assertFalse(microphone.active)
            verify(records.constructed().single()).release()
        }
    }
}
