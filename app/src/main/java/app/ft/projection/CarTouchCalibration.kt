package app.ft.projection

import android.content.Context
import app.ft.core.DiagLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * A calibration for the FT launcher / embedded browser only. Android Auto and
 * phone mirroring keep their existing (independent) touch transforms.
 */
internal data class TouchScale(val gain: Double, val offsetPx: Double) {
    fun map(x: Int, width: Int): Int =
        (x * gain + offsetPx).roundToInt().coerceIn(0, (width - 1).coerceAtLeast(0))

    companion object {
        fun fromTwoTargets(width: Int, measuredLeft: Int, measuredRight: Int): TouchScale? {
            if (width < 200 || measuredRight - measuredLeft < width / 4) return null
            val wantedLeft = width * 0.20
            val wantedRight = width * 0.80
            val gain = (wantedRight - wantedLeft) / (measuredRight - measuredLeft).toDouble()
            if (!gain.isFinite() || gain !in 0.60..1.80) return null
            val offset = wantedLeft - measuredLeft * gain
            if (!offset.isFinite() || kotlin.math.abs(offset) > width * 0.35) return null
            return TouchScale(gain, offset)
        }
    }
}

internal data class TouchCalibrationState(
    val step: Int = 0, val width: Int = 0, val height: Int = 0
)

/**
 * Reads actual HU UIBC touches on two visible FT targets; no arbitrary
 * per-phone offset. Calibration is keyed by virtual display geometry.
 */
internal object CarTouchCalibration {
    private val _state = MutableStateFlow(TouchCalibrationState())
    val state: StateFlow<TouchCalibrationState> = _state
    private var measuredLeft = -1
    private var swallowUntilUp = false

    fun begin(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        measuredLeft = -1
        swallowUntilUp = false
        _state.value = TouchCalibrationState(step = 1, width = width, height = height)
    }

    fun cancel() {
        measuredLeft = -1
        _state.value = TouchCalibrationState()
    }

    /** Intercept every UIBC touch while calibration targets are displayed. */
    fun consume(context: Context, action: Int, x: Int, y: Int): Boolean {
        val c = _state.value
        if (c.step !in 1..2) {
            if (!swallowUntilUp) return false
            if (action == CarTouch.UP) swallowUntilUp = false
            return true
        }
        if (action != CarTouch.DOWN) {
            if (action == CarTouch.UP) swallowUntilUp = false
            return true
        }
        swallowUntilUp = true
        // Upper-left corner offers a cancellation gesture without relying on hit testing.
        if (x < c.width / 9 && y < c.height / 6) {
            cancel()
            return true
        }
        if (c.step == 1) {
            measuredLeft = x
            _state.value = c.copy(step = 2)
        } else {
            val model = TouchScale.fromTwoTargets(c.width, measuredLeft, x)
            if (model == null) {
                DiagLog.w("ICCOA", "FT touch calibration needs two separated target taps; restart")
                measuredLeft = -1
                _state.value = c.copy(step = 1)
                return true
            }
            val key = "${c.width}x${c.height}"
            context.getSharedPreferences("ft-iccoa-touch", Context.MODE_PRIVATE).edit()
                .putFloat("${key}_gain", model.gain.toFloat())
                .putFloat("${key}_offset", model.offsetPx.toFloat())
                .apply()
            DiagLog.i("ICCOA", "FT touch calibration saved for $"+"key gain=${model.gain} offset=${model.offsetPx}")
            cancel()
        }
        return true
    }

    fun correct(context: Context, x: Int, width: Int, height: Int): Int {
        val prefs = context.getSharedPreferences("ft-iccoa-touch", Context.MODE_PRIVATE)
        val key = "${width}x${height}"
        val gain = prefs.getFloat("${key}_gain", 1f).toDouble()
        val offset = prefs.getFloat("${key}_offset", 0f).toDouble()
        if (!gain.isFinite() || gain !in 0.60..1.80 || !offset.isFinite() ||
            kotlin.math.abs(offset) > width * 0.35) return x
        return TouchScale(gain, offset).map(x, width)
    }
}
