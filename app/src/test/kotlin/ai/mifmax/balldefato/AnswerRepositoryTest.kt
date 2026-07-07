package ai.mifmax.balldefato

import android.content.Context
import ai.mifmax.balldefato.logic.AnswerRepository
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnswerRepositoryTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun eachLanguagePoolIsLargeUniqueAndNonEmpty() {
        for (lang in listOf("en", "ru", "de", "es", "fr")) {
            val pool = AnswerRepository.load(context, lang)
            assertThat(pool.size).isAtLeast(1900)
            assertThat(pool).containsNoDuplicates()
            assertThat(pool.none { it.isBlank() }).isTrue()
        }
    }

    @Test
    fun unsupportedLanguageFallsBackToEnglish() {
        assertThat(AnswerRepository.load(context, "zz"))
            .isEqualTo(AnswerRepository.load(context, "en"))
    }
}
