# Ball De Fato — M5: Выбор языка в приложении

**Дата:** 2026-07-07
**Статус:** утверждён
**Милстоун:** M5 (после M4)

## Цель

Дать пользователю выбирать язык интерфейса и ответов прямо в приложении (в дополнение
к OS per-app language из M2), через `AppCompatDelegate.setApplicationLocales` — работает
и на Android <13.

## Решения (утверждены)

| Вопрос | Решение |
|---|---|
| Триггер | Иконка-глобус в верхнем углу (справа), полупрозрачная, инсет-aware |
| Список | Системный + English / Русский / Deutsch / Español / Français (эндонимы) |
| Механизм | `AppCompatDelegate.setApplicationLocales(LocaleListCompat)`; пусто = системный |
| Персистентность | Авто-хранение AppCompat (манифест-сервис `autoStoreLocales=true` для <13; ОС на 13+) |
| Стиль диалога | Тот же `ThemeOverlay.BallDeFato.Dialog` (тёмный полупрозрачный) |

## Компоненты

- **`LanguageOptions`** (чистый Kotlin, `logic`):
  - `val TAGS: List<String?> = listOf(null, "en", "ru", "de", "es", "fr")` (null = система).
  - `val ENDONYMS: List<String> = listOf("English", "Русский", "Deutsch", "Español", "Français")`.
  - `fun tagForIndex(index: Int): String?`.
  - `fun indexForCurrent(currentTag: String?): Int` — «de-DE»→«de», пусто/несовпадение → 0
    (системный).
  - Тестируемо детерминированно.
- **Иконка**: векторный `res/drawable/ic_language.xml` (глобус). В `activity_main.xml`
  `ImageButton` (id `languageButton`) в `top|end`, тинт белый ~70%, безрамочный ripple,
  `contentDescription=@string/language_title`. Верхний отступ = инсет статус-бара
  (через `setOnApplyWindowInsetsListener`, как у `topBar`).
- **`MainActivity.showLanguageDialog()`**: строит `MaterialAlertDialogBuilder(this,
  R.style.ThemeOverlay_BallDeFato_Dialog)` с `setSingleChoiceItems`:
  - items = `[getString(R.string.system_default)] + LanguageOptions.ENDONYMS`;
  - checked = `LanguageOptions.indexForCurrent(AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { null })`;
  - при выборе: `tag = LanguageOptions.tagForIndex(which)`;
    `AppCompatDelegate.setApplicationLocales(if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))`;
    диалог закрыть. Активити пересоздастся → строки и пул ответов (грузится по локали в
    `onCreate`) обновятся.
- **Манифест**: сервис для авто-хранения локали на <13:
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

## Строки (локализованы на en/ru/de/es/fr)

- `language_title` («Language»/«Язык»/«Sprache»/«Idioma»/«Langue»).
- `system_default` («System default»/«Системный»/«Systemstandard»/«Predeterminado del sistema»/«Système»).
- Эндонимы языков — константы в `LanguageOptions`, перевод не нужен.

## Тесты

- `LanguageOptionsTest` (JUnit5, чистый): `tagForIndex` возвращает корректные теги;
  `indexForCurrent("de")`=3, `indexForCurrent("de-DE")`=3, `indexForCurrent(null)`=0,
  `indexForCurrent("")`=0, `indexForCurrent("xx")`=0.
- Локализация: `language_title`/`system_default` присутствуют во всех локалях
  (резолвятся; можно проверить непустоту в `LocalizationTest`).

## Вне scope

- Флаги, поиск по языкам, изменение других экранов; скрытые настройки (шар долгим
  тапом) не трогаем.

## Критерии готовности

1. Глобус открывает диалог; выбор языка меняет UI и пул ответов; «Системный» сбрасывает
   на язык телефона.
2. Выбор сохраняется между запусками (`AppCompatDelegate` авто-хранение).
3. `LanguageOptions` + тест зелёный; `./gradlew testDebugUnitTest`/`assembleDebug` зелёные;
   проверка переключения на устройстве.
