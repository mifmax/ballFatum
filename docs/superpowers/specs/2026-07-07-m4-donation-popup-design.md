# Ball De Fato — M4: Донат-попап

**Дата:** 2026-07-07
**Статус:** утверждён
**Милстоун:** M4 (после M3)

## Цель

Ненавязчиво предлагать поддержку разработчика: пока пользователь тряс шар в рамках
одной сессии, на «магических» числах трясок показывать всплывающий диалог с
мистическим текстом-просьбой и кнопкой доната.

## Решения (утверждены)

| Вопрос | Решение |
|---|---|
| Триггер | Счётчик трясок за сессию делится на **7 или 13**, И прошло **≥6** трясок с прошлого показа |
| Формат | **Material AlertDialog**: случайный «звёздный» текст + «Поддержать» / «Позже» |
| Тексты | **12 вариантов** «звёзды просят поддержать разработчика», локализованы на 5 языков; разные каждый раз |
| Счётчик | В рамках сессии; сбрасывается при выходе из приложения |
| Ссылка | Настраиваемая константа `DonationConfig.URL` (заглушка `https://ko-fi.com/mifmax`) |
| Платформа | Ko-fi (рекомендация) или Gumroad/иное — решает пользователь позже, меняется одной строкой |

## Компоненты

- **`DonationPromptPolicy`** (чистый Kotlin, `logic`): без Android.
  - `class DonationPromptPolicy(private val minGap: Int = 6)`; состояние
    `lastPromptCount: Int = 0`.
  - `fun shouldPrompt(shakeCount: Int): Boolean` — `true`, если
    `(shakeCount % 7 == 0 || shakeCount % 13 == 0) && shakeCount - lastPromptCount >= minGap`;
    при `true` присваивает `lastPromptCount = shakeCount`.
  - Тестируемо детерминированно.
- **`DonationConfig`** (объект): `const val URL = "https://ko-fi.com/mifmax"`.
- **`MainActivity`**:
  - Поле `shakeCount: Int = 0` (сессия). Инкремент при каждом ответе-по-тряске
    (в `onSensorChanged`, когда `shakeDetector` вернул `true`).
  - После инкремента: `if (donationPolicy.shouldPrompt(shakeCount)) showDonationDialog()`.
  - `showDonationDialog()`: `MaterialAlertDialogBuilder` с случайным текстом из
    `R.array.donation_prompts` (через `AnswerPicker`/`Random`), кнопки
    `donate_action` → `startActivity(Intent(ACTION_VIEW, Uri.parse(DonationConfig.URL)))`
    (обёрнуто в try/catch на случай отсутствия браузера), `donate_later` → dismiss.
  - `android:configChanges="orientation|screenSize"` у активити в манифесте, чтобы
    поворот не сбрасывал сессионный счётчик.

## Строки (локализованы на en/ru/de/es/fr)

- `donate_action` («Поддержать» / «Support» / …), `donate_later` («Позже» / …),
  `donate_title` (заголовок диалога, напр. «Звёзды шепчут…»).
- `string-array donation_prompts` — 12 мистических просьб в духе «звёзды просят
  поддержать того, кто создал этот шар…». В базе (en) + во всех локалях (как в M2:
  переводимые строки в `values-*`, размер массива одинаков).

## Тесты

- `DonationPromptPolicyTest` (JUnit5, чистый):
  - Кратные 7 и 13 при достаточном зазоре → `true` (7, 13→нет т.к. gap<6? 13-7=6 ок →
    true; 14 → 14-13=1 <6 → false; 21 → 21-13=8 → true; 26 → 26-21=5 → false; 28 →
    28-21=7 → true).
  - Не-кратные (1..6, 8..12, 15, 16…) → всегда `false`.
- Локализация: `donation_prompts` одинакового размера во всех локалях (расширить
  существующий `LocalizationTest` или отдельный тест).

## Вне scope

- Аналитика показов/кликов, A/B, реальная платёжная интеграция (только внешняя
  ссылка), частота между сессиями (сброс на выходе), выбор языка в приложении (это M5).

## Критерии готовности

1. `DonationPromptPolicy` + тест зелёный; правило 7/13 с зазором ≥6 работает.
2. Диалог показывается на нужных числах, ведёт на `DonationConfig.URL`, тексты
   локализованы (12 вариантов ×5 языков), кнопки переведены.
3. Счётчик сессионный (сброс на выходе; поворот не сбрасывает).
4. `./gradlew testDebugUnitTest` и `assembleDebug` зелёные; проверка попапа на устройстве.
