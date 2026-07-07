package ai.mifmax.balldefato

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LocalizationTest {

    private fun resFor(language: String): Resources {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return base.createConfigurationContext(config).resources
    }

    @Test
    fun everyLocaleHasCompleteArrays() {
        for (language in listOf("en", "ru", "de", "es", "fr")) {
            val res = resFor(language)
            assertThat(res.getStringArray(R.array.responses)).hasLength(20)
            assertThat(res.getStringArray(R.array.instructions)).hasLength(10)
            assertThat(res.getStringArray(R.array.donation_prompts)).hasLength(12)
            assertThat(res.getString(R.string.system_default)).isNotEmpty()
            assertThat(res.getString(R.string.shake_me_caption)).isNotEmpty()
        }
    }

    @Test
    fun translatedLocalesActuallyDifferFromEnglishBase() {
        val en = resFor("en").getStringArray(R.array.responses)
        for (language in listOf("ru", "de", "es", "fr")) {
            val localized = resFor(language).getStringArray(R.array.responses)
            // If a locale file were missing, this would fall back to English and fail.
            assertThat(localized[0]).isNotEqualTo(en[0])
        }
    }
}
