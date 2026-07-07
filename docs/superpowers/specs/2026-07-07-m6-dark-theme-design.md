# Ball De Fato — M6: Тёмная тема

**Дата:** 2026-07-07
**Статус:** утверждён
**Милстоун:** M6 (после M5)

## Цель

Полноценная тёмная тема с переключателем в приложении (Системная/Светлая/Тёмная) и
тёмным вариантом фона главного экрана. Выбор сохраняется между запусками.

## Решения (утверждены)

| Вопрос | Решение |
|---|---|
| Управление | Переключатель в приложении (иконка рядом с глобусом): Системная/Светлая/Тёмная |
| Механизм | `AppCompatDelegate.setDefaultNightMode(...)` |
| Персистентность | SharedPreferences + применение в `Application.onCreate` |
| Фон в тёмной теме | `res/drawable-night/bg.xml` — тёмный космический радиальный градиент |
| Стиль диалога | Тот же `ThemeOverlay.BallDeFato.Dialog` |

## Компоненты

- **`ThemeOptions`** (чистый Kotlin, `logic`): значения совпадают с `AppCompatDelegate.MODE_*`.
  - `const val SYSTEM = -1`, `LIGHT = 1`, `DARK = 2`; `val MODES = listOf(SYSTEM, LIGHT, DARK)`.
  - `fun modeForIndex(index: Int): Int = MODES[index]`.
  - `fun indexForMode(mode: Int): Int` (неизвестное → 0/системная).
- **`ThemePrefs`** (`logic`): `load(context): Int` (по умолчанию SYSTEM), `save(context, mode)`
  в SharedPreferences (`theme`/`night_mode`).
- **`BallDeFatoApp : Application`**: в `onCreate` — `AppCompatDelegate.setDefaultNightMode(ThemePrefs.load(this))`.
  Регистрируется `android:name=".BallDeFatoApp"` в манифесте.
- **`MainActivity.showThemeDialog()`**: single-choice (Системная/Светлая/Тёмная), текущее
  из `ThemePrefs.load`; при выборе — `ThemePrefs.save` + `AppCompatDelegate.setDefaultNightMode(mode)`.
- **UI**: `ImageButton themeButton` (иконка `ic_theme.xml`, луна/яркость) рядом с
  `languageButton` в ряду `top|end`; инсет статус-бара применяется к ряду.
- **Фон**: `res/drawable-night/bg.xml` — `<shape>` с радиальным градиентом
  (`#2A1E5C` центр → `#141033` → `#07060F` края), перекрывает `bg.jpg` в ночном режиме.

## Строки (локализованы на en/ru/de/es/fr)

- `theme_title`, `theme_system`, `theme_light`, `theme_dark`.

## Тесты

- `ThemeOptionsTest` (JUnit5, чистый): `modeForIndex(0/1/2)` = SYSTEM/LIGHT/DARK;
  `indexForMode` обратно; неизвестный режим → 0.
- Локализация: `theme_title`/`theme_system` непусты во всех локалях (в `LocalizationTest`).

## Вне scope

- Ручная палитра, тема лендинга (M7), редизайн шара/иконок. Диалоги (M4/M5) уже тёмные.

## Критерии готовности

1. Кнопка темы открывает диалог; выбор меняет тему сразу и переживает перезапуск.
2. В тёмной теме фон главного экрана — тёмный градиент; в светлой — фото как было.
3. `ThemeOptions` + тест зелёный; `./gradlew testDebugUnitTest`/`assembleDebug` зелёные;
   проверка на устройстве.
