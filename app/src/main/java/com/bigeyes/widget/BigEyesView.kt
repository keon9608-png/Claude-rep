package com.bigeyes.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Full-screen, 60fps Big Eyes. The eyes look around and change expression on
 * their own; touching the screen lets you lead their gaze for a moment.
 */
class BigEyesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val eyes = EyesAnimator()

    private val frame = object : Runnable {
        override fun run() {
            eyes.step()
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frame)
    }

    override fun onDraw(canvas: Canvas) {
        // Transparent background — the activity supplies the black backdrop.
        EyesRenderer.draw(
            canvas = canvas,
            width = width,
            height = height,
            gazeX = eyes.gazeX,
            gazeY = eyes.gazeY,
            blink = eyes.blink,
            face = eyes.face,
            accent = Prefs.accent(context),
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
}
