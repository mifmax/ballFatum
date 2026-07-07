package ai.mifmax.balldefato.logic

import android.content.Context
import ai.mifmax.balldefato.R
import java.io.IOException

/**
 * Loads the answer pool from assets/answers/<lang>.txt (one answer per line).
 * Fallback chain: requested language → English → the built-in 20-item array.
 */
object AnswerRepository {

    /** Pure parsing: trim, drop blank/`#` lines, dedupe case-insensitively, keep order. */
    fun parse(lines: Sequence<String>): List<String> {
        val seen = HashSet<String>()
        val out = ArrayList<String>()
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (seen.add(line.lowercase())) out.add(line)
        }
        return out
    }

    fun load(context: Context, language: String): List<String> {
        for (lang in listOf(language, "en")) {
            val list = readAsset(context, "answers/$lang.txt")
            if (list.isNotEmpty()) return list
        }
        return context.resources.getStringArray(R.array.responses).toList()
    }

    private fun readAsset(context: Context, path: String): List<String> =
        try {
            context.assets.open(path).bufferedReader().useLines { parse(it) }
        } catch (e: IOException) {
            emptyList()
        }
}
