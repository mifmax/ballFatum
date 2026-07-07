package ai.mifmax.balldefato.logic

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Accelerometer shake detection with a cooldown so one continuous shake yields exactly one
 * prediction.
 *
 * Once a shake fires, the detector *disarms*: further samples never fire again until the
 * device has been calm (force below threshold) for at least [cooldownMillis]. So holding a
 * shake keeps the found answer, and only stopping (for ≥ a second) arms the next one.
 *
 * Pure JVM logic (no Android dependency) — time is passed in via `nowMillis` so it stays
 * directly unit-testable.
 */
class ShakeDetector(private val cooldownMillis: Long = 1000L) {

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var shakeCount = 0
    private var armed = true
    private var lastTriggerMillis = 0L

    fun onSample(
        x: Float,
        y: Float,
        z: Float,
        threshold: Float,
        shakeCountLimit: Int,
        nowMillis: Long,
    ): Boolean {
        var force = 0.0
        force += ((x - lastX) / GRAVITY_EARTH).toDouble().pow(2.0)
        force += ((y - lastY) / GRAVITY_EARTH).toDouble().pow(2.0)
        force += ((z - lastZ) / GRAVITY_EARTH).toDouble().pow(2.0)
        force = sqrt(force)

        lastX = x
        lastY = y
        lastZ = z

        val shaking = force > threshold

        if (!armed) {
            // Re-arm only once the device is calm AND the cooldown has elapsed.
            if (!shaking && nowMillis - lastTriggerMillis >= cooldownMillis) {
                armed = true
                shakeCount = 0
            }
            return false
        }

        if (shaking) {
            shakeCount++
            if (shakeCount > shakeCountLimit) {
                shakeCount = 0
                lastX = 0f
                lastY = 0f
                lastZ = 0f
                lastTriggerMillis = nowMillis
                armed = false
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
