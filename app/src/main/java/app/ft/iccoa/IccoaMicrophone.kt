package app.ft.iccoa

import app.ft.core.ProtoWriter

/** ICCOA car microphone: 16 kHz mono PCM16, delivered on the audio lane as method 9. */
internal class IccoaMicrophone(
    private val sendControl: (Int, ByteArray) -> Boolean,
    private val onPcm: (ByteArray) -> Unit,
    private val log: (String) -> Unit = {},
    private val phoneStateBody: (Boolean) -> ByteArray = { voice ->
        ProtoWriter().bool(4, voice).toByteArray()
    }
) : AutoCloseable {
    private var wanted = false
    private var microphoneQueued = false
    private var voiceQueued = false
    private var closed = false
    private var packets = 0L
    private var bytes = 0L
    private var peak = 0

    @Synchronized fun setWanted(value: Boolean) {
        if (closed) return
        if (wanted && !value) summary()
        if (!wanted && value) { packets = 0; bytes = 0; peak = 0 }
        wanted = value
        flush()
    }

    /** Retry an unqueued state when a control lane becomes available. */
    @Synchronized fun flush(): Boolean {
        if (closed) return false
        // The reference announces voice before microphone ON, and clears it after OFF.
        // Track both enqueued messages so retries preserve order without replaying a
        // cancelled start or duplicating the successful half of a transition.
        return if (wanted) !queueVoice(true) || !queueMicrophone(true)
        else !queueMicrophone(false) || !queueVoice(false)
    }

    private fun queueVoice(value: Boolean): Boolean {
        if (voiceQueued == value) return true
        val body = try { phoneStateBody(value) } catch (e: RuntimeException) {
            log("car voice state unavailable: ${e.javaClass.simpleName}")
            return false
        }
        if (!sendControl(9, body)) return false
        voiceQueued = value
        log("car voice ${if (value) "on" else "off"} queued")
        return true
    }

    private fun queueMicrophone(value: Boolean): Boolean {
        if (microphoneQueued == value) return true
        val body = ProtoWriter().int32(1, if (value) 1 else 0).int32(2, 16000)
            .int32(3, 1).int32(4, 2).toByteArray()
        if (!sendControl(6, body)) return false
        microphoneQueued = value
        log("car microphone ${if (value) "on" else "off"} queued (16 kHz mono PCM16)")
        return true
    }

    fun receive(method: Int, pcm: ByteArray) {
        synchronized(this) {
            if (closed || !wanted || method != 9 || pcm.isEmpty() || pcm.size % 2 != 0) return
            packets++
            bytes += pcm.size
            for (i in pcm.indices step 2) {
                val sample = ((pcm[i].toInt() and 255) or (pcm[i + 1].toInt() shl 8)).toShort().toInt()
                peak = maxOf(peak, kotlin.math.abs(sample))
            }
            if (packets == 1L) log("car microphone first PCM packet: ${pcm.size} bytes, peak=$peak")
        }
        // This runs on the separate ICCOA audio reader, never on the main/UI thread.
        onPcm(pcm)
    }

    private fun summary() { log("car microphone input: packets=$packets bytes=$bytes peak=$peak") }

    @Synchronized override fun close() {
        if (closed) return
        setWanted(false)
        closed = true
    }
}
