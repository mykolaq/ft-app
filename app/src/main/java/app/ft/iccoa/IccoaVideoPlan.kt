package app.ft.iccoa

import app.ft.projection.VideoPlan

/** Keep the receiver's geometry, but avoid encoding duplicate AA frames at 60 Hz. */
internal object IccoaVideoPlan {
    fun from(config: IccoaVideoConfig): VideoPlan {
        val width = config.width and -2
        val height = config.height and -2
        return VideoPlan(width, height, width, height, config.fps.coerceIn(1, 30),
            8_000_000, constantBitrate = true)
    }
}
