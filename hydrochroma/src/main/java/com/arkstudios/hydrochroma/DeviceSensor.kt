package com.arkstudios.hydrochroma

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sin

/**
 * 🎛️ rememberTilt
 * Hooks into the device Accelerometer to detect tilt.
 * * @param restingAngleDegrees The angle at which the phone is considered "flat" (default 45°).
 */
@Composable
fun rememberTilt(restingAngleDegrees: Float = 45f): Offset {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // X axis: Tilt Left/Right (Normal logic)
                    val xRaw = it.values[0]
                    val x = -(xRaw / 5f).coerceIn(-1f, 1f)

                    // Y axis: Tilt Up/Down
                    // 9.81 = Vertical (90°), 0 = Flat (0°)
                    // We want to offset so 45° (approx 7.0m/s²) is 0.
                    val yRaw = it.values[1]

                    // Convert degrees to expected gravity component roughly
                    // sin(45) * 9.81 ≈ 6.9
                    val restingGravity = sin(Math.toRadians(restingAngleDegrees.toDouble())).toFloat() * 9.81f

                    val y = ((yRaw - restingGravity) / 5f).coerceIn(-1f, 1f)

                    tilt = Offset(x, y)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return tilt
}