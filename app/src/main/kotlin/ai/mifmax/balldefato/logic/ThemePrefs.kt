package ai.mifmax.balldefato.logic

import android.content.Context

/** Persists the chosen night mode across app restarts. */
object ThemePrefs {
    private const val PREFS = "theme"
    private const val KEY = "night_mode"

    fun load(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, ThemeOptions.SYSTEM)

    fun save(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, mode).apply()
    }
}
