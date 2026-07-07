package ai.mifmax.balldefato

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// Pin to API 26: exercises VibrationEffect.createOneShot via the plain VIBRATOR_SERVICE
// path (well-supported ShadowVibrator), avoiding VibratorManager indirection.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class VibrationsTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun oneShot_withPositiveDuration_vibrates() {
        val vibrator = Vibrations.defaultVibrator(context)
        Vibrations.oneShot(vibrator, 200L)
        assertThat(shadowOf(vibrator).isVibrating).isTrue()
    }

    @Test
    fun oneShot_withZeroDuration_doesNothing() {
        val vibrator = Vibrations.defaultVibrator(context)
        Vibrations.oneShot(vibrator, 0L)
        assertThat(shadowOf(vibrator).isVibrating).isFalse()
    }
}
