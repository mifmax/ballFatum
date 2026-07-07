package ai.mifmax.balldefato.logic

/** Language rows for the picker. Index 0 = follow system; the rest are supported languages. */
object LanguageOptions {

    val TAGS: List<String?> = listOf(null, "en", "ru", "de", "es", "fr")
    val ENDONYMS: List<String> = listOf("English", "Русский", "Deutsch", "Español", "Français")

    fun tagForIndex(index: Int): String? = TAGS[index]

    /** Row for the currently active app-locale tag ("" / null / unknown => system row 0). */
    fun indexForCurrent(currentTag: String?): Int {
        if (currentTag.isNullOrEmpty()) return 0
        val lang = currentTag.substringBefore('-').lowercase()
        val index = TAGS.indexOf(lang)
        return if (index >= 0) index else 0
    }
}
