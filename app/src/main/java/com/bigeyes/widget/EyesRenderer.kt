package com.bigeyes.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.min

/** The expression the eyes are currently wearing. */
enum class Face { NEUTRAL, HAPPY }

/**
 * Draws the "Big Eyes" face onto any Canvas.
 *
 * A black rounded square with two big **solid eyes** (no pupils). The whole
 * eyes shift around to "look", narrow into two dashes (blink / a long content
 * squint), or curve up into happy `^ ^` arcs.
 *
 * All coordinates are computed from the given size so the same renderer works
 * for a tiny widget bitmap or a full-screen view.
 */
object EyesRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()
    private val path = Path()

    /**
     * @param gazeX  look direction on X, range [-1, 1]
     * @param gazeY  look direction on Y, range [-1, 1]
     * @param blink  0 = fully open, 1 = fully closed (used when [face] is NEUTRAL)
     * @param face   the current expression
     * @param accent eye color (white by default; orange for the Claude theme)
     * @param drawBackground draw the black rounded square (false = transparent)
     */
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        gazeX: Float = 0f,
        gazeY: Float = 0f,
        blink: Float = 0f,
        face: Face = Face.NEUTRAL,
        accent: Int = Color.WHITE,
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

        eyePaint.color = accent
        strokePaint.color = accent

        val eyeRadius = size * 0.15f
        val eyeGap = size * 0.19f
        val eyeCenterY = cy + size * 0.05f

        val moveX = size * 0.075f
        val moveY = size * 0.065f
        val offX = gazeX.coerceIn(-1f, 1f) * moveX
        val offY = gazeY.coerceIn(-1f, 1f) * moveY

        val leftX = cx - eyeGap + offX
        val rightX = cx + eyeGap + offX
        val ey = eyeCenterY + offY

        when (face) {
            Face.HAPPY -> {
                strokePaint.strokeWidth = eyeRadius * 0.44f
                drawHappyEye(canvas, leftX, ey, eyeRadius)
                drawHappyEye(canvas, rightX, ey, eyeRadius)
            }
            Face.NEUTRAL -> {
                val openFactor = 1f - blink.coerceIn(0f, 1f)
                drawEye(canvas, leftX, ey, eyeRadius, openFactor)
                drawEye(canvas, rightX, ey, eyeRadius, openFactor)
            }
        }
    }

    private fun drawEye(canvas: Canvas, ex: Float, ey: Float, eyeRadius: Float, openFactor: Float) {
        if (openFactor <= 0.12f) {
            // Closed/squinting eye: a short dash ("-"), like a blink or a
            // content, held gaze.
            val lineHalf = eyeRadius * 0.85f
            val lineThick = eyeRadius * 0.16f
            rect.set(ex - lineHalf, ey - lineThick, ex + lineHalf, ey + lineThick)
            canvas.drawRoundRect(rect, lineThick, lineThick, eyePaint)
            return
        }
        val save = canvas.save()
        canvas.scale(1f, openFactor, ex, ey)
        canvas.drawCircle(ex, ey, eyeRadius, eyePaint)
        canvas.restoreToCount(save)
    }

    private fun drawHappyEye(canvas: Canvas, ex: Float, ey: Float, eyeRadius: Float) {
        // An upward arch ( ^ ) — a smiling, laughing eye.
        val w = eyeRadius * 0.95f
        val h = eyeRadius * 0.78f
        path.reset()
        path.moveTo(ex - w, ey + h * 0.45f)
        path.quadTo(ex, ey - h, ex + w, ey + h * 0.45f)
        canvas.drawPath(path, strokePaint)
    }
}
