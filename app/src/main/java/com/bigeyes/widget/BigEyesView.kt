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

    private val eyes = EyesAnimator()

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val frame = object : Runnable {
        override fun run() {
            eyes.step()
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
            gazeX = eyes.gazeX,
            gazeY = eyes.gazeY,
            blink = eyes.blink,
            drawBackground = false,
        )
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> eyes.setTouch(
                (event.x / width) * 2f - 1f,
                (event.y / height) * 2f - 1f,
            )
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> eyes.clearTouch()
        }
        return true
    }

    override fun onSensorChanged(event: SensorEvent) {
        // x tilts left/right, y tilts up/down. Scale down for a gentle drift.
        eyes.setTilt(-event.values[0] / 6f, (event.values[1] - 3f) / 6f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
