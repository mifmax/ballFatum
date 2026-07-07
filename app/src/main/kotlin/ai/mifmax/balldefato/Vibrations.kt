package ai.mifmax.balldefato

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Version-correct vibration for the deliberate "here is your answer" buzz.
 *
 * The buzz is tagged as PHYSICAL_EMULATION (a game/toy rumble). On devices where the user has
 * turned off touch/alarm/notification vibration, that channel is typically still enabled, so the
 * buzz plays instead of being dropped as `ignored_for_settings`.
 */
object Vibrations {

    private val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun defaultVibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    fun oneShot(vibrator: Vibrator, millis: Long) {
        if (millis <= 0L) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            null
        }
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val attributes = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_PHYSICAL_EMULATION)
                    .build()
                vibrator.vibrate(effect!!, attributes)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect!!, alarmAudioAttributes)
            }
            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(millis)
            }
        }
    }
}
