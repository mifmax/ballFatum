# Ball De Fato — M2: Локализация (en/ru/de/es/fr)

**Дата:** 2026-07-07
**Статус:** утверждён
**Милстоун:** M2 (после M1 + Тесты)

## Цель

Локализовать «Ball De Fato» на 5 языков — **английский (база/дефолт)**, русский,
немецкий, испанский, французский. Переключение — через OS per-app language
(Android 13+), без внутриигрового переключателя. Поведение приложения не меняется.

## Решения (утверждены)

| Вопрос | Решение |
|---|---|
| Языки | en (база), ru, de, es, fr |
| Дефолт/фолбэк | **Английский** — `values/` становится английским |
| Debug-настройки | **Не локализуем** — строки экрана настроек только в базе (англ.); на всех языках показываются по-английски |
| Переключение | OS per-app language, `generateLocaleConfig=true`, без in-app свитчера |
| `app_name` | «Ball De Fato» — бренд, одинаково во всех языках |

## Структура ресурсов

- `values/strings.xml` (**база = английский**): все строки, включая:
  - переводимые: `app_name`, `shake_me_caption`, `instruction_caption`,
    `eight_ball_description`, `menu_shake_caption`, `menu_preferences_caption`,
    `instructions` (10), `responses` (20);
  - **не переводимые** (остаются только здесь): pref-ключи `shake_count_id`,
    `threshold_id`, `vibrate_time_id`; строки debug-настроек
    (`preferences_section_title`, `shake_count_title/summary/dialogTitle`,
    `threshold_*`, `vibrate_time_*`).
- `values-ru/strings.xml`: русские переводы переводимых строк (текущий контент).
- `values-de/strings.xml`, `values-es/strings.xml`, `values-fr/strings.xml`:
  немецкий / испанский / французский переводы переводимых строк.
- Локали НЕ содержат pref-ключи и строки debug-настроек — те резолвятся из базы
  (английский), поэтому настройки везде на английском.

### Канонические английские ответы (база)

20 стандартных ответов Magic 8-Ball (в том же порядке = позитив/нейтрал/негатив):
It is certain · It is decidedly so · Without a doubt · Yes — definitely ·
You may rely on it · As I see it, yes · Most likely · Outlook good · Yes ·
Signs point to yes · Reply hazy, try again · Ask again later ·
Better not tell you now · Cannot predict now · Concentrate and ask again ·
Don't count on it · My reply is no · My sources say no · Outlook not so good ·
Very doubtful.

Русские — текущие; немецкие/испанские/французские — соответствующие переводы.
`instructions` (10 мистических подсказок гадалки) переводятся на все 5 языков.

## Механизм переключения языка

- `app/build.gradle.kts` → `android { androidResources { generateLocaleConfig = true } }`.
- `app/src/main/res/resources.properties` → `unqualifiedResLocale=en-US`.
- AGP генерирует `locales_config.xml` из `values-*` и вписывает
  `android:localeConfig` в манифест. Пользователь меняет язык в
  Настройки → Приложение → Язык (Android 13+); на старых — системный язык.

## Тестирование

- Robolectric-тест `LocalizationTest`: для каждого языка (`@Config(qualifiers=...)`
  или через `Configuration`) проверяет `resources.getStringArray(R.array.responses).size == 20`
  и `R.array.instructions.size == 10`, а также что ключевые строки
  (`shake_me_caption`) непусты. Ловит пропущенные/битые переводы-массивы.

## Вне scope

- Внутриигровой переключатель языка; RTL; локализация debug-настроек; изменение
  логики/поведения; расширение числа ответов (это M3).

## Критерии готовности

1. `values/` — английский; `values-ru/de/es/fr` — полные переводы переводимых строк.
2. `generateLocaleConfig` включён, `resources.properties` задаёт `en-US`; сборка
   генерирует `locales_config` и приложение показывает язык по выбору OS.
3. Все локали имеют `responses`=20 и `instructions`=10; debug-настройки на английском.
4. `./gradlew testDebugUnitTest` и `assembleDebug` — зелёные; проверка смены языка
   на устройстве (напр. `adb shell ... locale`) показывает переключение.
