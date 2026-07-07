package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

class AnswerPickerTest {

    @Test
    fun `pick returns the element at the random index`() {
        val answers = listOf("a", "b", "c", "d")
        // Fake Random that always yields index 2.
        val fixed = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(until: Int): Int = 2
        }
        val picker = AnswerPicker(answers, fixed)
        assertThat(picker.pick()).isEqualTo("c")
    }

    @Test
    fun `pick always stays within bounds for a seeded random`() {
        val answers = listOf("x", "y", "z")
        val picker = AnswerPicker(answers, Random(42))
        repeat(1000) {
            assertThat(answers).contains(picker.pick())
        }
    }

    @Test
    fun `single element list always returns that element`() {
        val picker = AnswerPicker(listOf("only"), Random(1))
        assertThat(picker.pick()).isEqualTo("only")
    }
}
