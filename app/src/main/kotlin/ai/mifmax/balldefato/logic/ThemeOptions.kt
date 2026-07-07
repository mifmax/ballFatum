package ai.mifmax.balldefato.logic

/**
 * Theme rows for the picker. Values equal AppCompatDelegate.MODE_NIGHT_* so they can be
 * passed straight to setDefaultNightMode. Index 0 = follow system.
 */
object ThemeOptions {

    const val SYSTEM = -1 // AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    const val LIGHT = 1 // AppCompatDelegate.MODE_NIGHT_NO
    const val DARK = 2 // AppCompatDelegate.MODE_NIGHT_YES

    val MODES: List<Int> = listOf(SYSTEM, LIGHT, DARK)

    fun modeForIndex(index: Int): Int = MODES[index]

    fun indexForMode(mode: Int): Int = MODES.indexOf(mode).let { if (it >= 0) it else 0 }
}
