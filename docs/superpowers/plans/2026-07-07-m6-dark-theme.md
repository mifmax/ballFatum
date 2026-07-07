# M6 — Dark Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Переключатель темы (Системная/Светлая/Тёмная) в приложении + тёмный фон главного экрана в ночном режиме, с сохранением выбора.

**Architecture:** Чистые `ThemeOptions` (index↔mode) и `ThemePrefs` (SharedPreferences). `BallDeFatoApp` применяет сохранённый режим на старте. `MainActivity` показывает диалог темы. Тёмный фон — `drawable-night/bg.xml`.

**Tech Stack:** AppCompat `setDefaultNightMode`, Application, Material dialog, JUnit5.

**Команды (JDK 17):** префикс `JAVA_HOME=$(/usr/libexec/java_home -v 17)`. Коммитит пользователь.

---

## Структура файлов

```
app/src/main/kotlin/ai/mifmax/balldefato/logic/ThemeOptions.kt        (Task 1)
app/src/test/kotlin/ai/mifmax/balldefato/logic/ThemeOptionsTest.kt    (Task 1)
app/src/main/kotlin/ai/mifmax/balldefato/logic/ThemePrefs.kt          (Task 2)
app/src/main/kotlin/ai/mifmax/balldefato/BallDeFatoApp.kt             (Task 2)
app/src/main/AndroidManifest.xml                                      (Task 2, android:name)
app/src/main/res/drawable/ic_theme.xml                                (Task 3)
app/src/main/res/drawable-night/bg.xml                                (Task 3)
app/src/main/res/values/strings.xml                                   (Task 3)
app/src/main/res/layout/activity_main.xml                             (Task 3, кнопка темы)
app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt              (Task 4, wiring)
app/src/main/res/values-{ru,de,es,fr}/strings.xml                     (Task 5)
```

---

## Task 1: ThemeOptions (чистая логика, TDD)

**Files:**
- Test: `app/src/test/kotlin/ai/mifmax/balldefato/logic/ThemeOptionsTest.kt`
- Create: `app/src/main/kotlin/ai/mifmax/balldefato/logic/ThemeOptions.kt`

- [ ] **Step 1: Падающий тест**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/test/kotlin/ai/mifmax/balldefato/logic/ThemeOptionsTest.kt`:

```kotlin
package ai.mifmax.balldefato.logic

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ThemeOptionsTest {

    @Test
    fun `modeForIndex maps rows to night-mode constants`() {
        assertThat(ThemeOptions.modeForIndex(0)).isEqualTo(ThemeOptions.SYSTEM)
        assertThat(ThemeOptions.modeForIndex(1)).isEqualTo(ThemeOptions.LIGHT)
        assertThat(ThemeOptions.modeForIndex(2)).isEqualTo(ThemeOptions.DARK)
    }

    @Test
    fun `indexForMode is the inverse and defaults unknown to system`() {
        assertThat(ThemeOptions.indexForMode(ThemeOptions.SYSTEM)).isEqualTo(0)
        assertThat(ThemeOptions.indexForMode(ThemeOptions.LIGHT)).isEqualTo(1)
        assertThat(ThemeOptions.indexForMode(ThemeOptions.DARK)).isEqualTo(2)
        assertThat(ThemeOptions.indexForMode(999)).isEqualTo(0)
    }
}
```

- [ ] **Step 2: Запустить — падает**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.logic.ThemeOptionsTest" --no-daemon
```
Expected: FAIL — `unresolved reference: ThemeOptions`.

- [ ] **Step 3: Реализация**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/kotlin/ai/mifmax/balldefato/logic/ThemeOptions.kt`:

```kotlin
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
```

- [ ] **Step 4: Запустить — проходит**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.logic.ThemeOptionsTest" --no-daemon
```
Expected: `BUILD SUCCESSFUL`, 2 теста зелёные.

---

## Task 2: ThemePrefs + Application + манифест

**Files:**
- Create: `app/src/main/kotlin/ai/mifmax/balldefato/logic/ThemePrefs.kt`
- Create: `app/src/main/kotlin/ai/mifmax/balldefato/BallDeFatoApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: ThemePrefs**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/kotlin/ai/mifmax/balldefato/logic/ThemePrefs.kt`:

```kotlin
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
```

- [ ] **Step 2: Application, применяющий сохранённую тему на старте**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/kotlin/ai/mifmax/balldefato/BallDeFatoApp.kt`:

```kotlin
package ai.mifmax.balldefato

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import ai.mifmax.balldefato.logic.ThemePrefs

class BallDeFatoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.load(this))
    }
}
```

- [ ] **Step 3: Зарегистрировать Application**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/AndroidManifest.xml` в теге `<application ...>`
добавить атрибут:

```xml
        android:name=".BallDeFatoApp"
```

---

## Task 3: Иконка, тёмный фон, строки, кнопка темы

**Files:**
- Create: `app/src/main/res/drawable/ic_theme.xml`, `app/src/main/res/drawable-night/bg.xml`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Иконка темы (луна)**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/drawable/ic_theme.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="#FFFFFF">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 -0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4 -0.44,-0.06 -0.9,-0.1 -1.36,-0.1z" />
</vector>
```

- [ ] **Step 2: Тёмный фон (ночной вариант)**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/drawable-night/bg.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- Dark cosmic gradient; overrides the light photo bg.jpg in night mode. -->
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:type="radial"
        android:gradientRadius="640"
        android:centerX="0.5"
        android:centerY="0.42"
        android:startColor="#2A1E5C"
        android:centerColor="#141033"
        android:endColor="#07060F" />
</shape>
```

- [ ] **Step 3: Английские строки**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values/strings.xml` перед
`<string name="language_title">` добавить:

```xml
    <string name="theme_title">Theme</string>
    <string name="theme_system">System</string>
    <string name="theme_light">Light</string>
    <string name="theme_dark">Dark</string>
```

- [ ] **Step 4: Кнопка темы рядом с глобусом (ряд top|end)**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/layout/activity_main.xml`
заменить блок `languageButton`:

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
на ряд из двух кнопок:

```xml
    <LinearLayout
        android:id="@+id/topActions"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="top|end"
        android:layout_margin="4dp"
        android:orientation="horizontal">

        <ImageButton
            android:id="@+id/themeButton"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:alpha="0.75"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/theme_title"
            android:src="@drawable/ic_theme"
            app:tint="#FFFFFF" />

        <ImageButton
            android:id="@+id/languageButton"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:alpha="0.75"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/language_title"
            android:src="@drawable/ic_language"
            app:tint="#FFFFFF" />
    </LinearLayout>
```

---

## Task 4: Wiring в MainActivity

**Files:**
- Modify: `app/src/main/kotlin/ai/mifmax/balldefato/MainActivity.kt`

- [ ] **Step 1: Импорты**

Добавить:

```kotlin
import ai.mifmax.balldefato.logic.ThemeOptions
import ai.mifmax.balldefato.logic.ThemePrefs
```

- [ ] **Step 2: Инсет на ряд + клики**

Заменить блок инсетов и клика языка:

```kotlin
            (binding.languageButton.layoutParams as FrameLayout.LayoutParams).topMargin =
                top + (8 * density).toInt()
            binding.topBar.requestLayout()
            binding.languageButton.requestLayout()
            insets
        }

        binding.languageButton.setOnClickListener { showLanguageDialog() }
```
на:

```kotlin
            (binding.topActions.layoutParams as FrameLayout.LayoutParams).topMargin =
                top + (4 * density).toInt()
            binding.topBar.requestLayout()
            binding.topActions.requestLayout()
            insets
        }

        binding.languageButton.setOnClickListener { showLanguageDialog() }
        binding.themeButton.setOnClickListener { showThemeDialog() }
```

- [ ] **Step 3: showThemeDialog**

Добавить в класс (например, перед `showLanguageDialog`):

```kotlin
    private fun showThemeDialog() {
        val labels = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
        )
        val checked = ThemeOptions.indexForMode(ThemePrefs.load(this))
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BallDeFato_Dialog)
            .setTitle(R.string.theme_title)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                val mode = ThemeOptions.modeForIndex(which)
                ThemePrefs.save(this, mode)
                AppCompatDelegate.setDefaultNightMode(mode)
                dialog.dismiss()
            }
            .show()
    }
```

- [ ] **Step 4: Сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

---

## Task 5: Локализация строк темы (ru/de/es/fr)

**Files:**
- Modify: `app/src/main/res/values-{ru,de,es,fr}/strings.xml`

- [ ] **Step 1: Добавить theme_* в каждую локаль** (перед `<string name="language_title">`)

`values-ru/strings.xml`:
```xml
    <string name="theme_title">Тема</string>
    <string name="theme_system">Системная</string>
    <string name="theme_light">Светлая</string>
    <string name="theme_dark">Тёмная</string>
```
`values-de/strings.xml`:
```xml
    <string name="theme_title">Design</string>
    <string name="theme_system">System</string>
    <string name="theme_light">Hell</string>
    <string name="theme_dark">Dunkel</string>
```
`values-es/strings.xml`:
```xml
    <string name="theme_title">Tema</string>
    <string name="theme_system">Sistema</string>
    <string name="theme_light">Claro</string>
    <string name="theme_dark">Oscuro</string>
```
`values-fr/strings.xml`:
```xml
    <string name="theme_title">Thème</string>
    <string name="theme_system">Système</string>
    <string name="theme_light">Clair</string>
    <string name="theme_dark">Sombre</string>
```

- [ ] **Step 2: (опц.) непустота в LocalizationTest**

В `app/src/test/kotlin/ai/mifmax/balldefato/LocalizationTest.kt` в цикл добавить:
```kotlin
            assertThat(res.getString(R.string.theme_title)).isNotEmpty()
```

---

## Task 6: Полный прогон и проверка на устройстве

**Files:** нет постоянных изменений.

- [ ] **Step 1: Все тесты + сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`; тесты зелёные (22 прежних + ThemeOptionsTest 2).

- [ ] **Step 2: Устройство**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; D="192.168.0.166:5555"
"$ADB" connect 192.168.0.166:5555 >/dev/null 2>&1
"$ADB" -s "$D" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$D" shell am force-stop ai.mifmax.balldefato
"$ADB" -s "$D" shell am start -n ai.mifmax.balldefato/.MainActivity
```
Expected: в ряду сверху-справа — иконки темы и языка; выбор «Тёмная» меняет фон на
тёмный градиент и переживает перезапуск; «Светлая» — фото-фон; «Системная» — по телефону.

---

## Self-Review (выполнено при написании плана)

- **Покрытие спеки:** `ThemeOptions`+тест (T1), `ThemePrefs`+`BallDeFatoApp`+манифест (T2),
  иконка/тёмный фон/строки/кнопка (T3), диалог+wiring (T4), локализация (T5), прогон+
  устройство (T6). Персистентность — Application (T2). Пробелов нет.
- **Плейсхолдеры:** нет — весь код/иконка/градиент/строки приведены.
- **Согласованность:** `ThemeOptions.SYSTEM/LIGHT/DARK` = AppCompat MODE_* значения,
  передаются в `setDefaultNightMode`. `ThemePrefs.load/save` — как в диалоге и Application.
  `binding.topActions/themeButton/languageButton` соответствуют layout. Инсет переключён
  с `languageButton` на `topActions`.
