# M3 — Answer Bank ×100 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 2000 нативных ответов на каждый из 5 языков (en/ru/de/es/fr, тон 2:1:1), в `assets/answers/<lang>.txt`, читаемых `AnswerRepository` по локали.

**Architecture:** Ответы переезжают из `strings.xml` в assets. `AnswerRepository.parse()` — чистая логика (дедуп/скип пустых/`#`), `load()` — asset IO + фолбэк `<lang>`→`en`→встроенные 20. Генерация — многоагентным Workflow с дедупом и loop-until-target.

**Tech Stack:** Kotlin, Android assets, Robolectric, JUnit5, Workflow tool (fan-out генерация).

**Команды (JDK 17):** префикс `JAVA_HOME=$(/usr/libexec/java_home -v 17)`. Коммитит пользователь (агент не коммитит).

---

## Структура файлов

```
app/src/main/kotlin/ai/mifmax/balldefato/logic/AnswerRepository.kt   (Task 1)
app/src/test/kotlin/ai/mifmax/balldefato/logic/AnswerParseTest.kt    (Task 1)
app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt             (Task 2, wiring)
docs/superpowers/workflows/m3-answers.workflow.js                    (Task 3, скрипт)
app/src/main/assets/answers/{en,ru,de,es,fr}.txt                     (Task 4, ×2000)
app/src/test/kotlin/ai/mifmax/balldefato/AnswerRepositoryTest.kt     (Task 5)
```

---

## Task 1: AnswerRepository (parse чистая + load) — TDD

**Files:**
- Create: `app/src/main/kotlin/ai/mifmax/balldefato/logic/AnswerRepository.kt`
- Test: `app/src/test/kotlin/ai/mifmax/balldefato/logic/AnswerParseTest.kt`

- [ ] **Step 1: Написать падающий тест на `parse`**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/test/kotlin/ai/mifmax/balldefato/logic/AnswerParseTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Запустить — падает**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.logic.AnswerParseTest" --no-daemon
```
Expected: FAIL — `unresolved reference: AnswerRepository`.

- [ ] **Step 3: Реализовать AnswerRepository**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/kotlin/ai/mifmax/balldefato/logic/AnswerRepository.kt`:

```kotlin
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
```

- [ ] **Step 4: Запустить — проходит**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.logic.AnswerParseTest" --no-daemon
```
Expected: `BUILD SUCCESSFUL`, 2 теста зелёные.

---

## Task 2: Подключить AnswerRepository в MainActivity

**Files:**
- Modify: `app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt`

- [ ] **Step 1: Импорт**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt` добавить импорт рядом с другими `ai.mifmax.balldefato.logic.*`:

```kotlin
import ai.mifmax.balldefato.logic.AnswerRepository
```

- [ ] **Step 2: Строить пул по текущей локали**

Заменить строку инициализации `answerPicker` в `onCreate`:

```kotlin
        answerPicker = AnswerPicker(resources.getStringArray(R.array.responses).toList())
```
на:
```kotlin
        val language = resources.configuration.locales[0].language
        answerPicker = AnswerPicker(AnswerRepository.load(this, language))
```

- [ ] **Step 3: Сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`. (Ассетов ещё нет → на рантайме сработает фолбэк на 20 — это ок до Task 4.)

---

## Task 3: Скрипт генерации (Workflow)

**Files:**
- Create: `docs/superpowers/workflows/m3-answers.workflow.js`

- [ ] **Step 1: Сохранить скрипт workflow**

Create `/Users/mverakhovskiy/work/ballFatum/docs/superpowers/workflows/m3-answers.workflow.js`:

```javascript
export const meta = {
  name: 'ballfatum-answers',
  description: 'Generate 2000 native Magic 8-Ball answers per language (en/ru/de/es/fr), tone 2:1:1, deduped',
  phases: [{ title: 'Generate', detail: '5 languages x 3 tones, batched with dedup' }],
}

const LANGS = [
  { code: 'en', name: 'English' },
  { code: 'ru', name: 'Russian' },
  { code: 'de', name: 'German' },
  { code: 'es', name: 'Spanish' },
  { code: 'fr', name: 'French' },
]
const TONES = [
  { key: 'affirmative', target: 1000, desc: 'clearly positive, yes-leaning, encouraging' },
  { key: 'neutral', target: 500, desc: 'non-committal, uncertain, "ask again" style' },
  { key: 'negative', target: 500, desc: 'clearly negative, no-leaning, discouraging' },
]
const MAX_ROUNDS = 40
const BATCH = 120
const MAX_LEN = 50
const SCHEMA = {
  type: 'object',
  properties: { answers: { type: 'array', items: { type: 'string' } } },
  required: ['answers'],
  additionalProperties: false,
}

function norm(s) {
  return s.toLowerCase().replace(/[\s.,!?…"'«»„“”()\-–—]+/g, ' ').trim()
}

async function genTone(lang, tone) {
  const seen = new Set()
  const kept = []
  let dry = 0
  for (let round = 0; round < MAX_ROUNDS && kept.length < tone.target; round++) {
    const sample = []
    const step = Math.max(1, Math.floor(kept.length / 25))
    for (let i = 0; i < kept.length && sample.length < 25; i += step) sample.push(kept[i])
    const prompt =
      `You write answers for a Magic 8-Ball fortune app, in ${lang.name}.\n` +
      `Produce ${BATCH} DISTINCT short mystical predictions that are ${tone.desc}.\n` +
      `Rules:\n` +
      `- ${lang.name} only, natural and idiomatic.\n` +
      `- Each answer <= ${MAX_LEN} characters, single line, no numbering, no surrounding quotes.\n` +
      `- Magic 8-Ball style, but VARIED wording; all ${BATCH} different from each other.\n` +
      (sample.length ? `- Do NOT repeat these already-used ones: ${sample.join(' | ')}\n` : '') +
      `Return JSON {"answers":[...]}. Need ${tone.target - kept.length} more (round ${round + 1}).`
    const res = await agent(prompt, { label: `${lang.code}:${tone.key}:r${round + 1}`, phase: 'Generate', schema: SCHEMA })
    const before = kept.length
    for (const raw of (res && res.answers) || []) {
      const a = (raw || '').trim()
      if (!a || a.length > MAX_LEN) continue
      const k = norm(a)
      if (!k || seen.has(k)) continue
      seen.add(k)
      kept.push(a)
      if (kept.length >= tone.target) break
    }
    log(`${lang.code}/${tone.key}: ${kept.length}/${tone.target}`)
    if (kept.length === before) { dry++; if (dry >= 3) break } else dry = 0
  }
  return { lang: lang.code, tone: tone.key, list: kept }
}

const results = await parallel(
  LANGS.flatMap(lang => TONES.map(tone => () => genTone(lang, tone)))
)

const byLang = {}
for (const L of LANGS) byLang[L.code] = []
for (const r of results.filter(Boolean)) byLang[r.lang].push(...r.list)

const counts = {}
for (const L of LANGS) counts[L.code] = byLang[L.code].length
log('final counts: ' + JSON.stringify(counts))
return byLang
```

- [ ] **Step 2: Запустить workflow**

Вызвать инструмент `Workflow` с `scriptPath:
"/Users/mverakhovskiy/work/ballFatum/docs/superpowers/workflows/m3-answers.workflow.js"`.
Дождаться завершения (task-notification). Результат — объект `{en:[...], ru:[...], de:[...], es:[...], fr:[...]}`.
Ожидание: у каждого языка ≤ 2000 (в идеале 2000). Если по какому-то тону недобор —
зафиксировать в отчёте (не замалчивать).

---

## Task 4: Записать assets

**Files:**
- Create: `app/src/main/assets/answers/en.txt`, `ru.txt`, `de.txt`, `es.txt`, `fr.txt`

- [ ] **Step 1: Записать по файлу на язык**

Для каждого языка записать `app/src/main/assets/answers/<lang>.txt`: по одному ответу
на строку (значения из `byLang[<lang>]`), UTF-8, без пустых строк. Заголовок-комментарий
первой строкой допустим: `# <lang> answers, generated M3`.

- [ ] **Step 2: Проверить объём и уникальность**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum/app/src/main/assets/answers
for f in en ru de es fr; do
  total=$(grep -vcE '^\s*(#|$)' "$f.txt")
  uniq=$(grep -vE '^\s*(#|$)' "$f.txt" | tr 'A-Z' 'a-z' | sort -u | wc -l | tr -d ' ')
  echo "$f: lines=$total unique=$uniq"
done
```
Expected: каждый `lines`≈2000 и `unique`==`lines` (полностью уникальны). Если где-то
меньше 2000 — это зафиксированный недобор тона из Task 3.

---

## Task 5: AnswerRepositoryTest (Robolectric, реальные assets)

**Files:**
- Test: `app/src/test/kotlin/ai/mifmax/balldefato/AnswerRepositoryTest.kt`

- [ ] **Step 1: Написать тест**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/test/kotlin/ai/mifmax/balldefato/AnswerRepositoryTest.kt`:

```kotlin
package ai.mifmax.balldefato

import android.content.Context
import ai.mifmax.balldefato.logic.AnswerRepository
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnswerRepositoryTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun eachLanguagePoolIsLargeUniqueAndNonEmpty() {
        for (lang in listOf("en", "ru", "de", "es", "fr")) {
            val pool = AnswerRepository.load(context, lang)
            assertThat(pool.size).isAtLeast(1900) // near target 2000, allows minor tone shortfall
            assertThat(pool).containsNoDuplicates()
            assertThat(pool.none { it.isBlank() }).isTrue()
        }
    }

    @Test
    fun unsupportedLanguageFallsBackToEnglish() {
        assertThat(AnswerRepository.load(context, "zz")).isEqualTo(AnswerRepository.load(context, "en"))
    }
}
```

- [ ] **Step 2: Запустить**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.AnswerRepositoryTest" --no-daemon
```
Expected: `BUILD SUCCESSFUL`, 2 теста зелёные. (Если пул < 1900 по языку — вернуться
к Task 3 и догенерировать этот тон.)

---

## Task 6: Полный прогон и проверка на устройстве

**Files:** нет изменений.

- [ ] **Step 1: Все тесты + сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`; все тесты зелёные (14 из M1/M2 + AnswerParseTest 2 + AnswerRepositoryTest 2).

- [ ] **Step 2: Проверить на устройстве (APK-размер + запуск)**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
ls -la app/build/outputs/apk/debug/app-debug.apk
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; D="192.168.0.166:5555"
"$ADB" connect 192.168.0.166:5555 >/dev/null 2>&1
"$ADB" -s "$D" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$D" shell am force-stop ai.mifmax.balldefato
"$ADB" -s "$D" shell am start -n ai.mifmax.balldefato/.MainActivity
```
Expected: приложение запускается; при тряске выдаёт ответы из большого пула (визуально
разные каждый раз). Ассеты добавят ~0.5–1 МБ к APK — норм.

---

## Self-Review (выполнено при написании плана)

- **Покрытие спеки:** assets-хранение (Task 4), `AnswerRepository.parse`/`load` +
  фолбэк (Task 1), wiring по локали (Task 2), генерация workflow нативно×5 с дедупом и
  loop-until-target (Task 3), тесты parse + репозиторий (Task 1/5), полный прогон +
  устройство (Task 6). Тон 2:1:1 и лимит длины заданы в скрипте (Task 3). Пробелов нет.
- **Плейсхолдеры:** нет — весь код и скрипт приведены целиком.
- **Согласованность:** `AnswerRepository.parse(Sequence<String>)`/`load(Context, String)`
  одинаковы в Task 1/2/5. `AnswerPicker(list)` совпадает с M1. Пути ассетов
  `answers/<lang>.txt` совпадают в скрипте/репозитории/тесте. Порог теста ≥1900
  согласован с «недобор фиксируем» из спеки.
