package ai.mifmax.balldefato

import android.os.Looper
import android.view.View
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

    @Test
    fun initialMessageIsShown() {
        val activity = launch()
        val text = activity.findViewById<TextView>(R.id.MessageTextView).text.toString()
        assertThat(text).isNotEmpty()
    }

    @Test
    fun longPressOnBallOpensSettings() {
        val activity = launch()
        activity.findViewById<View>(R.id.ball).performLongClick()
        val started = shadowOf(activity).nextStartedActivity
        assertThat(started.component?.className).isEqualTo(SettingsActivity::class.java.name)
    }
}
