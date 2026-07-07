# Ball De Fato — тесты на весь функционал (милстоун «Тесты»)

**Дата:** 2026-07-07
**Статус:** утверждён
**Ветка:** `m1-gradle-kotlin-migration` (после M1; PR/коммит на границе M2 делает пользователь)

## Цель

Покрыть тестами весь функционал приложения после перевода на Kotlin. Для этого
вынести «зашитую» в `MainActivity` логику в чистые тестируемые классы и добавить
два уровня тестов — чистые JUnit5 и Robolectric (оба на JVM, без устройства, т.к.
adb/эмулятор в среде недоступны). Поведение приложения сохраняется 1:1.

## Порядок милстоунов

M1 (миграция, готов) → **Тесты** (эта спека) → M2 (i18n) → M3 (ответы ×100).

## Решения (утверждены)

| Вопрос | Решение |
|---|---|
| Охват | Unit + Robolectric (без instrumented/Espresso — нет устройства) |
| Рефакторинг | Да: вынести `ShakeDetector`, `AnswerPicker`, `Vibrations` |
| Фреймворк | JUnit5 (Jupiter); Robolectric-тесты как JUnit4 под `junit-vintage-engine` |

## Рефакторинг (поведение 1:1)

Новый пакет `ai.mifmax.balldefato.logic`:

- **`ShakeDetector`** — чистый Kotlin, без зависимостей от Android.
  - Состояние: `lastX`, `lastY`, `lastZ` (Float), `shakeCount` (Int).
  - `fun onSample(x: Float, y: Float, z: Float, threshold: Float, shakeCountLimit: Int): Boolean`
    — дословный перенос `isShakeEnough`: считает силу по трём осям через
    `pow`/`sqrt`, обновляет `lastX/Y/Z`; если сила > `threshold`, инкрементит
    `shakeCount`; если `shakeCount > shakeCountLimit` — сбрасывает состояние и
    возвращает `true`.
  - Константа гравитации локальная: `private const val GRAVITY_EARTH = 9.80665f`
    (значение `SensorManager.GRAVITY_EARTH`) — чтобы класс тестировался чистым
    JUnit5 без Robolectric.
  - Конфиг (`threshold`, `shakeCountLimit`) передаётся в каждый вызов — как в
    оригинале, где значения читались из prefs на каждый сэмпл (живые изменения
    настроек сохраняются).

- **`AnswerPicker`** — чистый Kotlin.
  - `class AnswerPicker(private val answers: List<String>, private val random: Random = Random.Default)`
  - `fun pick(): String = answers[random.nextInt(answers.size)]`
  - RNG инъектируется → детерминированные тесты (seeded/fake `Random`).

- **`Vibrations`** — хелпер (object) для версионно-корректной вибрации.
  - `fun defaultVibrator(context: Context): Vibrator` — API 31+ через
    `VibratorManager.defaultVibrator`, иначе `VIBRATOR_SERVICE`.
  - `fun oneShot(vibrator: Vibrator, millis: Long)` — при `millis <= 0` ничего не
    делает; API 26+ `VibrationEffect.createOneShot`, иначе deprecated `vibrate(ms)`.

`MainActivity` использует `ShakeDetector` (создаётся один раз), `AnswerPicker`
(создаётся из `resources.getStringArray(R.array.responses).toList()`), `Vibrations`.
Чтение prefs (`threshold`/`shakeCount`/`vibrateTime`) остаётся в активности и
передаётся в `ShakeDetector.onSample` / `Vibrations.oneShot`. Логика неизменна.

## Стек тестов

- **JUnit5 (Jupiter)** — чистая логика.
- **Robolectric** — Activity/Settings/вибрация. Тесты пишутся как JUnit4
  (`@RunWith(RobolectricTestRunner)`), исполняются под JUnit Platform через
  `junit-vintage-engine`. Оба движка (jupiter + vintage) работают под одним
  `useJUnitPlatform()`.
- `app/build.gradle.kts`:
  ```kotlin
  android {
      testOptions {
          unitTests {
              isIncludeAndroidResources = true
              all { it.useJUnitPlatform() }
          }
      }
  }
  ```
- `app/src/test/resources/robolectric.properties` → `sdk=34` (Robolectric 4.14 ещё
  не поддерживает SDK 36; поведение проверяем на 34, при minSdk 24).

### Зависимости (version catalog)

| Артефакт | Версия |
|---|---|
| org.junit.jupiter:junit-jupiter | 5.11.4 |
| org.junit.platform:junit-platform-launcher | 1.11.4 |
| org.junit.vintage:junit-vintage-engine | 5.11.4 |
| junit:junit | 4.13.2 |
| org.robolectric:robolectric | 4.14.1 |
| androidx.test:core | 1.6.1 |
| androidx.test.ext:junit | 1.2.1 |
| com.google.truth:truth | 1.4.4 |

Конфигурации: `testImplementation` для jupiter/junit4/robolectric/androidx-test/
truth; `testRuntimeOnly` для `junit-platform-launcher` и `junit-vintage-engine`.

## Покрытие

**Чистые (JUnit5), `app/src/test/kotlin/ai/mifmax/balldefato/logic/`:**
- `ShakeDetectorTest`:
  - Слабые сэмплы (сила ≤ threshold) → всегда `false`.
  - Сильные сэмплы: `false`, пока `shakeCount` не превысит `shakeCountLimit`;
    на `limit+1`-м сильном сэмпле → `true`.
  - После срабатывания состояние сброшено: следующий цикл снова требует накопления.
- `AnswerPickerTest`:
  - С `Random(seed)` — `pick()` возвращает элемент по предсказуемому индексу.
  - Индекс всегда в границах; список из одного элемента → этот элемент.

**Robolectric (JUnit4/vintage), `app/src/test/kotlin/ai/mifmax/balldefato/`:**
- `MainActivityTest`:
  - После запуска `MessageTextView` непустой.
  - `clickMenuItem(R.id.shake)` → текст `MessageTextView` содержится в массиве
    `responses`.
  - `clickMenuItem(R.id.preferences)` → следующая запущенная активность —
    `SettingsActivity` (через `ShadowActivity.nextStartedActivity`).
- `SettingsActivityTest`:
  - Активность стартует, `PreferenceFragmentCompat` загружен, `findPreference`
    для ключей `shakeCount`/`threshold`/`vibrateTime` не `null`.
- `VibrationsTest`:
  - `oneShot(vibrator, 200)` → шэдоу-вибратор зафиксировал вибрацию.
  - `oneShot(vibrator, 0)` → вибрации нет.

## Критерий готовности

`./gradlew testDebugUnitTest` (JDK 17) — **BUILD SUCCESSFUL**, все тесты зелёные. Классы логики (`ShakeDetector`,
`AnswerPicker`, `Vibrations`) и активности (`MainActivity`, `SettingsActivity`)
покрыты.

## Вне scope

- Instrumented/Espresso-тесты (нет устройства; не пишем).
- Изменения поведения, UI, набора ответов или ключей настроек.
- Покрытие тривиального геттера-хелпера ради метрики coverage ради метрики.
