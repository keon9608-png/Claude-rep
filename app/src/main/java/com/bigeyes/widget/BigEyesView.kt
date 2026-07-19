package com.bigeyes.widget

import android.content.Context
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

/**
 * Full-screen, 60fps Big Eyes.
 *
 * The pupils smoothly follow your finger while you touch the screen, and drift
 * with the phone's tilt (accelerometer) otherwise. The eyes blink on their own
 * every few seconds so the face feels alive.
 */
class BigEyesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs), SensorEventListener {

    // Current gaze (rendered) and target gaze (where the eyes want to look).
    private var gazeX = 0f
    private var gazeY = 0f
    private var targetX = 0f
    private var targetY = 0f

    // Blink state.
    private var blink = 0f
    private var blinkVel = 0f
    private var nextBlinkAt = now() + blinkDelay()

    private var touching = false

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val frame = object : Runnable {
        override fun run() {
            step()
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sensorManager.unregisterListener(this)
        removeCallbacks(frame)
    }

    override fun onDraw(canvas: Canvas) {
        // Transparent background here — the activity supplies the black backdrop
        // so the eyes can fill the whole screen.
        EyesRenderer.draw(
            canvas = canvas,
            width = width,
            height = height,
            gazeX = gazeX,
            gazeY = gazeY,
            blink = blink,
            drawBackground = false,
        )
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                touching = true
                targetX = ((event.x / width) * 2f - 1f).coerceIn(-1f, 1f)
                targetY = ((event.y / height) * 2f - 1f).coerceIn(-1f, 1f)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> touching = false
        }
        return true
    }

    private fun step() {
        // Ease the rendered gaze toward the target for a smooth follow.
        gazeX += (targetX - gazeX) * 0.18f
        gazeY += (targetY - gazeY) * 0.18f

        // Blink animation: a quick close-then-open spring.
        val t = now()
        if (blink == 0f && blinkVel == 0f && t >= nextBlinkAt) {
            blinkVel = 0.35f
        }
        if (blinkVel != 0f || blink != 0f) {
            blink += blinkVel
            if (blink >= 1f) {
                blink = 1f
                blinkVel = -0.35f
            } else if (blink <= 0f && blinkVel < 0f) {
                blink = 0f
                blinkVel = 0f
                nextBlinkAt = t + blinkDelay()
            }
        }
    }

    // --- Accelerometer (used only when not touching) ---

    override fun onSensorChanged(event: SensorEvent) {
        if (touching) return
        // x tilts left/right, y tilts up/down. Scale down for a gentle drift.
        val ax = event.values[0]
        val ay = event.values[1]
        val nx = (-ax / 6f).coerceIn(-1f, 1f)
        val ny = ((ay - 3f) / 6f).coerceIn(-1f, 1f)
        // Ignore tiny jitter so a still phone keeps still eyes.
        if (abs(nx - targetX) > 0.02f) targetX = nx
        if (abs(ny - targetY) > 0.02f) targetY = ny
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun blinkDelay(): Long = 2200L + Random.nextLong(0, 3200L)

    private fun now(): Long = System.currentTimeMillis()
}
