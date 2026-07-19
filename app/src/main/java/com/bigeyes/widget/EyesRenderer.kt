package com.bigeyes.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min

/**
 * Draws the "Big Eyes" face onto any Canvas.
 *
 * A black rounded square with two big **solid white eyes** (no pupils). The
 * whole eyes shift around to "look" in a direction, and collapse into two short
 * white dashes when they blink — matching the Nothing Playground widget.
 *
 * All coordinates are computed from the given size so the same renderer works
 * for a tiny widget bitmap or a full-screen view.
 */
object EyesRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
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
            val half = size / 2f
            rect.set(cx - half, cy - half, cx + half, cy + half)
            canvas.drawRoundRect(rect, corner, corner, bgPaint)
        }

        val eyeRadius = size * 0.15f
        val eyeGap = size * 0.19f           // half-distance between the two eyes
        val eyeCenterY = cy + size * 0.05f

        // The whole eyes slide around to look; keep the travel gentle so they
        // never leave the black square.
        val moveX = size * 0.075f
        val moveY = size * 0.065f
        val offX = gazeX.coerceIn(-1f, 1f) * moveX
        val offY = gazeY.coerceIn(-1f, 1f) * moveY

        val leftX = cx - eyeGap + offX
        val rightX = cx + eyeGap + offX
        val ey = eyeCenterY + offY

        val openFactor = 1f - blink.coerceIn(0f, 1f)

        drawEye(canvas, leftX, ey, eyeRadius, openFactor)
        drawEye(canvas, rightX, ey, eyeRadius, openFactor)
    }

    private fun drawEye(
        canvas: Canvas,
        ex: Float,
        ey: Float,
        eyeRadius: Float,
        openFactor: Float,
    ) {
        if (openFactor <= 0.08f) {
            // Closed eye: a short white dash ("-"), like the widget's blink.
            val lineHalf = eyeRadius * 0.85f
            val lineThick = eyeRadius * 0.16f
            rect.set(ex - lineHalf, ey - lineThick, ex + lineHalf, ey + lineThick)
            canvas.drawRoundRect(rect, lineThick, lineThick, eyePaint)
            return
        }

        val save = canvas.save()
        // Squash vertically to fake the eyelid closing.
        canvas.scale(1f, openFactor, ex, ey)
        canvas.drawCircle(ex, ey, eyeRadius, eyePaint)
        canvas.restoreToCount(save)
    }
}
