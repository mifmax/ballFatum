package ai.mifmax.balldefato.logic

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Accelerometer shake detection, ported verbatim from the original isShakeEnough().
 * Pure JVM logic (no Android dependency) so it can be unit-tested directly.
 */
class ShakeDetector {

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var shakeCount = 0

    /**
     * Feeds one accelerometer sample. Increments an internal counter whenever the
     * per-axis force exceeds [threshold]; returns true (and resets state) once that
     * counter passes [shakeCountLimit].
     */
    fun onSample(x: Float, y: Float, z: Float, threshold: Float, shakeCountLimit: Int): Boolean {
        var force = 0.0
        force += ((x - lastX) / GRAVITY_EARTH).toDouble().pow(2.0)
        force += ((y - lastY) / GRAVITY_EARTH).toDouble().pow(2.0)
        force += ((z - lastZ) / GRAVITY_EARTH).toDouble().pow(2.0)
        force = sqrt(force)

        lastX = x
        lastY = y
        lastZ = z

        if (force > threshold) {
            shakeCount++
            if (shakeCount > shakeCountLimit) {
                shakeCount = 0
                lastX = 0f
                lastY = 0f
                lastZ = 0f
                return true
            }
        }
        return false
    }

    private companion object {
        // Value of android.hardware.SensorManager.GRAVITY_EARTH (kept local to stay Android-free).
        const val GRAVITY_EARTH = 9.80665f
    }
}
