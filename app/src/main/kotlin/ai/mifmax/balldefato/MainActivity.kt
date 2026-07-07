package ai.mifmax.balldefato

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.os.Vibrator
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import ai.mifmax.balldefato.databinding.ActivityMainBinding
import ai.mifmax.balldefato.logic.AnswerPicker
import ai.mifmax.balldefato.logic.AnswerRepository
import ai.mifmax.balldefato.logic.ShakeDetector
import ai.mifmax.constants.GlobalConstants

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private lateinit var vibrator: Vibrator
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private val shakeDetector = ShakeDetector()
    private lateinit var answerPicker: AnswerPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = Vibrations.defaultVibrator(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val language = resources.configuration.locales[0].language
        answerPicker = AnswerPicker(AnswerRepository.load(this, language))

        // A different fortune-teller prompt each launch.
        binding.instruction.text =
            AnswerPicker(resources.getStringArray(R.array.instructions).toList()).pick()

        // Keep the title clear of the status bar (edge-to-edge on Android 15+).
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            (view.layoutParams as FrameLayout.LayoutParams).topMargin =
                top + (16 * resources.displayMetrics.density).toInt()
            view.requestLayout()
            insets
        }

        // Settings are hidden: reach the debug tunables with a long-press on the ball.
        binding.ball.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
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

    private fun showMessage(message: String, vibrate: Boolean = false) {
        val view = binding.MessageTextView
        view.text = message
        // Stay blank for START_OFFSET (fillBefore keeps alpha 0), then fade in slowly — a
        // prediction gradually surfacing.
        view.startAnimation(
            AlphaAnimation(0f, 1f).apply {
                startOffset = GlobalConstants.START_OFFSET
                duration = GlobalConstants.FADE_DURATION
            },
        )

        if (vibrate) {
            val millis = preferences
                .getString(getString(R.string.vibrate_time_id), GlobalConstants.VIBRATE_TIME)!!
                .toLong()
            Vibrations.oneShot(vibrator, millis)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val threshold = preferences
            .getString(getString(R.string.threshold_id), GlobalConstants.THRESHOLD)!!
            .toFloat()
        val limit = preferences
            .getString(getString(R.string.shake_count_id), GlobalConstants.SHAKE_COUNT)!!
            .toInt()
        val triggered = shakeDetector.onSample(
            event.values[0], event.values[1], event.values[2],
            threshold, limit, SystemClock.uptimeMillis(),
        )
        if (triggered) {
            showMessage(answerPicker.pick(), vibrate = true)
        }
    }
}
