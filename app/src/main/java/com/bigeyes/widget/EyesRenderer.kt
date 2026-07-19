package com.bigeyes.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min

/**
 * Draws the "Big Eyes" face onto any Canvas.
 *
 * The look is intentionally minimal (a nod to the Nothing Playground widget):
 * a black rounded square with two big white eyes. Dark pupils track a gaze
 * target so the eyes appear to follow your finger / the phone's tilt.
 *
 * All coordinates are computed from the given [size] so the same renderer works
 * for a tiny widget bitmap or a full-screen view.
 */
object EyesRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    private val scleraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A0A0A")
    }

    private val rect = RectF()

    /**
     * @param canvas   target canvas
     * @param width    canvas width in px
     * @param height   canvas height in px
     * @param gazeX    look direction on X, range [-1, 1] (-1 = left, 1 = right)
     * @param gazeY    look direction on Y, range [-1, 1] (-1 = up, 1 = down)
     * @param blink    0 = fully open, 1 = fully closed
     * @param drawBackground draw the black rounded square (false = transparent)
     */
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        gazeX: Float = 0f,
        gazeY: Float = 0f,
        blink: Float = 0f,
        drawBackground: Boolean = true,
    ) {
        val size = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f

        if (drawBackground) {
            val corner = size * 0.24f
            // Fill the whole canvas with the rounded square, centered.
            val half = size / 2f
            rect.set(cx - half, cy - half, cx + half, cy + half)
            canvas.drawRoundRect(rect, corner, corner, bgPaint)
        }

        // Eye geometry.
        val eyeRadius = size * 0.155f
        val eyeGap = size * 0.20f          // half-distance between the two eyes
        val eyeCenterY = cy + size * 0.06f // sit slightly below middle, like the icon
        val leftX = cx - eyeGap
        val rightX = cx + eyeGap

        // Pupil can travel this far from the eye center before hitting the rim.
        val pupilRadius = eyeRadius * 0.42f
        val travel = eyeRadius - pupilRadius - eyeRadius * 0.06f
        val gx = gazeX.coerceIn(-1f, 1f)
        val gy = gazeY.coerceIn(-1f, 1f)
        val pupilDx = gx * travel
        val pupilDy = gy * travel

        val openFactor = (1f - blink.coerceIn(0f, 1f))

        drawEye(canvas, leftX, eyeCenterY, eyeRadius, pupilRadius, pupilDx, pupilDy, openFactor)
        drawEye(canvas, rightX, eyeCenterY, eyeRadius, pupilRadius, pupilDx, pupilDy, openFactor)
    }

    private fun drawEye(
        canvas: Canvas,
        ex: Float,
        ey: Float,
        eyeRadius: Float,
        pupilRadius: Float,
        pupilDx: Float,
        pupilDy: Float,
        openFactor: Float,
    ) {
        if (openFactor <= 0.02f) {
            // Closed eye: a short white line (a "-" style blink).
            val lineHalf = eyeRadius * 0.7f
            val lineThick = eyeRadius * 0.16f
            rect.set(ex - lineHalf, ey - lineThick, ex + lineHalf, ey + lineThick)
            canvas.drawRoundRect(rect, lineThick, lineThick, scleraPaint)
            return
        }

        val save = canvas.save()
        // Squash vertically to fake the eyelid closing.
        canvas.scale(1f, openFactor, ex, ey)

        canvas.drawCircle(ex, ey, eyeRadius, scleraPaint)
        canvas.drawCircle(ex + pupilDx, ey + pupilDy, pupilRadius, pupilPaint)

        canvas.restoreToCount(save)
    }
}
