# Ball De Fato — Gradle+Kotlin Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Перевести старый Eclipse ADT Android-проект «Ball De Fato» на Gradle (Kotlin DSL + version catalog) и Kotlin, сменить пакет `com.Maximv.*` → `ai.mifmax.*`, модернизировать устаревшие API — при неизменном поведении.

**Architecture:** Стандартный single-module AGP-проект (`:app`). Исходники — Kotlin в `ai.mifmax.balldefato` / `ai.mifmax.constants`. AppCompat + Material3, ViewBinding, `PreferenceFragmentCompat`, версионно-корректная вибрация. Ресурсы переезжают из корня в `app/src/main/res` почти без изменений.

**Tech Stack:** AGP 8.9.1, Gradle 8.11.1, Kotlin 2.1.10, JDK 17 target (сборка запускается на JDK 21), compileSdk/targetSdk 36, minSdk 24, AndroidX (core-ktx, appcompat, material, preference-ktx).

**Замечание про верификацию:** это build-миграция UI/сенсорного приложения — юнит-тесты не добавляем (исходник их не имел, логика завязана на Android-фреймворк). Сигнал верификации = успешный `./gradlew assembleDebug` + `lint` + построчная эквивалентность поведения оригиналу. Проект впервые становится собираемым только после появления всего скелета (Task 8), поэтому ранние задачи заканчиваются коммитом без сборки.

**Фиксированные пути окружения (эта машина):**
- JDK 8: `/Users/mverakhovskiy/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home`
- JDK 21: `/Users/mverakhovskiy/Library/Java/JavaVirtualMachines/openjdk-21.0.2/Contents/Home`
- Android SDK: `/Users/mverakhovskiy/Library/Android/sdk`

---

## Целевая структура файлов

```
settings.gradle.kts            (Task 2)
build.gradle.kts               (Task 2)  root
gradle.properties              (Task 2)
gradle/libs.versions.toml      (Task 2)
gradlew, gradlew.bat           (Task 3, генерируются)
gradle/wrapper/…               (Task 3, генерируются)
.gitignore                     (Task 1, дополняется)
app/
  build.gradle.kts             (Task 4)
  proguard-rules.pro           (Task 4)
  src/main/
    AndroidManifest.xml        (Task 7)
    java/ai/mifmax/balldefato/MainActivity.kt      (Task 6)
    java/ai/mifmax/balldefato/SettingsActivity.kt  (Task 6)
    java/ai/mifmax/constants/GlobalConstants.kt    (Task 5)
    res/…                      (Task 1 перенос + Task 7 чистка)
local.properties               (Task 8, gitignored)
```

Удаляются (Task 1 / Task 9): корневые `src/`, `AndroidManifest.xml`, `project.properties`, `proguard-project.txt`, `libs/android-support-v4.jar`, `ic_launcher-web.png`.

---

## Task 1: Перенос ресурсов и очистка Eclipse-структуры

**Files:**
- Move: `res/` → `app/src/main/res/`
- Modify: `.gitignore`
- Delete: `libs/android-support-v4.jar`, `ic_launcher-web.png`, `project.properties`, `proguard-project.txt`

- [ ] **Step 1: Перенести ресурсы и удалить Eclipse-мусор через git**

```bash
cd /Users/mverakhovskiy/work/ballFatum
mkdir -p app/src/main
git mv res app/src/main/res
git rm -r --cached libs >/dev/null 2>&1 || true
rm -rf libs
git rm --cached ic_launcher-web.png project.properties proguard-project.txt 2>/dev/null || true
rm -f ic_launcher-web.png project.properties proguard-project.txt
```

Примечание: корневые `src/` и `AndroidManifest.xml` НЕ трогаем здесь — они удаляются в Task 9 после того, как их заменят Kotlin-версии (чтобы всегда была точка отката).

- [ ] **Step 2: Дополнить `.gitignore` записями Gradle/Android**

Заменить весь файл `/Users/mverakhovskiy/work/ballFatum/.gitignore` на:

```gitignore
# built application files
*.apk
*.ap_
*.aab

# files for the dex VM
*.dex

# Java class files
*.class

# generated files
bin/
gen/
build/
.gradle/

# Local configuration file (sdk path, etc)
local.properties

# Android Studio / IntelliJ
*.iml
.idea/

# Keystore
*.jks
*.keystore
```

- [ ] **Step 3: Проверить, что ресурсы на месте**

Run: `ls app/src/main/res && ls app/src/main/res/values`
Expected: видны `drawable/ layout/ menu/ values/ ...` и `strings.xml styles.xml`.

- [ ] **Step 4: Commit**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add -A
git commit -m "chore: move resources into app module, drop Eclipse artifacts

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Корневые Gradle-файлы и version catalog

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`

- [ ] **Step 1: `settings.gradle.kts`**

Create `/Users/mverakhovskiy/work/ballFatum/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BallDeFato"
include(":app")
```

- [ ] **Step 2: Корневой `build.gradle.kts`**

Create `/Users/mverakhovskiy/work/ballFatum/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

- [ ] **Step 3: `gradle.properties`**

Create `/Users/mverakhovskiy/work/ballFatum/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
# Safety net: allow compileSdk 36 even if the resolved AGP flags it as "not fully tested".
android.suppressUnsupportedCompileSdk=36
```

- [ ] **Step 4: `gradle/libs.versions.toml`**

Create `/Users/mverakhovskiy/work/ballFatum/gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.9.1"
kotlin = "2.1.10"
coreKtx = "1.13.1"
appcompat = "1.7.0"
material = "1.12.0"
preference = "1.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-preference-ktx = { group = "androidx.preference", name = "preference-ktx", version.ref = "preference" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

- [ ] **Step 5: Commit**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml
git commit -m "build: add root Gradle config and version catalog

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Gradle wrapper (8.11.1)

**Files:**
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Сгенерировать wrapper старым gradle на JDK 8**

Системный `gradle` = 4.9 и работает только на JDK 8; он умеет создать wrapper под любую версию.

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=/Users/mverakhovskiy/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home \
  gradle wrapper --gradle-version 8.11.1 --distribution-type bin
```
Expected: создаются `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`; в properties строка `distributionUrl=...gradle-8.11.1-bin.zip`.

Fallback (если системный gradle недоступен/падает): скачать wrapper-скрипты и jar из дистрибутива —
```bash
curl -sSL https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradlew -o gradlew
curl -sSL https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradlew.bat -o gradlew.bat
mkdir -p gradle/wrapper
curl -sSL https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.properties -o gradle/wrapper/gradle-wrapper.properties
curl -sSL https://github.com/gradle/gradle/raw/v8.11.1/gradle/wrapper/gradle-wrapper.jar -o gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew
```

- [ ] **Step 2: Проверить, что wrapper бутстрапится на JDK 21**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=/Users/mverakhovskiy/Library/Java/JavaVirtualMachines/openjdk-21.0.2/Contents/Home \
  ./gradlew --version
```
Expected: `Gradle 8.11.1`, `JVM: 21.0.2`. (Первый запуск скачивает дистрибутив — нужна сеть.)

- [ ] **Step 3: Commit**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties
git commit -m "build: add Gradle 8.11.1 wrapper

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Конфигурация модуля `app`

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: `app/build.gradle.kts`**

Create `/Users/mverakhovskiy/work/ballFatum/app/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference.ktx)
}
```

- [ ] **Step 2: `app/proguard-rules.pro`**

Create `/Users/mverakhovskiy/work/ballFatum/app/proguard-rules.pro`:

```proguard
# Ball De Fato — no custom keep rules needed (no reflection, no serialization).
# Minification is disabled for the release build (see build.gradle.kts).
```

- [ ] **Step 3: Commit**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add app/build.gradle.kts app/proguard-rules.pro
git commit -m "build: add app module configuration

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: `GlobalConstants.kt`

**Files:**
- Create: `app/src/main/java/ai/mifmax/constants/GlobalConstants.kt`

- [ ] **Step 1: Создать Kotlin `object` с исходными значениями**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/java/ai/mifmax/constants/GlobalConstants.kt`:

```kotlin
package ai.mifmax.constants

object GlobalConstants {
    const val FADE_DURATION = 1500L
    const val START_OFFSET = 1000L
    const val VIBRATE_TIME = "250"
    const val THRESHOLD = "2.75"
    const val SHAKE_COUNT = "4"
}
```

Примечание: `FADE_DURATION`/`START_OFFSET` сделаны `Long`, т.к. используются как `AlphaAnimation.duration`/`startOffset` (тип `Long`). Значения не изменены.

- [ ] **Step 2: Commit**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add app/src/main/java/ai/mifmax/constants/GlobalConstants.kt
git commit -m "feat: port GlobalConstants to Kotlin in ai.mifmax.constants

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Активности на Kotlin (`MainActivity`, `SettingsActivity`)

**Files:**
- Create: `app/src/main/java/ai/mifmax/balldefato/MainActivity.kt`
- Create: `app/src/main/java/ai/mifmax/balldefato/SettingsActivity.kt`

- [ ] **Step 1: `MainActivity.kt`**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/java/ai/mifmax/balldefato/MainActivity.kt`:

```kotlin
package ai.mifmax.balldefato

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import ai.mifmax.balldefato.databinding.ActivityMainBinding
import ai.mifmax.constants.GlobalConstants
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private lateinit var vibrator: Vibrator
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var shakeCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = obtainVibrator()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.shake -> {
                showMessage(getAnswer())
                true
            }
            R.id.preferences -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        showMessage(
            if (sensor != null) getString(R.string.shake_me_caption)
            else getString(R.string.menu_shake_caption),
        )
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    /** the magic code here */
    private fun getAnswer(): String {
        val responses = resources.getStringArray(R.array.responses)
        return responses[Random.nextInt(responses.size)]
    }

    private fun showMessage(message: String) {
        val view = binding.MessageTextView
        view.visibility = View.INVISIBLE
        view.text = message

        val animation = AlphaAnimation(0f, 1f).apply {
            startOffset = GlobalConstants.START_OFFSET
            duration = GlobalConstants.FADE_DURATION
        }
        view.visibility = View.VISIBLE
        view.startAnimation(animation)

        val millis = preferences
            .getString(getString(R.string.vibrate_time_id), GlobalConstants.VIBRATE_TIME)!!
            .toLong()
        vibrate(millis)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER &&
            isShakeEnough(event.values[0], event.values[1], event.values[2])
        ) {
            showMessage(getAnswer())
        }
    }

    private fun isShakeEnough(x: Float, y: Float, z: Float): Boolean {
        var force = 0.0
        force += ((x - lastX) / SensorManager.GRAVITY_EARTH).toDouble().pow(2.0)
        force += ((y - lastY) / SensorManager.GRAVITY_EARTH).toDouble().pow(2.0)
        force += ((z - lastZ) / SensorManager.GRAVITY_EARTH).toDouble().pow(2.0)
        force = sqrt(force)

        lastX = x
        lastY = y
        lastZ = z

        val threshold = preferences
            .getString(getString(R.string.threshold_id), GlobalConstants.THRESHOLD)!!
            .toFloat()
        if (force > threshold) {
            shakeCount++
            val maxShakes = preferences
                .getString(getString(R.string.shake_count_id), GlobalConstants.SHAKE_COUNT)!!
                .toInt()
            if (shakeCount > maxShakes) {
                shakeCount = 0
                lastX = 0f
                lastY = 0f
                lastZ = 0f
                return true
            }
        }
        return false
    }

    private fun obtainVibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private fun vibrate(milliseconds: Long) {
        if (milliseconds <= 0L) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }
}
```

Примечание про ViewBinding: id `@+id/MessageTextView` в layout не содержит `_`, поэтому сгенерированное свойство — `binding.MessageTextView` (регистр сохраняется).

- [ ] **Step 2: `SettingsActivity.kt`**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/java/ai/mifmax/balldefato/SettingsActivity.kt`:

```kotlin
package ai.mifmax.balldefato

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add app/src/main/java/ai/mifmax/balldefato/MainActivity.kt \
        app/src/main/java/ai/mifmax/balldefato/SettingsActivity.kt
git commit -m "feat: port activities to Kotlin (AppCompat, ViewBinding, PreferenceFragmentCompat)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Манифест и чистка ресурсов

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/xml/preferences.xml`

- [ ] **Step 1: `AndroidManifest.xml` модуля (без `package`/`uses-sdk`)**

Create `/Users/mverakhovskiy/work/ballFatum/app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".SettingsActivity"
            android:exported="false"
            android:label="@string/menu_preferences_caption" />
    </application>

</manifest>
```

- [ ] **Step 2: Тема Material3 в `styles.xml`**

Заменить весь `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values/styles.xml` на:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- Application theme. Material3 with a top app bar (ActionBar) for the options menu. -->
    <style name="AppTheme" parent="Theme.Material3.DayNight" />

</resources>
```

- [ ] **Step 3: Добавить строку описания картинки в `strings.xml`**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/values/strings.xml` добавить перед закрывающим `</resources>` строку:

```xml
    <string name="eight_ball_description">Магический шар</string>
```

- [ ] **Step 4: Почистить `activity_main.xml`**

Заменить весь `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/layout/activity_main.xml` на:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg"
    tools:context=".MainActivity">

    <ImageView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_gravity="center_vertical"
        android:layout_margin="10dp"
        android:contentDescription="@string/eight_ball_description"
        android:src="@drawable/eight_ball" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:orientation="vertical">

        <TextView
            android:id="@+id/MessageTextView"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:layout_marginTop="14dp"
            android:background="@drawable/triangle"
            android:focusable="false"
            android:gravity="center_vertical|center"
            android:text="@string/shake_me_caption" />
    </LinearLayout>

</FrameLayout>
```

- [ ] **Step 5: Убрать `f`-суффикс дефолта threshold в `preferences.xml`**

В `/Users/mverakhovskiy/work/ballFatum/app/src/main/res/xml/preferences.xml` заменить
`android:defaultValue="2.75f"` на `android:defaultValue="2.75"` (одно вхождение, у threshold-preference). Остальное не трогать.

- [ ] **Step 6: Commit**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add app/src/main/AndroidManifest.xml app/src/main/res
git commit -m "feat: add app manifest, Material3 theme, resource cleanup

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Первая сборка и исправление ошибок

**Files:**
- Create: `local.properties` (gitignored)

- [ ] **Step 1: `local.properties` с путём к SDK**

Create `/Users/mverakhovskiy/work/ballFatum/local.properties`:

```properties
sdk.dir=/Users/mverakhovskiy/Library/Android/sdk
```

- [ ] **Step 2: Собрать debug-APK на JDK 21**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=/Users/mverakhovskiy/Library/Java/JavaVirtualMachines/openjdk-21.0.2/Contents/Home \
  ./gradlew clean assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`; артефакт `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: При ошибках — диагностировать и чинить (superpowers:systematic-debugging)**

Типовые проблемы и действия:
- *AGP/Gradle version mismatch* («Minimum supported Gradle version is …» / «requires Android Gradle plugin …»): подобрать совместимую пару, поправив `agp` в `gradle/libs.versions.toml` и `distributionUrl` в `gradle/wrapper/gradle-wrapper.properties`. Совместимость — по таблице AGP↔Gradle.
- *compileSdk 36 not supported this AGP*: поднять `agp` (напр. до `8.10.x`) + соответствующий Gradle; safety-net `android.suppressUnsupportedCompileSdk=36` уже задан.
- *`Failed to find target with hash string 'android-36'`*: установить платформу —
  `~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-36"` (в этой среде уже установлена).
- *ViewBinding property не найдено* (`binding.MessageTextView`): проверить, что id в layout — `@+id/MessageTextView` и `viewBinding = true` включён.
- Повторять `assembleDebug`, пока не `BUILD SUCCESSFUL`.

- [ ] **Step 4: Прогнать lint**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=/Users/mverakhovskiy/Library/Java/JavaVirtualMachines/openjdk-21.0.2/Contents/Home \
  ./gradlew :app:lintDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL` (предупреждения допустимы; фатальных ошибок быть не должно). Отчёт: `app/build/reports/lint-results-debug.html`.

- [ ] **Step 5: Commit (если правились версии/конфиг)**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add -A
git commit -m "build: get assembleDebug green

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```
(Если ничего не менялось на шаге 3 — коммит пропустить.)

---

## Task 9: Удаление старого Java-исходника и финальная проверка

**Files:**
- Delete: `src/` (корневой), корневой `AndroidManifest.xml`

- [ ] **Step 1: Удалить legacy-исходники**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git rm -r src
git rm AndroidManifest.xml
```

- [ ] **Step 2: Убедиться, что ссылок на старьё не осталось**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
grep -rn "com.Maximv\|android-support-v4\|PreferenceActivity\|fill_parent" \
  --include=*.kt --include=*.xml --include=*.kts app/ ; echo "exit=$?"
```
Expected: совпадений нет (`exit=1` у grep = «ничего не найдено» — это то, что нужно).

- [ ] **Step 3: Пересобрать после удаления legacy**

Run:
```bash
cd /Users/mverakhovskiy/work/ballFatum
JAVA_HOME=/Users/mverakhovskiy/Library/Java/JavaVirtualMachines/openjdk-21.0.2/Contents/Home \
  ./gradlew clean assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
cd /Users/mverakhovskiy/work/ballFatum
git add -A
git commit -m "chore: remove legacy Java sources and root manifest

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 5: Итоговая проверка эквивалентности поведения**

Сверить с оригиналом (без изменений в логике):
- тряска → случайный ответ из 20 (порог/счётчик из настроек);
- пункт меню «Трясти» → ответ; «Настройки» → экран настроек;
- анимация появления текста + вибрация по `vibrateTime`;
- ключи настроек `shakeCount`/`threshold`/`vibrateTime` и дефолты совпадают.

Отметить любые расхождения; при необходимости — доработать и вернуться к Task 8/9.

---

## Self-Review (выполнено при написании плана)

- **Покрытие спеки:** структура проекта (T1–T4), wrapper (T3), namespace/applicationId/SDK/версии (T4), подпакеты `ai.mifmax.balldefato`+`ai.mifmax.constants` (T5–T6), модернизация MainActivity/Settings/вибрация/GlobalConstants (T5–T6), манифест/тема Material3/чистка ресурсов (T7), удаление support-v4 и `com.Maximv.*` (T1, T9), критерий сборки `assembleDebug` (T8–T9). Пробелов нет.
- **Плейсхолдеры:** отсутствуют — весь код и команды приведены целиком.
- **Согласованность типов:** `GlobalConstants.FADE_DURATION/START_OFFSET` — `Long`, используются как `duration`/`startOffset` в `MainActivity`. `vibrate(Long)` принимает результат `getString(...).toLong()`. Свойство ViewBinding `binding.MessageTextView` соответствует id `@+id/MessageTextView`. Ключи настроек читаются через те же строковые ресурсы, что и в `preferences.xml`.
