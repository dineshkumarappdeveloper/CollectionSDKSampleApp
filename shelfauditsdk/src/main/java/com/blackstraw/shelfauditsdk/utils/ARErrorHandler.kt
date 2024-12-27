package com.blackstraw.shelfauditsdk.utils

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.LightEstimate
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

class ShakeDetector(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var shakeListener: (() -> Unit)? = null

    fun startListening(shakeListener: () -> Unit) {
        this.shakeListener = shakeListener
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        if (acceleration > 12) { // Threshold for shake detection
            shakeListener?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

fun isTilted(frame: Frame, maxTiltAngle: Float): Boolean {
    val cameraPose = frame.camera.pose
    val rotation = cameraPose.rotationQuaternion
    val pitch = asin(2.0 * (rotation[3] * rotation[1] - rotation[2] * rotation[0]))
    return abs(pitch) > Math.toRadians(maxTiltAngle.toDouble())
}

fun isTooFar(anchor: Anchor, maxDistance: Float): Boolean {
    val distance = sqrt(anchor.pose.tx() * anchor.pose.tx() + anchor.pose.ty() * anchor.pose.ty() + anchor.pose.tz() * anchor.pose.tz())
    return distance > maxDistance
}

fun isTooClose(anchor: Anchor, minDistance: Float): Boolean {
    val distance = sqrt(anchor.pose.tx() * anchor.pose.tx() + anchor.pose.ty() * anchor.pose.ty() + anchor.pose.tz() * anchor.pose.tz())
    return distance < minDistance
}

fun isLowLighting(frame: Frame): Boolean {
    val lightEstimate = frame.lightEstimate
    return lightEstimate.state == LightEstimate.State.VALID && lightEstimate.pixelIntensity < 2.0f
}


fun isLandscape(context: Context): Boolean {
    return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

fun isLandscape(frame: Frame): Boolean {
    val cameraPose = frame.camera.pose
    val rotation = cameraPose.rotationQuaternion

    // Calculate the roll angle
    val roll = Math.toDegrees(atan2(2.0 * (rotation[3] * rotation[2] + rotation[1] * rotation[0]), 1.0 - 2.0 * (rotation[1] * rotation[1] + rotation[2] * rotation[2])))

    // Check if the roll angle indicates landscape orientation
    return abs(roll) > 45 && abs(roll) < 135
}