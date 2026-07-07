package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ShakeDetectorTest {

    private val t = 1.0f
    private val limit = 3

    @Test
    fun `weak samples never trigger a shake`() {
        val detector = ShakeDetector()
        repeat(10) { i ->
            val triggered = detector.onSample(0.1f, 0.1f, 0.1f, 1000f, limit, nowMillis = i.toLong())
            assertThat(triggered).isFalse()
        }
    }

    @Test
    fun `shake triggers once the count exceeds the limit`() {
        val detector = ShakeDetector()
        assertThat(detector.onSample(100f, 0f, 0f, t, limit, 0)).isFalse() // count 1
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 0)).isFalse()   // count 2
        assertThat(detector.onSample(100f, 0f, 0f, t, limit, 0)).isFalse() // count 3
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 0)).isTrue()    // count 4 > 3 -> shake
    }

    @Test
    fun `continuous shaking after a trigger does not produce a second answer`() {
        val detector = ShakeDetector()
        // First trigger at now = 0.
        detector.onSample(100f, 0f, 0f, t, limit, 0)
        detector.onSample(0f, 0f, 0f, t, limit, 0)
        detector.onSample(100f, 0f, 0f, t, limit, 0)
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 0)).isTrue()

        // Keep shaking — even past the cooldown, it never re-fires while motion continues.
        assertThat(detector.onSample(100f, 0f, 0f, t, limit, 500)).isFalse()
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 900)).isFalse()
        assertThat(detector.onSample(100f, 0f, 0f, t, limit, 1500)).isFalse()
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 2000)).isFalse()
    }

    @Test
    fun `re-arms only after calm plus cooldown, then can trigger again`() {
        val detector = ShakeDetector()
        // Trigger at now = 0; state resets, detector disarms.
        detector.onSample(100f, 0f, 0f, t, limit, 0)
        detector.onSample(0f, 0f, 0f, t, limit, 0)
        detector.onSample(100f, 0f, 0f, t, limit, 0)
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 0)).isTrue()

        // Calm before cooldown elapses: still disarmed.
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 500)).isFalse()
        // Calm after cooldown: re-arms (this sample itself does not fire).
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 1000)).isFalse()

        // A fresh shake now yields a new answer.
        assertThat(detector.onSample(100f, 0f, 0f, t, limit, 1100)).isFalse()
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 1110)).isFalse()
        assertThat(detector.onSample(100f, 0f, 0f, t, limit, 1120)).isFalse()
        assertThat(detector.onSample(0f, 0f, 0f, t, limit, 1130)).isTrue()
    }
}
