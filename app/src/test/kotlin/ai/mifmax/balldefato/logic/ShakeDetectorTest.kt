package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ShakeDetectorTest {

    @Test
    fun `weak samples never trigger a shake`() {
        val detector = ShakeDetector()
        repeat(10) {
            val triggered = detector.onSample(0.1f, 0.1f, 0.1f, threshold = 1000f, shakeCountLimit = 3)
            assertThat(triggered).isFalse()
        }
    }

    @Test
    fun `shake triggers once the count exceeds the limit`() {
        val detector = ShakeDetector()
        val t = 1.0f
        val limit = 3
        // Each transition (rest <-> strong jolt) far exceeds the threshold.
        assertThat(detector.onSample(100f, 0f, 0f, t, limit)).isFalse() // count 1
        assertThat(detector.onSample(0f, 0f, 0f, t, limit)).isFalse()   // count 2
        assertThat(detector.onSample(100f, 0f, 0f, t, limit)).isFalse() // count 3
        assertThat(detector.onSample(0f, 0f, 0f, t, limit)).isTrue()    // count 4 > 3 -> shake
    }

    @Test
    fun `state resets after a shake`() {
        val detector = ShakeDetector()
        val t = 1.0f
        val limit = 3
        detector.onSample(100f, 0f, 0f, t, limit)
        detector.onSample(0f, 0f, 0f, t, limit)
        detector.onSample(100f, 0f, 0f, t, limit)
        assertThat(detector.onSample(0f, 0f, 0f, t, limit)).isTrue()
        // Counter reset to 0: a single strong sample must not trigger again.
        assertThat(detector.onSample(100f, 0f, 0f, t, limit)).isFalse()
    }
}
