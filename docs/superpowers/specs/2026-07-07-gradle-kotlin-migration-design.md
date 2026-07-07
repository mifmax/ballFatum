# Ball De Fato — миграция на Gradle + Kotlin, пакет ai.mifmax

**Дата:** 2026-07-07
**Статус:** утверждён

## Цель

«Оживить» старый Eclipse ADT Android-проект «Ball De Fato» (магический шар / Magic
8-Ball): перевести сборку на Gradle (Kotlin DSL + version catalog), исходники на
Kotlin, сменить пакет `com.Maximv.*` → `ai.mifmax.*` и модернизировать устаревшие/
удалённые Android API. **Поведение приложения сохраняется 1:1.**

## Милстоуны (общий план проекта)

Данная спека покрывает **M1**. M2/M3 получат отдельные спеки/планы позже.

- **M1 — Миграция** (эта спека): Gradle+Kotlin, пакет `ai.mifmax`, модернизация API.
  Поведение и контент без изменений (по-прежнему один русский язык, 20 ответов).
- **M2 — Локализация** (ru/en/de/es/fr): вынести все строки, добавить `values-en`,
  `values-de`, `values-es`, `values-fr`; перевести UI и текущие 20 ответов.
  Переключение языка — через OS per-app language (`androidResources {
  generateLocaleConfig = true }`, Android 13+; на старых — системный язык), без
  внутриигрового переключателя. В M2 в `app/build.gradle.kts` добавится
  `generateLocaleConfig` и `resources.properties` с дефолтной локалью.
- **M3 — Расширение банка ответов** до ~2000 на каждый из 5 языков (×100 от
  исходных 20). Объём ~10 000 строк — генерируется отдельным процессом
  (многоагентный workflow) с проверкой уникальности/качества; тональные группы
  (позитив/нейтрал/негатив) сохраняются.

## Что делает приложение (сохраняем без изменений)

- Тряска телефона (акселерометр) **или** пункт меню «Трясти» → случайный ответ из
  20-строчного массива `responses` (`res/values/strings.xml`, русский язык).
- Ответ появляется в `TextView` поверх картинки шара с анимацией плавного
  появления (`AlphaAnimation`) + вибрация.
- Экран настроек: 3 `EditTextPreference` — `shakeCount`, `threshold`,
  `vibrateTime`. Ключи, дефолты, тексты — прежние.
- Меню в ActionBar: Settings / Трясти / Настройки.
- Логика детекции тряски (порог силы по акселерометру, счётчик тряски) — без
  изменений.

## Решения (утверждены пользователем)

| Вопрос | Решение |
|---|---|
| Глубина модернизации | Полная (AndroidX, PreferenceFragmentCompat, VibrationEffect, ViewBinding) |
| Структура пакетов | Сохранить подпакеты: `ai.mifmax.balldefato` + `ai.mifmax.constants` |
| SDK | `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36` |
| Сборка | Gradle Kotlin DSL + version catalog (`libs.*`), стиль референс-проекта `jigsawhint` |
| Java/Kotlin target | Java 17 / `JvmTarget.JVM_17` |
| `versionName` / `versionCode` | `"2026.07.1"` / `2` |
| Тема | Material3 (`Theme.Material3.DayNight`) |

## Целевая структура проекта

```
settings.gradle.kts
build.gradle.kts                 # root: plugins { ... } apply false
gradle.properties
gradle/libs.versions.toml        # version catalog
gradle/wrapper/gradle-wrapper.properties
gradle/wrapper/gradle-wrapper.jar
gradlew  gradlew.bat             # Gradle 8.x wrapper
.gitignore                       # дополнить Gradle/Android-записями
app/
  build.gradle.kts
  proguard-rules.pro
  src/main/
    AndroidManifest.xml
    java/ai/mifmax/balldefato/MainActivity.kt
    java/ai/mifmax/balldefato/SettingsActivity.kt   # Activity + PreferenceFragmentCompat
    java/ai/mifmax/constants/GlobalConstants.kt
    res/                          # перенос текущего res/ как есть (+ мелкая чистка)
```

Старые Eclipse-артефакты удаляются: `project.properties`, `proguard-project.txt`
(заменяется на `app/proguard-rules.pro`), `libs/android-support-v4.jar`,
корневые `AndroidManifest.xml`/`res`/`src` (переезжают в `app/`).

## Сборка

### `gradle/libs.versions.toml`
Плагины: `android.application` (AGP 8.x), `kotlin.android` (Kotlin 2.x).
Библиотеки: `androidx.core:core-ktx`, `androidx.appcompat:appcompat`,
`com.google.android.material:material`, `androidx.preference:preference-ktx`.

### `app/build.gradle.kts` (по образцу jigsawhint)
```kotlin
android {
    namespace = "ai.mifmax.balldefato"
    compileSdk = 36
    defaultConfig {
        applicationId = "ai.mifmax.balldefato"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2026.07.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { viewBinding = true }
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
```

## Модернизация API

- **`MainActivity` (Kotlin, `AppCompatActivity`, `SensorEventListener`)**
  - ViewBinding вместо `findViewById`.
  - Массив ответов через `resources.getStringArray(R.array.responses)`.
  - Меню — `onCreateOptionsMenu` / `onOptionsItemSelected` (в AppCompat не
    deprecated). Пункт «Трясти» → показать ответ; «Настройки» → запустить
    `SettingsActivity`.
  - `SharedPreferences` через `androidx.preference.PreferenceManager
    .getDefaultSharedPreferences(this)`.
  - Логика `isShakeEnough` / сенсора переносится дословно.
- **`SettingsActivity` (Kotlin, `AppCompatActivity`)** + вложенный
  `SettingsFragment : PreferenceFragmentCompat`, грузит существующий
  `res/xml/preferences.xml` через `setPreferencesFromResource`. Заменяет удалённый
  `android.preference.PreferenceActivity`.
- **Вибрация** — хелпер с версионными ветками:
  - API 31+: `getSystemService(VibratorManager)`→`defaultVibrator`; иначе
    `getSystemService(VIBRATOR_SERVICE) as Vibrator`.
  - API 26+: `vibrate(VibrationEffect.createOneShot(ms, DEFAULT_AMPLITUDE))`;
    на 24/25 — `@Suppress("DEPRECATION") vibrate(ms)`.
- **`GlobalConstants`** → Kotlin `object` с теми же значениями
  (`FADE_DURATION=1500`, `START_OFFSET=1000`, `VIBRATE_TIME="250"`,
  `THRESHOLD="2.75"`, `SHAKE_COUNT="4"`).

## Ресурсы и манифест

- Перенести `res/` в `app/src/main/res/` без изменения содержимого массива ответов
  и drawables (`bg`, `eight_ball`, `triangle`, иконки).
- Мелкая чистка в `activity_main.xml`: `fill_parent`→`match_parent`, `dip`→`dp`,
  битый `android:contentDescription="@drawable/eight_ball"` → строковый ресурс.
- `res/values/styles.xml`: старую `AppBaseTheme`/`AppTheme` (parent
  `android:Theme.Light`) заменить темой на базе `Theme.Material3.DayNight` (с
  ActionBar для меню). `app_name` = «Ball De Fato».
- `AndroidManifest.xml`: убрать атрибут `package`, `uses-sdk` (уезжают в Gradle);
  сохранить `uses-permission VIBRATE`; объявить `MainActivity` (launcher,
  `android:exported="true"`) и `SettingsActivity`; ссылку на тему обновить.

## Вне scope

- Новые фичи, изменения текстов/ответов/ключей настроек.
- Юнит-тесты и выделение логики тряски в отдельный класс (возможно позже; исходник
  тестов не имел).
- Изменение UX/вёрстки помимо перечисленной мелкой чистки.

## Критерии готовности

1. Проект имеет валидную структуру Gradle-модуля с wrapper (Gradle 8.x).
2. `./gradlew assembleDebug` собирает APK (при наличии Android SDK и
   `local.properties`/`ANDROID_HOME`). Если SDK недоступен в среде — фиксируем это
   и подтверждаем сборку конфигурации/синтаксиса доступными средствами.
3. Все исходники — Kotlin в пакетах `ai.mifmax.balldefato` / `ai.mifmax.constants`;
   ссылок на `com.Maximv.*` и support-v4 не осталось.
4. Поведение (тряска → ответ, меню, настройки, вибрация) эквивалентно оригиналу.
