package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class LanguageOptionsTest {

    @Test
    fun `tagForIndex maps rows to language tags`() {
        assertThat(LanguageOptions.tagForIndex(0)).isNull()
        assertThat(LanguageOptions.tagForIndex(1)).isEqualTo("en")
        assertThat(LanguageOptions.tagForIndex(5)).isEqualTo("fr")
    }

    @Test
    fun `indexForCurrent handles system, region tags and unknowns`() {
        assertThat(LanguageOptions.indexForCurrent(null)).isEqualTo(0)
        assertThat(LanguageOptions.indexForCurrent("")).isEqualTo(0)
        assertThat(LanguageOptions.indexForCurrent("de")).isEqualTo(3)
        assertThat(LanguageOptions.indexForCurrent("de-DE")).isEqualTo(3)
        assertThat(LanguageOptions.indexForCurrent("xx")).isEqualTo(0)
    }
}
