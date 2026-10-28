package app.ft.iccoa

import app.ft.core.ProtoWriter

/** ICCOA car microphone: 16 kHz mono PCM16, delivered on the audio lane as method 9. */
internal class IccoaMicrophone(
    private val sendControl: (Int, ByteArray) -> Boolean,
    private val onPcm: (ByteArray) -> Unit,
    private val log: (String) -> Unit = {}
) : AutoCloseable {
    private var wanted = false
    private var queued: Boolean? = null
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
        if (closed || queued == wanted || (!wanted && queued == null)) return false
        val body = ProtoWriter().int32(1, if (wanted) 1 else 0).int32(2, 16000)
            .int32(3, 1).int32(4, 2).toByteArray()
        if (!sendControl(6, body)) return true
        queued = wanted
        log("car microphone ${if (wanted) "on" else "off"} queued (16 kHz mono PCM16)")
        return false
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
