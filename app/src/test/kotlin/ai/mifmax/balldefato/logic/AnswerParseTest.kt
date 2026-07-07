package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AnswerParseTest {

    @Test
    fun `skips blank lines and comments, trims, dedupes case-insensitively`() {
        val lines = sequenceOf(
            "  It is certain  ",
            "# a comment",
            "",
            "It is certain",      // duplicate (different spacing)
            "IT IS CERTAIN",      // duplicate (case)
            "Very doubtful",
        )
        val result = AnswerRepository.parse(lines)
        assertThat(result).containsExactly("It is certain", "Very doubtful").inOrder()
    }

    @Test
    fun `empty input yields empty list`() {
        assertThat(AnswerRepository.parse(emptySequence())).isEmpty()
    }
}
