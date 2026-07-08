<div align="center">

# 🎱 Ball De Fato

**Ask the ball a question, shake your phone — and let fate answer.**

A modern take on the classic Magic 8-Ball, rebuilt from an old Eclipse ADT project
into a clean, tested Kotlin + Android app.

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![Language](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?logo=kotlin&logoColor=white)](#)
[![minSdk](https://img.shields.io/badge/minSdk-24-blue)](#)
[![targetSdk](https://img.shields.io/badge/targetSdk-36-blue)](#)
[![Tests](https://img.shields.io/badge/tests-passing-brightgreen)](#-testing)

</div>

---

## ✨ Features

- 🎱 **Shake to answer** — accelerometer-based shake detection with haptic feedback
- 🗂️ **2000 answers per language** — a hand-tuned bank of native responses (tone mix 2:1:1) with graceful fallback to the built-in classics
- 🌍 **5 languages** — English, Русский, Deutsch, Español, Français, with an in-app switcher (globe button) on top of Android per-app locale support
- 🌗 **Light / Dark / System theme** — in-app picker (moon button), persisted across launches, with a dedicated dark cosmic background
- ⚙️ **Tunable physics** — hidden debug screen to adjust shake count, sensor threshold, and vibration time
- ☕ **Support the dev** — an unobtrusive, occasional Ko-fi prompt with localized copy
- 🧪 **Fully tested** — pure logic extracted and covered by JUnit 5, activities verified with Robolectric

## 📱 How it works

1. Open the app and read the oracle's prompt.
2. Make a wish that troubles your soul.
3. Shake the phone — the ball vibrates and reveals fate's answer.

> Tip: no accelerometer? A tap works too.

## 🛠️ Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin 2.1.10 (JVM 17) |
| Build | Gradle 8.11.1 (Kotlin DSL) + version catalog · AGP 8.9.1 |
| UI | AppCompat · Material Components · ViewBinding |
| Settings | `androidx.preference` (`PreferenceFragmentCompat`) |
| Theming / locale | `AppCompatDelegate` (night mode + application locales) |
| Testing | JUnit 5 (Jupiter) · JUnit 4 (Vintage) · Robolectric · Truth |
