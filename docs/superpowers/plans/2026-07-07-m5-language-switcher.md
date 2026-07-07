# M5 — In-app Language Switcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Глобус-иконка в углу открывает диалог выбора языка (Системный + 5 языков), меняющий локаль через `AppCompatDelegate.setApplicationLocales` с авто-сохранением.

**Architecture:** Маппинг индекс↔тег вынесен в чистый `LanguageOptions`. `MainActivity` показывает single-choice диалог (тот же тёмный стиль) и вызывает `setApplicationLocales`. Персистентность — авто-хранение AppCompat (манифест-сервис на <13).

**Tech Stack:** AppCompat 1.7 (`setApplicationLocales`), `LocaleListCompat`, Material dialog, JUnit5.

**Команды (JDK 17):** префикс `JAVA_HOME=$(/usr/libexec/java_home -v 17)`. Коммитит пользователь.

---

## Структура файлов

```
app/src/main/kotlin/ai/mifmax/balldefato/logic/LanguageOptions.kt   (Task 1)
app/src/test/kotlin/ai/mifmax/balldefato/logic/LanguageOptionsTest.kt (Task 1)
app/src/main/res/drawable/ic_language.xml                            (Task 2)
app/src/main/res/values/strings.xml + styles.xml                     (Task 2)
app/src/main/AndroidManifest.xml                                     (Task 2, service)
app/src/main/res/layout/activity_main.xml                            (Task 3, globe)
app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt             (Task 3, wiring)
app/src/main/res/values-{ru,de,es,fr}/strings.xml                    (Task 4)
```

---

## Task 1: LanguageOptions (чистая логика, TDD)

**Files:**
- Test: `app/src/test/kotlin/ai/mifmax/balldefato/logic/LanguageOptionsTest.kt`
- Create: `app/src/main/kotlin/ai/mifmax/balldefato/logic/LanguageOptions.kt`

- [ ] **Step 1: Падающий тест**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/test/kotlin/ai/mifmax/balldefato/logic/LanguageOptionsTest.kt`:

```kotlin
package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class LanguageOptionsTest {

    @Test
    fun `tagForIndex maps rows to language tags`() {
        assertThat(LanguageOptions.tagForIndex(0)).isNull()
        assertThat(LanguageOptions.tagForIndex(1)).isEqualTo("en")
        assertThat(LanguageOptions.tagForIndex(5)).isEqualTo("fr")
    }

    @Test
    fun `indexForCurrent handles system, region tags and unknowns`() {
        assertThat(LanguageOptions.indexForCurrent(null)).isEqualTo(0)
        assertThat(LanguageOptions.indexForCurrent("")).isEqualTo(0)
        assertThat(LanguageOptions.indexForCurrent("de")).isEqualTo(3)
        assertThat(LanguageOptions.indexForCurrent("de-DE")).isEqualTo(3)
        assertThat(LanguageOptions.indexForCurrent("xx")).isEqualTo(0)
    }
}
```

- [ ] **Step 2: Запустить — падает**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.logic.LanguageOptionsTest" --no-daemon
```
Expected: FAIL — `unresolved reference: LanguageOptions`.

- [ ] **Step 3: Реализация**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/kotlin/ai/mifmax/balldefato/logic/LanguageOptions.kt`:

```kotlin
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
```

- [ ] **Step 4: Запустить — проходит**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.logic.LanguageOptionsTest" --no-daemon
```
Expected: `BUILD SUCCESSFUL`, 2 теста зелёные.

---

## Task 2: Иконка, строки, тема, манифест-сервис

**Files:**
- Create: `app/src/main/res/drawable/ic_language.xml`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Векторная иконка глобуса**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/drawable/ic_language.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM18.92,8h-2.95c-0.32,-1.25 -0.78,-2.45 -1.38,-3.56 1.84,0.63 3.37,1.9 4.33,3.56zM12,4.04c0.83,1.2 1.48,2.53 1.91,3.96h-3.82c0.43,-1.43 1.08,-2.76 1.91,-3.96zM4.26,14C4.1,13.36 4,12.69 4,12s0.1,-1.36 0.26,-2h3.38c-0.08,0.66 -0.14,1.32 -0.14,2s0.06,1.34 0.14,2L4.26,14zM5.08,16h2.95c0.32,1.25 0.78,2.45 1.38,3.56 -1.84,-0.63 -3.37,-1.9 -4.33,-3.56zM8.03,8L5.08,8c0.96,-1.66 2.49,-2.93 4.33,-3.56C8.81,5.55 8.35,6.75 8.03,8zM12,19.96c-0.83,-1.2 -1.48,-2.53 -1.91,-3.96h3.82c-0.43,1.43 -1.08,2.76 -1.91,3.96zM14.34,14L9.66,14c-0.09,-0.66 -0.16,-1.32 -0.16,-2s0.07,-1.35 0.16,-2h4.68c0.09,0.65 0.16,1.32 0.16,2s-0.07,1.34 -0.16,2zM14.59,19.56c0.6,-1.11 1.06,-2.31 1.38,-3.56h2.95c-0.96,1.65 -2.49,2.93 -4.33,3.56zM16.36,14c0.08,-0.66 0.14,-1.32 0.14,-2s-0.06,-1.34 -0.14,-2h3.38c0.16,0.64 0.26,1.31 0.26,2s-0.1,1.36 -0.26,2h-3.38z" />
</vector>
```

- [ ] **Step 2: Английские строки**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values/strings.xml` перед `</resources>` добавить:

```xml
    <string name="language_title">Language</string>
    <string name="system_default">System default</string>
```

- [ ] **Step 3: Видимый цвет радио-кнопки в тёмном диалоге**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values/styles.xml` в стиль
`ThemeOverlay.BallDeFato.Dialog` добавить (чтобы отметка single-choice была видна на тёмном фоне):

```xml
        <item name="colorPrimary">#B39DFF</item>
```

- [ ] **Step 4: Манифест-сервис авто-хранения локали (для Android <13)**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/AndroidManifest.xml` внутри `<application>`
(после `<activity ... SettingsActivity ... />`) добавить:

```xml
        <service
            android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
            android:enabled="false"
            android:exported="false">
            <meta-data
                android:name="autoStoreLocales"
                android:value="true" />
        </service>
```

---

## Task 3: Глобус в layout + wiring в MainActivity

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt`

- [ ] **Step 1: Кнопка-глобус в activity_main**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/layout/activity_main.xml`
перед закрывающим `</FrameLayout>` добавить:

```xml
    <ImageButton
        android:id="@+id/languageButton"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="top|end"
        android:layout_margin="8dp"
        android:alpha="0.75"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/language_title"
        android:src="@drawable/ic_language"
        app:tint="#FFFFFF" />
```

- [ ] **Step 2: Импорты в MainActivity**

Добавить:

```kotlin
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ai.mifmax.balldefato.logic.LanguageOptions
```

- [ ] **Step 3: Инсет для глобуса + клик**

Заменить существующий блок инсетов (слушатель на `binding.topBar`):

```kotlin
        // Keep the title clear of the status bar (edge-to-edge on Android 15+).
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            (view.layoutParams as FrameLayout.LayoutParams).topMargin =
                top + (16 * resources.displayMetrics.density).toInt()
            view.requestLayout()
            insets
        }
```
на (инсет и для заголовка, и для глобуса):

```kotlin
        // Keep the title and the language button clear of the status bar (edge-to-edge).
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            val density = resources.displayMetrics.density
            (binding.topBar.layoutParams as FrameLayout.LayoutParams).topMargin =
                top + (16 * density).toInt()
            (binding.languageButton.layoutParams as FrameLayout.LayoutParams).topMargin =
                top + (8 * density).toInt()
            binding.topBar.requestLayout()
            binding.languageButton.requestLayout()
            insets
        }

        binding.languageButton.setOnClickListener { showLanguageDialog() }
```

- [ ] **Step 4: Метод showLanguageDialog**

Добавить в класс (например, после `showDonationDialog`):

```kotlin
    private fun showLanguageDialog() {
        val labels = (listOf(getString(R.string.system_default)) + LanguageOptions.ENDONYMS)
            .toTypedArray()
        val currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { null }
        val checked = LanguageOptions.indexForCurrent(currentTag)
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BallDeFato_Dialog)
            .setTitle(R.string.language_title)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                val tag = LanguageOptions.tagForIndex(which)
                val locales = if (tag == null) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }
                AppCompatDelegate.setApplicationLocales(locales)
                dialog.dismiss()
            }
            .show()
    }
```

- [ ] **Step 5: Сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

---

## Task 4: Локализация строк языка (ru/de/es/fr)

**Files:**
- Modify: `app/src/main/res/values-{ru,de,es,fr}/strings.xml`

- [ ] **Step 1: Добавить language_title/system_default в каждую локаль**

Перед `</resources>` в каждом файле добавить:

`values-ru/strings.xml`:
```xml
    <string name="language_title">Язык</string>
    <string name="system_default">Системный</string>
```
`values-de/strings.xml`:
```xml
    <string name="language_title">Sprache</string>
    <string name="system_default">Systemstandard</string>
```
`values-es/strings.xml`:
```xml
    <string name="language_title">Idioma</string>
    <string name="system_default">Predeterminado del sistema</string>
```
`values-fr/strings.xml`:
```xml
    <string name="language_title">Langue</string>
    <string name="system_default">Langue du système</string>
```

- [ ] **Step 2: (опц.) Проверка непустоты в LocalizationTest**

В `app/src/test/kotlin/ai/mifmax/balldefato/LocalizationTest.kt` в цикл
`everyLocaleHasCompleteArrays` добавить:
```kotlin
            assertThat(res.getString(R.string.system_default)).isNotEmpty()
```

---

## Task 5: Полный прогон и проверка на устройстве

**Files:** нет постоянных изменений.

- [ ] **Step 1: Все тесты + сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`; тесты зелёные (20 прежних + LanguageOptionsTest 2).

- [ ] **Step 2: Проверить на устройстве**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; D="192.168.0.166:5555"
"$ADB" connect 192.168.0.166:5555 >/dev/null 2>&1
"$ADB" -s "$D" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$D" shell am force-stop ai.mifmax.balldefato
"$ADB" -s "$D" shell am start -n ai.mifmax.balldefato/.MainActivity
```
Expected: в верхнем углу — глобус; тап открывает диалог с «Системный/English/Русский/
Deutsch/Español/Français»; выбор меняет язык интерфейса, инструкций и пула ответов;
после перезапуска выбор сохраняется.

---

## Self-Review (выполнено при написании плана)

- **Покрытие спеки:** `LanguageOptions`+тест (Task 1), глобус-иконка/строки/тема/сервис
  (Task 2), layout+wiring `setApplicationLocales` (Task 3), локализация language_title/
  system_default (Task 4), прогон+устройство (Task 5). Персистентность — сервис
  `autoStoreLocales` (Task 2). Пробелов нет.
- **Плейсхолдеры:** нет — весь код/иконка/строки приведены.
- **Согласованность:** `LanguageOptions.tagForIndex/indexForCurrent` — как в тесте и
  `showLanguageDialog`. Диалог использует `R.style.ThemeOverlay_BallDeFato_Dialog` (из M4).
  `binding.languageButton` соответствует id в layout. Инсет-слушатель переключён с
  `topBar` на `root` и правит оба вью.
