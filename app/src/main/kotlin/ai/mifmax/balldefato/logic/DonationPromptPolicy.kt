package ai.mifmax.balldefato.logic

/**
 * Decides when to show the donation prompt based on the in-session shake count:
 * on multiples of 7 or 13, but never closer than [minGap] shakes apart.
 */
class DonationPromptPolicy(private val minGap: Int = 6) {

    private var lastPromptCount = 0

    fun shouldPrompt(shakeCount: Int): Boolean {
        val isMagic = shakeCount % 7 == 0 || shakeCount % 13 == 0
        if (isMagic && shakeCount - lastPromptCount >= minGap) {
            lastPromptCount = shakeCount
            return true
        }
        return false
    }
}
