package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DonationPromptPolicyTest {

    @Test
    fun `prompts on multiples of 7 or 13 respecting the minimum gap`() {
        val policy = DonationPromptPolicy()
        val shown = (1..30).filter { policy.shouldPrompt(it) }
        // 7 -> ok; 13 (gap 6) -> ok; 14 (gap 1) -> skip; 21 (gap 8) -> ok;
        // 26 (gap 5) -> skip; 28 (gap 7) -> ok.
        assertThat(shown).containsExactly(7, 13, 21, 28).inOrder()
    }

    @Test
    fun `non-multiples never prompt`() {
        val policy = DonationPromptPolicy()
        for (n in listOf(1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 20)) {
            assertThat(policy.shouldPrompt(n)).isFalse()
        }
    }
}
