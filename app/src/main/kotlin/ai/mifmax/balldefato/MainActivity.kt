package ai.mifmax.balldefato

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import ai.mifmax.balldefato.databinding.ActivityMainBinding
import ai.mifmax.balldefato.logic.AnswerPicker
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
        answerPicker = AnswerPicker(resources.getStringArray(R.array.responses).toList())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.shake -> {
                showMessage(answerPicker.pick())
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
        Vibrations.oneShot(vibrator, millis)
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
        if (shakeDetector.onSample(event.values[0], event.values[1], event.values[2], threshold, limit)) {
            showMessage(answerPicker.pick())
        }
    }
}
