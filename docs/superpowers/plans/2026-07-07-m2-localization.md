# M2 — Localization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Локализовать «Ball De Fato» на en (база), ru, de, es, fr через OS per-app language (`generateLocaleConfig`), без внутриигрового переключателя.

**Architecture:** `values/` становится английской базой (переводимые строки + не переводимые pref-ключи и англ. debug-настройки). Русский переезжает в `values-ru`, добавляются `values-de/es/fr` — только с переводимыми строками (остальное резолвится из базы). AGP генерирует `locales_config.xml`.

**Tech Stack:** Android string resources, AGP `androidResources.generateLocaleConfig`, `resources.properties`, Robolectric.

**Команды (JDK 17):** префикс `JAVA_HOME=$(/usr/libexec/java_home -v 17)` перед `./gradlew`. Коммиты делает пользователь (в этом проекте агент не коммитит) — шаги "Commit" опущены; вместо них в конце задач — проверка сборки.

---

## Структура файлов

```
app/build.gradle.kts                         (Task 1: generateLocaleConfig)
app/src/main/res/resources.properties        (Task 1: unqualifiedResLocale=en-US)
app/src/main/res/values/strings.xml          (Task 2: → английская база)
app/src/main/res/values-ru/strings.xml       (Task 3)
app/src/main/res/values-de/strings.xml       (Task 4)
app/src/main/res/values-es/strings.xml       (Task 5)
app/src/main/res/values-fr/strings.xml       (Task 6)
app/src/test/kotlin/ai/mifmax/balldefato/LocalizationTest.kt  (Task 7)
```

Переводимые строки (во всех локалях): `shake_me_caption`, `eight_ball_description`,
`menu_shake_caption`, `instruction_caption`, массивы `instructions` (10) и
`responses` (20). База-только (не в локалях): `app_name` (бренд),
`menu_preferences_caption` + весь debug-экран настроек + pref-ключи.

---

## Task 1: Включить generateLocaleConfig

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/res/resources.properties`

- [ ] **Step 1: Добавить androidResources в build.gradle**

В `/Users/mverakhovskiy/work/ballFatum/app/build.gradle.kts` внутри `android { ... }`
(после блока `buildFeatures { ... }`) добавить:

```kotlin
    androidResources {
        generateLocaleConfig = true
    }
```

- [ ] **Step 2: Задать дефолтную локаль**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/resources.properties`:

```properties
unqualifiedResLocale=en-US
```

- [ ] **Step 3: Проверить сборку**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`. (`locales_config.xml` появится после того, как будут созданы `values-*`.)

---

## Task 2: База `values/` → английский

**Files:**
- Modify (полная замена): `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Заменить весь файл на английскую базу**

Заменить весь `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values/strings.xml` на:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Ball De Fato</string>

    <!-- Shown in the ball when there is no accelerometer (rare). -->
    <string name="menu_shake_caption">Shake</string>
    <!-- Hidden debug settings screen title (English only). -->
    <string name="menu_preferences_caption">Settings</string>

    <string name="shake_me_caption">Shake\nme</string>
    <string name="eight_ball_description">Magic ball</string>
    <string name="instruction_caption">Make a wish that troubles your soul,\nand shake the ball — fate will answer…</string>

    <!-- ===== Hidden debug settings screen: English only, NOT localized ===== -->
    <string name="preferences_section_title">Magic 8-ball settings</string>
    <string name="shake_count_id">shakeCount</string>
    <string name="shake_count_title">Shake count</string>
    <string name="shake_count_summary">Number of shakes required to get an answer</string>
    <string name="shake_count_dialogTitle">Enter the number of shakes</string>
    <string name="threshold_id">threshold</string>
    <string name="threshold_title">Threshold</string>
    <string name="threshold_summary">Sensor sensitivity threshold</string>
    <string name="threshold_dialogTitle">Choose a value between 0.1 and 4.0</string>
    <string name="vibrate_time_id">vibrateTime</string>
    <string name="vibrate_time_title">Vibration time</string>
    <string name="vibrate_time_summary">How long the phone vibrates to signal an answer</string>
    <string name="vibrate_time_dialogTitle">Set the time in milliseconds</string>

    <string-array name="instructions">
        <item>Make a wish that troubles your soul,\nand shake the ball — fate will answer…</item>
        <item>Focus on your question\nand shake the ball of fate…</item>
        <item>Close your eyes, ask with your heart,\nand shake the oracle\'s ball…</item>
        <item>Ask what gives you no peace,\nand shake the ball — behold the answer…</item>
        <item>Whisper your question to the universe\nand shake the ball of fate…</item>
        <item>Think of the wish you keep silent,\nand shake the ball…</item>
        <item>Let the question into your heart\nand shake the ball — a prophecy is coming…</item>
        <item>Concentrate on what is hidden\nand shake the oracle\'s ball…</item>
        <item>Ask what burns inside you,\nand shake the ball — fate will speak…</item>
        <item>Trust in fate, make your question,\nand shake the ball of prophecy…</item>
    </string-array>

    <string-array name="responses">
        <item>As I see it,\nyes</item>
        <item>It is\ncertain</item>
        <item>It is\ndecidedly so</item>
        <item>Most\nlikely</item>
        <item>Outlook\ngood</item>
        <item>Signs point\nto yes</item>
        <item>Without\na doubt</item>
        <item>Yes</item>
        <item>Yes,\ndefinitely</item>
        <item>You may\nrely on it</item>
        <item>Reply hazy,\ntry again</item>
        <item>Ask again\nlater</item>
        <item>Better not\ntell you now</item>
        <item>Cannot\npredict now</item>
        <item>Concentrate\nand ask again</item>
        <item>Don\'t count\non it</item>
        <item>My reply\nis no</item>
        <item>My sources\nsay no</item>
        <item>Outlook\nnot so good</item>
        <item>Very\ndoubtful</item>
    </string-array>
</resources>
```

- [ ] **Step 2: Проверка** — `grep -c "<item>" app/src/main/res/values/strings.xml` → 30 (10 инструкций + 20 ответов).

---

## Task 3: `values-ru` (русский)

**Files:**
- Create: `app/src/main/res/values-ru/strings.xml`

- [ ] **Step 1: Создать файл (только переводимые строки)**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values-ru/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="menu_shake_caption">Трясти</string>
    <string name="shake_me_caption">Потряси\nменя</string>
    <string name="eight_ball_description">Магический шар</string>
    <string name="instruction_caption">Загадай вопрос, что тревожит душу,\nи встряхни шар — судьба ответит…</string>

    <string-array name="instructions">
        <item>Загадай вопрос, что тревожит душу,\nи встряхни шар — судьба ответит…</item>
        <item>Сосредоточься на своём вопросе\nи потряси шар судьбы…</item>
        <item>Закрой глаза, спроси сердцем\nи встряхни шар оракула…</item>
        <item>Спроси о том, что не даёт покоя,\nи потряси шар — узри ответ…</item>
        <item>Прошепчи свой вопрос вселенной\nи встряхни шар судьбы…</item>
        <item>Задумай желание, о котором молчишь,\nи потряси шар…</item>
        <item>Впусти вопрос в своё сердце\nи встряхни шар — грядёт пророчество…</item>
        <item>Сконцентрируйся на сокровенном\nи потряси шар оракула…</item>
        <item>Задай вопрос, что жжёт изнутри,\nи встряхни шар — судьба заговорит…</item>
        <item>Доверься судьбе, задумай вопрос\nи потряси шар предсказаний…</item>
    </string-array>

    <string-array name="responses">
        <item>Я вижу это\nкак Да</item>
        <item>Это\nфакт</item>
        <item>Это\nбесспорно\nтак</item>
        <item>Наиболее\nвероятно</item>
        <item>Перспектива\nхорошая</item>
        <item>Знаки\nговорят\nда</item>
        <item>Без\nсомнения</item>
        <item>Да</item>
        <item>Определенно\nда</item>
        <item>Вы можете\nположиться\nна это</item>
        <item>Туманный\nответ,\nпопробуй\nеще</item>
        <item>Спросите\nснова\nпозже</item>
        <item>Лучше\nне говорить\nВам\nсейчас</item>
        <item>Не могу\nпредсказать\nсейчас</item>
        <item>Сконцент-\nрируйтесь\nи спросите\nснова</item>
        <item>Не надейся\nна это</item>
        <item>Мой ответ\nнет</item>
        <item>Мои\nисточники\nговорят\nнет</item>
        <item>Перспектива\nне столь\nхорошая</item>
        <item>Думаю,\nне стоит</item>
    </string-array>
</resources>
```

---

## Task 4: `values-de` (немецкий)

**Files:**
- Create: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Создать файл**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values-de/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="menu_shake_caption">Schütteln</string>
    <string name="shake_me_caption">Schüttel\nmich</string>
    <string name="eight_ball_description">Magische Kugel</string>
    <string name="instruction_caption">Stelle eine Frage, die deine Seele bewegt,\nund schüttle die Kugel — das Schicksal antwortet…</string>

    <string-array name="instructions">
        <item>Stelle eine Frage, die deine Seele bewegt,\nund schüttle die Kugel — das Schicksal antwortet…</item>
        <item>Konzentriere dich auf deine Frage\nund schüttle die Kugel des Schicksals…</item>
        <item>Schließ die Augen, frag mit dem Herzen,\nund schüttle die Kugel des Orakels…</item>
        <item>Frag, was dir keine Ruhe lässt,\nund schüttle die Kugel — sieh die Antwort…</item>
        <item>Flüstere deine Frage dem Universum zu\nund schüttle die Kugel des Schicksals…</item>
        <item>Denk an den Wunsch, den du verschweigst,\nund schüttle die Kugel…</item>
        <item>Lass die Frage in dein Herz\nund schüttle die Kugel — eine Prophezeiung naht…</item>
        <item>Konzentriere dich auf das Verborgene\nund schüttle die Kugel des Orakels…</item>
        <item>Frag, was in dir brennt,\nund schüttle die Kugel — das Schicksal spricht…</item>
        <item>Vertraue dem Schicksal, stelle deine Frage\nund schüttle die Kugel der Prophezeiung…</item>
    </string-array>

    <string-array name="responses">
        <item>Wie ich es\nsehe, ja</item>
        <item>Es ist\nsicher</item>
        <item>Es ist\neindeutig so</item>
        <item>Höchst-\nwahrscheinlich</item>
        <item>Aussichten\ngut</item>
        <item>Zeichen deuten\nauf ja</item>
        <item>Ohne\nZweifel</item>
        <item>Ja</item>
        <item>Ja,\ndefinitiv</item>
        <item>Du kannst dich\ndarauf verlassen</item>
        <item>Antwort unklar,\nversuch es erneut</item>
        <item>Frag später\nnoch einmal</item>
        <item>Sage ich dir\njetzt besser nicht</item>
        <item>Kann es jetzt\nnicht vorhersagen</item>
        <item>Konzentriere dich\nund frag erneut</item>
        <item>Verlass dich\nnicht darauf</item>
        <item>Meine Antwort\nist nein</item>
        <item>Meine Quellen\nsagen nein</item>
        <item>Aussichten\nnicht so gut</item>
        <item>Sehr\nzweifelhaft</item>
    </string-array>
</resources>
```

---

## Task 5: `values-es` (испанский)

**Files:**
- Create: `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Создать файл**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values-es/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="menu_shake_caption">Agitar</string>
    <string name="shake_me_caption">Agítame</string>
    <string name="eight_ball_description">Bola mágica</string>
    <string name="instruction_caption">Pide un deseo que inquieta tu alma\ny agita la bola — el destino responderá…</string>

    <string-array name="instructions">
        <item>Pide un deseo que inquieta tu alma\ny agita la bola — el destino responderá…</item>
        <item>Concéntrate en tu pregunta\ny agita la bola del destino…</item>
        <item>Cierra los ojos, pregunta con el corazón\ny agita la bola del oráculo…</item>
        <item>Pregunta lo que no te da paz\ny agita la bola — contempla la respuesta…</item>
        <item>Susurra tu pregunta al universo\ny agita la bola del destino…</item>
        <item>Piensa en el deseo que callas\ny agita la bola…</item>
        <item>Deja entrar la pregunta en tu corazón\ny agita la bola — llega una profecía…</item>
        <item>Concéntrate en lo oculto\ny agita la bola del oráculo…</item>
        <item>Pregunta lo que arde en tu interior\ny agita la bola — el destino hablará…</item>
        <item>Confía en el destino, formula tu pregunta\ny agita la bola de las profecías…</item>
    </string-array>

    <string-array name="responses">
        <item>Según lo veo,\nsí</item>
        <item>Es\ncierto</item>
        <item>Definitiva-\nmente sí</item>
        <item>Muy\nprobablemente</item>
        <item>Buenas\nperspectivas</item>
        <item>Las señales\napuntan a sí</item>
        <item>Sin\nduda</item>
        <item>Sí</item>
        <item>Sí,\ndefinitivamente</item>
        <item>Puedes\nconfiar en ello</item>
        <item>Respuesta confusa,\ninténtalo de nuevo</item>
        <item>Pregunta de\nnuevo más tarde</item>
        <item>Mejor no\ndecírtelo ahora</item>
        <item>No puedo\npredecirlo ahora</item>
        <item>Concéntrate y\npregunta otra vez</item>
        <item>No cuentes\ncon ello</item>
        <item>Mi respuesta\nes no</item>
        <item>Mis fuentes\ndicen que no</item>
        <item>Perspectivas\nno tan buenas</item>
        <item>Muy\ndudoso</item>
    </string-array>
</resources>
```

---

## Task 6: `values-fr` (французский)

**Files:**
- Create: `app/src/main/res/values-fr/strings.xml`

- [ ] **Step 1: Создать файл** (апострофы экранированы `\'`)

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values-fr/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="menu_shake_caption">Secouer</string>
    <string name="shake_me_caption">Secoue-\nmoi</string>
    <string name="eight_ball_description">Boule magique</string>
    <string name="instruction_caption">Formule un vœu qui trouble ton âme\net secoue la boule — le destin répondra…</string>

    <string-array name="instructions">
        <item>Formule un vœu qui trouble ton âme\net secoue la boule — le destin répondra…</item>
        <item>Concentre-toi sur ta question\net secoue la boule du destin…</item>
        <item>Ferme les yeux, demande avec le cœur\net secoue la boule de l\'oracle…</item>
        <item>Demande ce qui te laisse sans repos\net secoue la boule — vois la réponse…</item>
        <item>Murmure ta question à l\'univers\net secoue la boule du destin…</item>
        <item>Pense au souhait que tu tais\net secoue la boule…</item>
        <item>Laisse la question entrer dans ton cœur\net secoue la boule — une prophétie approche…</item>
        <item>Concentre-toi sur ce qui est caché\net secoue la boule de l\'oracle…</item>
        <item>Demande ce qui brûle en toi\net secoue la boule — le destin parlera…</item>
        <item>Fais confiance au destin, formule ta question\net secoue la boule des prophéties…</item>
    </string-array>

    <string-array name="responses">
        <item>À mon avis,\noui</item>
        <item>C\'est\ncertain</item>
        <item>C\'est\nvraiment le cas</item>
        <item>Très\nprobablement</item>
        <item>Perspectives\nfavorables</item>
        <item>Les signes\nindiquent oui</item>
        <item>Sans aucun\ndoute</item>
        <item>Oui</item>
        <item>Oui,\nabsolument</item>
        <item>Tu peux\ncompter dessus</item>
        <item>Réponse floue,\nréessaie</item>
        <item>Redemande\nplus tard</item>
        <item>Mieux vaut ne pas\nte le dire maintenant</item>
        <item>Impossible de\nprédire maintenant</item>
        <item>Concentre-toi\net redemande</item>
        <item>N\'y compte\npas</item>
        <item>Ma réponse\nest non</item>
        <item>Mes sources\ndisent non</item>
        <item>Perspectives\npeu favorables</item>
        <item>Très\ndouteux</item>
    </string-array>
</resources>
```

---

## Task 7: Тест полноты локализации

**Files:**
- Create: `app/src/test/kotlin/ai/mifmax/balldefato/LocalizationTest.kt`

- [ ] **Step 1: Написать тест**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/test/kotlin/ai/mifmax/balldefato/LocalizationTest.kt`:

```kotlin
package ai.mifmax.balldefato

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LocalizationTest {

    private fun resFor(language: String): Resources {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale(language))
        return base.createConfigurationContext(config).resources
    }

    @Test
    fun everyLocaleHasCompleteArrays() {
        for (language in listOf("en", "ru", "de", "es", "fr")) {
            val res = resFor(language)
            assertThat(res.getStringArray(R.array.responses)).hasLength(20)
            assertThat(res.getStringArray(R.array.instructions)).hasLength(10)
            assertThat(res.getString(R.string.shake_me_caption)).isNotEmpty()
        }
    }

    @Test
    fun translatedLocalesActuallyDifferFromEnglishBase() {
        val en = resFor("en").getStringArray(R.array.responses)
        for (language in listOf("ru", "de", "es", "fr")) {
            val localized = resFor(language).getStringArray(R.array.responses)
            // If a locale file were missing, this would fall back to English and fail.
            assertThat(localized[0]).isNotEqualTo(en[0])
        }
    }
}
```

- [ ] **Step 2: Запустить тест**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest --tests "ai.mifmax.balldefato.LocalizationTest" --no-daemon
```
Expected: `BUILD SUCCESSFUL`, 2 теста зелёные.
Если `createConfigurationContext` в Robolectric не подхватит локаль — заменить на
`@Config(qualifiers = "ru")`/и т.д. на отдельные тест-методы (тот же смысл: проверить
размеры массивов на каждую локаль).

---

## Task 8: Сборка и проверка на устройстве

**Files:** нет изменений.

- [ ] **Step 1: Полный прогон тестов + сборка**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew testDebugUnitTest assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`; все тесты (12 прежних + 2 новых) зелёные.

- [ ] **Step 2: Проверить, что locales_config сгенерирован**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
find app/build -name "*locale*config*.xml" -o -name "_generated_res_locale_config*" 2>/dev/null | head
```
Expected: файл `xml/_generated_res_locale_config.xml` (или подобный) со списком локалей en/ru/de/es/fr.

- [ ] **Step 3: Установить и проверить смену языка на устройстве**

Run:
```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"; D="192.168.0.166:5555"
"$ADB" connect 192.168.0.166:5555 >/dev/null 2>&1
"$ADB" -s "$D" install -r app/build/outputs/apk/debug/app-debug.apk
# Задать язык приложения (Android 13+ per-app locale):
"$ADB" -s "$D" shell cmd locale set-app-locales ai.mifmax.balldefato --user current --locales de
"$ADB" -s "$D" shell am force-stop ai.mifmax.balldefato
"$ADB" -s "$D" shell am start -n ai.mifmax.balldefato/.MainActivity
```
Expected: приложение открывается на немецком (заголовок «Ball De Fato», подсказка/ответы на немецком). Повторить для `ru`/`es`/`fr` при желании. Вернуть: `... set-app-locales ai.mifmax.balldefato --user current --locales en`.

---

## Self-Review (выполнено при написании плана)

- **Покрытие спеки:** база-английский (T2), values-ru/de/es/fr (T3–T6), pref-ключи и
  debug-настройки только в базе (T2, не дублируются в локалях), `generateLocaleConfig`
  + `resources.properties` (T1), тест полноты массивов по всем локалям (T7), сборка +
  проверка смены языка на устройстве (T8). Пробелов нет.
- **Плейсхолдеры:** отсутствуют — все 5 файлов строк приведены целиком.
- **Согласованность:** массивы `responses` (20) и `instructions` (10) одинакового
  размера и порядка во всех локалях (индекс = один и тот же ответ). Апострофы во
  французском/английском экранированы `\'`. `app_name`/`menu_preferences_caption`/
  debug-строки присутствуют только в базе → на всех языках по-английски, как решено.
