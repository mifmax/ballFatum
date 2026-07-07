# M4 — Donation Popup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** На «магических» числах трясок за сессию (кратно 7 или 13, с зазором ≥6) показывать Material-диалог с случайным локализованным мистическим текстом и кнопкой доната.

**Architecture:** Триггер вынесен в чистый `DonationPromptPolicy` (тестируемо). `MainActivity` ведёт сессионный счётчик трясок и показывает `MaterialAlertDialog`. Ссылка — константа `DonationConfig.URL`. Тексты локализованы (как в M2).

**Tech Stack:** Kotlin, Material `MaterialAlertDialogBuilder`, Android string-arrays, JUnit5/Robolectric.

**Команды (JDK 17):** префикс `JAVA_HOME=$(/usr/libexec/java_home -v 17)`. Коммитит пользователь.

---

## Структура файлов

```
app/src/main/kotlin/ai/mifmax/balldefato/logic/DonationPromptPolicy.kt   (Task 1)
app/src/test/kotlin/ai/mifmax/balldefato/logic/DonationPromptPolicyTest.kt (Task 1)
app/src/main/kotlin/ai/mifmax/balldefato/DonationConfig.kt               (Task 2)
app/src/main/res/values/strings.xml                                      (Task 2, +donate strings)
app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt                 (Task 3, counter+dialog)
app/src/main/AndroidManifest.xml                                         (Task 3, configChanges)
app/src/main/res/values-{ru,de,es,fr}/strings.xml                        (Task 4)
app/src/test/kotlin/ai/mifmax/balldefato/LocalizationTest.kt             (Task 5)
```

---

## Task 1: DonationPromptPolicy (чистая логика, TDD)

**Files:**
- Test: `app/src/test/kotlin/ai/mifmax/balldefato/logic/DonationPromptPolicyTest.kt`
- Create: `app/src/main/kotlin/ai/mifmax/balldefato/logic/DonationPromptPolicy.kt`

- [ ] **Step 1: Падающий тест**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/test/kotlin/ai/mifmax/balldefato/logic/DonationPromptPolicyTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Запустить — падает**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.logic.DonationPromptPolicyTest" --no-daemon
```
Expected: FAIL — `unresolved reference: DonationPromptPolicy`.

- [ ] **Step 3: Реализация**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/kotlin/ai/mifmax/balldefato/logic/DonationPromptPolicy.kt`:

```kotlin
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
```

- [ ] **Step 4: Запустить — проходит**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.logic.DonationPromptPolicyTest" --no-daemon
```
Expected: `BUILD SUCCESSFUL`, 2 теста зелёные.

---

## Task 2: DonationConfig + английские строки

**Files:**
- Create: `app/src/main/kotlin/ai/mifmax/balldefato/DonationConfig.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: DonationConfig**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/kotlin/ai/mifmax/balldefato/DonationConfig.kt`:

```kotlin
package ai.mifmax.balldefato

/** Where the donation button leads. Swap the URL when the platform/handle is chosen. */
object DonationConfig {
    const val URL = "https://ko-fi.com/mifmax"
}
```

- [ ] **Step 2: Добавить английские строки доната**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values/strings.xml` перед закрывающим `</resources>` добавить:

```xml
    <string name="donate_title">The stars whisper…</string>
    <string name="donate_action">Support</string>
    <string name="donate_later">Later</string>

    <string-array name="donation_prompts">
        <item>The stars whisper: support the one who conjured me.</item>
        <item>The oracle thrives on kindness — support its maker?</item>
        <item>A cosmic favor: help the creator keep the magic alive.</item>
        <item>The spirits nudge you toward the one who made me.</item>
        <item>Fate smiles wider on those who support my creator.</item>
        <item>The universe hints: a small gift for my maker?</item>
        <item>My visions flow freely — perhaps a coin for their keeper?</item>
        <item>The constellations ask a kindness for my creator.</item>
        <item>Magic has a price — support the hand that shaped me.</item>
        <item>The heavens favor generous souls — aid my maker?</item>
        <item>Destiny remembers those who support the dreamer behind me.</item>
        <item>The moon requests a token for the one who built me.</item>
    </string-array>
```

---

## Task 3: MainActivity — счётчик и диалог + манифест

**Files:**
- Modify: `app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: configChanges в манифесте**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/AndroidManifest.xml` в теге `<activity android:name=".MainActivity" ...>` добавить атрибут (чтобы поворот не сбрасывал сессионный счётчик):

```xml
            android:configChanges="orientation|screenSize|keyboardHidden"
```

- [ ] **Step 2: Импорты в MainActivity**

Добавить импорты:

```kotlin
import android.net.Uri
import ai.mifmax.balldefato.logic.DonationPromptPolicy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
```

- [ ] **Step 3: Поля счётчика/политики**

Рядом с `private val shakeDetector = ShakeDetector()` добавить:

```kotlin
    private val donationPolicy = DonationPromptPolicy()
    private var shakeCount = 0
```

- [ ] **Step 4: Инкремент и показ в onSensorChanged**

Заменить блок:

```kotlin
        if (triggered) {
            showMessage(answerPicker.pick(), vibrate = true)
        }
```
на:
```kotlin
        if (triggered) {
            showMessage(answerPicker.pick(), vibrate = true)
            shakeCount++
            if (donationPolicy.shouldPrompt(shakeCount)) showDonationDialog()
        }
```

- [ ] **Step 5: Методы диалога**

Добавить в класс (например, после `showMessage`):

```kotlin
    private fun showDonationDialog() {
        val prompts = resources.getStringArray(R.array.donation_prompts).toList()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.donate_title)
            .setMessage(AnswerPicker(prompts).pick())
            .setPositiveButton(R.string.donate_action) { _, _ -> openDonation() }
            .setNegativeButton(R.string.donate_later, null)
            .show()
    }

    private fun openDonation() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DonationConfig.URL)))
        } catch (e: android.content.ActivityNotFoundException) {
            // No browser available — silently ignore.
        }
    }
```

- [ ] **Step 6: Сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

---

## Task 4: Локализация строк доната (ru/de/es/fr)

**Files:**
- Modify: `app/src/main/res/values-ru/strings.xml`, `values-de/…`, `values-es/…`, `values-fr/…`

- [ ] **Step 1: Русский** — перед `</resources>` в `values-ru/strings.xml`:

```xml
    <string name="donate_title">Звёзды шепчут…</string>
    <string name="donate_action">Поддержать</string>
    <string name="donate_later">Позже</string>

    <string-array name="donation_prompts">
        <item>Звёзды шепчут: поддержи того, кто меня призвал.</item>
        <item>Оракул живёт добротой — поддержишь создателя?</item>
        <item>Космическая услуга: помоги творцу хранить магию.</item>
        <item>Духи направляют тебя к тому, кто меня создал.</item>
        <item>Судьба щедрее к тем, кто поддержал моего творца.</item>
        <item>Вселенная намекает: маленький дар создателю?</item>
        <item>Мои видения бесплатны — может, монету их хранителю?</item>
        <item>Созвездия просят доброты для моего творца.</item>
        <item>У магии есть цена — поддержи руку, что меня создала.</item>
        <item>Небеса благосклонны к щедрым — поможешь мастеру?</item>
        <item>Судьба помнит тех, кто поддержал мечтателя за мной.</item>
        <item>Луна просит дар для того, кто меня построил.</item>
    </string-array>
```

- [ ] **Step 2: Немецкий** — в `values-de/strings.xml`:

```xml
    <string name="donate_title">Die Sterne flüstern…</string>
    <string name="donate_action">Unterstützen</string>
    <string name="donate_later">Später</string>

    <string-array name="donation_prompts">
        <item>Die Sterne flüstern: unterstütze den, der mich rief.</item>
        <item>Das Orakel lebt von Güte — den Schöpfer unterstützen?</item>
        <item>Ein kosmischer Gefallen: hilf, die Magie zu bewahren.</item>
        <item>Die Geister weisen dich zu dem, der mich erschuf.</item>
        <item>Das Schicksal lächelt milder, wer meinen Schöpfer stützt.</item>
        <item>Das Universum deutet an: ein kleines Geschenk für ihn?</item>
        <item>Meine Visionen sind frei — eine Münze für den Hüter?</item>
        <item>Die Sternbilder erbitten Güte für meinen Schöpfer.</item>
        <item>Magie hat ihren Preis — stütze die Hand, die mich formte.</item>
        <item>Der Himmel begünstigt Großzügige — hilfst du dem Meister?</item>
        <item>Das Schicksal denkt an die, die den Träumer stützen.</item>
        <item>Der Mond erbittet eine Gabe für den, der mich baute.</item>
    </string-array>
```

- [ ] **Step 3: Испанский** — в `values-es/strings.xml`:

```xml
    <string name="donate_title">Las estrellas susurran…</string>
    <string name="donate_action">Apoyar</string>
    <string name="donate_later">Más tarde</string>

    <string-array name="donation_prompts">
        <item>Las estrellas susurran: apoya a quien me invocó.</item>
        <item>El oráculo vive de la bondad — ¿apoyas a su creador?</item>
        <item>Un favor cósmico: ayuda a conservar la magia.</item>
        <item>Los espíritus te guían hacia quien me creó.</item>
        <item>El destino sonríe más a quien apoya a mi creador.</item>
        <item>El universo insinúa: ¿un pequeño regalo para él?</item>
        <item>Mis visiones son gratis — ¿una moneda para su guardián?</item>
        <item>Las constelaciones piden bondad para mi creador.</item>
        <item>La magia tiene un precio — apoya la mano que me formó.</item>
        <item>El cielo favorece a los generosos — ¿ayudas al maestro?</item>
        <item>El destino recuerda a quien apoya al soñador tras de mí.</item>
        <item>La luna pide un obsequio para quien me construyó.</item>
    </string-array>
```

- [ ] **Step 4: Французский** (апострофы экранированы `\'`) — в `values-fr/strings.xml`:

```xml
    <string name="donate_title">Les étoiles murmurent…</string>
    <string name="donate_action">Soutenir</string>
    <string name="donate_later">Plus tard</string>

    <string-array name="donation_prompts">
        <item>Les étoiles murmurent : soutiens celui qui m\'a invoqué.</item>
        <item>L\'oracle vit de bonté — soutiens-tu son créateur ?</item>
        <item>Une faveur cosmique : aide à préserver la magie.</item>
        <item>Les esprits te guident vers celui qui m\'a créé.</item>
        <item>Le destin sourit plus à qui soutient mon créateur.</item>
        <item>L\'univers suggère : un petit cadeau pour lui ?</item>
        <item>Mes visions sont gratuites — une pièce pour son gardien ?</item>
        <item>Les constellations demandent de la bonté pour mon créateur.</item>
        <item>La magie a un prix — soutiens la main qui m\'a façonné.</item>
        <item>Le ciel favorise les généreux — aides-tu le maître ?</item>
        <item>Le destin se souvient de qui soutient le rêveur derrière moi.</item>
        <item>La lune demande un présent pour celui qui m\'a bâti.</item>
    </string-array>
```

---

## Task 5: Расширить LocalizationTest

**Files:**
- Modify: `app/src/test/kotlin/ai/mifmax/balldefato/LocalizationTest.kt`

- [ ] **Step 1: Добавить проверку donation_prompts**

В `/Users/mverakhovskiy/work/ballFatum/app/src/test/kotlin/ai/mifmax/balldefato/LocalizationTest.kt` в метод `everyLocaleHasCompleteArrays`, внутри цикла `for (language in ...)`, после проверки `instructions`, добавить строку:

```kotlin
            assertThat(res.getStringArray(R.array.donation_prompts)).hasLength(12)
```

- [ ] **Step 2: Запустить локализационный тест**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.LocalizationTest" --no-daemon
```
Expected: `BUILD SUCCESSFUL`, тесты зелёные (donation_prompts = 12 во всех локалях).

---

## Task 6: Полный прогон и проверка на устройстве

**Files:** нет постоянных изменений (шаг 2 — временная заплатка).

- [ ] **Step 1: Все тесты + сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`; тесты зелёные (18 прежних + DonationPromptPolicyTest 2).

- [ ] **Step 2: Проверить диалог на устройстве (временная заплатка)**

Временно вызвать диалог при запуске: в `MainActivity.onCreate` в самом конце добавить
`showDonationDialog()`. Собрать, установить, разбудить экран и снять скриншот:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleDebug --no-daemon
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; D="192.168.0.166:5555"
"$ADB" connect 192.168.0.166:5555 >/dev/null 2>&1
"$ADB" -s "$D" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$D" shell input keyevent KEYCODE_WAKEUP; "$ADB" -s "$D" shell svc power stayon true
"$ADB" -s "$D" shell am force-stop ai.mifmax.balldefato
"$ADB" -s "$D" shell am start -n ai.mifmax.balldefato/.MainActivity
sleep 3
"$ADB" -s "$D" exec-out screencap -p > /tmp/bf_donate.png
```
Expected: на скриншоте Material-диалог с заголовком «The stars whisper…»/локализованным,
мистическим текстом и кнопками «Support»/«Later».

- [ ] **Step 3: Убрать заплатку**

Удалить временный `showDonationDialog()` из `onCreate`; пересобрать и переустановить:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleDebug --no-daemon
"$HOME/Library/Android/sdk/platform-tools/adb" -s 192.168.0.166:5555 install -r app/build/outputs/apk/debug/app-debug.apk
"$HOME/Library/Android/sdk/platform-tools/adb" -s 192.168.0.166:5555 shell svc power stayon false
```
Expected: `git diff` по `MainActivity.kt` не содержит временного вызова.

---

## Self-Review (выполнено при написании плана)

- **Покрытие спеки:** `DonationPromptPolicy`+тест (Task 1), `DonationConfig.URL` (Task 2),
  диалог/счётчик/`configChanges` (Task 3), 12 локализованных текстов + кнопки ×5 (Task 2/4),
  тест размера массива (Task 5), полный прогон + устройство (Task 6). Пробелов нет.
- **Плейсхолдеры:** нет — весь код и все тексты приведены.
- **Согласованность:** `DonationPromptPolicy.shouldPrompt(Int)` совпадает в Task 1/3.
  `AnswerPicker(list).pick()` — как в M1/M3. `donation_prompts` (12) одинаково в базе и
  локалях; тест проверяет 12. `DonationConfig.URL` используется в `openDonation` (Task 3).
