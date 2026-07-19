package com.bigeyes.widget

import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder

/**
 * Live wallpaper version of Big Eyes: the whole home screen background is a
 * black field with two big eyes that look around and change expression on their
 * own; a touch briefly leads their gaze. The system only runs the animation
 * while the wallpaper is visible, so it is battery friendly.
 */
class BigEyesWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = BigEyesEngine()

    private inner class BigEyesEngine : Engine() {

        private val eyes = EyesAnimator()
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 0
        private var height = 0

        private val drawRunner = Runnable { drawFrame() }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, w: Int, h: Int) {
            width = w
            height = h
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (isVisible) handler.post(drawRunner) else handler.removeCallbacks(drawRunner)
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE ->
                    if (width > 0 && height > 0) {
                        eyes.setTouch(
                            (event.x / width) * 2f - 1f,
                            (event.y / height) * 2f - 1f,
                        )
                    }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> eyes.clearTouch()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            visible = false
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
                        face = eyes.face,
                        accent = Prefs.accent(this@BigEyesWallpaperService),
                        drawBackground = false,
                    )
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            handler.removeCallbacks(drawRunner)
            if (visible) handler.postDelayed(drawRunner, 16L)
        }
    }
}
