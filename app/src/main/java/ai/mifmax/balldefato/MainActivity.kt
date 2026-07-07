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
