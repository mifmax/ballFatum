package ai.mifmax.balldefato

import android.os.Looper
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SettingsActivityTest {

    @Test
    fun settingsScreenExposesAllThreePreferences() {
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        shadowOf(Looper.getMainLooper()).idle()

        val fragment = activity.supportFragmentManager
            .findFragmentById(android.R.id.content) as PreferenceFragmentCompat

        assertThat(fragment.findPreference<Preference>("shakeCount")).isNotNull()
        assertThat(fragment.findPreference<Preference>("threshold")).isNotNull()
        assertThat(fragment.findPreference<Preference>("vibrateTime")).isNotNull()
    }
}
