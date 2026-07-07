package ai.mifmax.balldefato

import android.os.Looper
import android.widget.TextView
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private fun launch(): MainActivity {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        shadowOf(Looper.getMainLooper()).idle()
        return activity
    }

    private fun messageText(activity: MainActivity): String =
        activity.findViewById<TextView>(R.id.MessageTextView).text.toString()

    @Test
    fun initialMessageIsShown() {
        val activity = launch()
        assertThat(messageText(activity)).isNotEmpty()
    }

    @Test
    fun shakeMenuItemShowsOneOfTheResponses() {
        val activity = launch()
        shadowOf(activity).clickMenuItem(R.id.shake)
        val responses = activity.resources.getStringArray(R.array.responses).toList()
        assertThat(responses).contains(messageText(activity))
    }

    @Test
    fun preferencesMenuItemLaunchesSettings() {
        val activity = launch()
        shadowOf(activity).clickMenuItem(R.id.preferences)
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.component?.className).isEqualTo(SettingsActivity::class.java.name)
    }
}
