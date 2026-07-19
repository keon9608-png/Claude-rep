package com.bigeyes.widget

import android.graphics.Canvas
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder

/**
 * Live wallpaper version of Big Eyes: the whole home screen background is a
 * black field with two big eyes that follow your finger, drift with the phone's
 * tilt, and blink. The system only runs the animation while the wallpaper is
 * visible, so it is battery friendly.
 *
 * Set it via: Settings > Wallpaper > Live wallpapers > Big Eyes (or long-press
 * the home screen > Wallpapers).
 */
class BigEyesWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = BigEyesEngine()

    private inner class BigEyesEngine : Engine(), SensorEventListener {

        private val eyes = EyesAnimator()
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 0
        private var height = 0

        private val sensorManager =
            getSystemService(SENSOR_SERVICE) as SensorManager
        private val accelerometer: Sensor? =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        private val drawRunner = Runnable { drawFrame() }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            w: Int,
            h: Int,
        ) {
            width = w
            height = h
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) {
                accelerometer?.let {
                    sensorManager.registerListener(
                        this, it, SensorManager.SENSOR_DELAY_GAME
                    )
                }
                handler.post(drawRunner)
            } else {
                sensorManager.unregisterListener(this)
                handler.removeCallbacks(drawRunner)
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    if (width > 0 && height > 0) {
                        eyes.setTouch(
                            (event.x / width) * 2f - 1f,
                            (event.y / height) * 2f - 1f,
                        )
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> eyes.clearTouch()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            visible = false
            sensorManager.unregisterListener(this)
            handler.removeCallbacks(drawRunner)
        }

        private fun drawFrame() {
            if (!visible) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    eyes.step()
                    canvas.drawColor(Color.BLACK)
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
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postDelayed(drawRunner, 16L)
        }

        override fun onSensorChanged(event: SensorEvent) {
            eyes.setTilt(-event.values[0] / 6f, (event.values[1] - 3f) / 6f)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
}
