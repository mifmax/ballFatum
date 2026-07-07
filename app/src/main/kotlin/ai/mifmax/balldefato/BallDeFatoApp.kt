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
