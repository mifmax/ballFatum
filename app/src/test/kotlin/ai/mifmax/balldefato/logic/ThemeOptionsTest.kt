package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ThemeOptionsTest {

    @Test
    fun `modeForIndex maps rows to night-mode constants`() {
        assertThat(ThemeOptions.modeForIndex(0)).isEqualTo(ThemeOptions.SYSTEM)
        assertThat(ThemeOptions.modeForIndex(1)).isEqualTo(ThemeOptions.LIGHT)
        assertThat(ThemeOptions.modeForIndex(2)).isEqualTo(ThemeOptions.DARK)
    }

    @Test
    fun `indexForMode is the inverse and defaults unknown to system`() {
        assertThat(ThemeOptions.indexForMode(ThemeOptions.SYSTEM)).isEqualTo(0)
        assertThat(ThemeOptions.indexForMode(ThemeOptions.LIGHT)).isEqualTo(1)
        assertThat(ThemeOptions.indexForMode(ThemeOptions.DARK)).isEqualTo(2)
        assertThat(ThemeOptions.indexForMode(999)).isEqualTo(0)
    }
}
