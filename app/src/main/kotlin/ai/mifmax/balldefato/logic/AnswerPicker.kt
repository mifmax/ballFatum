package ai.mifmax.balldefato.logic

import kotlin.random.Random

/** Picks a random answer. RNG is injectable so selection is deterministic in tests. */
class AnswerPicker(
    private val answers: List<String>,
    private val random: Random = Random.Default,
) {
    fun pick(): String = answers[random.nextInt(answers.size)]
}
