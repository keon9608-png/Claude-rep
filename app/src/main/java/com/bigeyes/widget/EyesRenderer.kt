package com.bigeyes.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min

/** The expression the eyes are currently wearing. */
enum class Face { NEUTRAL, HAPPY, WINK_LEFT, WINK_RIGHT }

/**
 * Draws the "Big Eyes" face onto any Canvas.
 *
 * A rounded square with two big **solid eyes** (no pupils). The whole eyes
 * shift around to "look", narrow into two dashes (blink / a content squint /
 * a side glance), pop wide open (surprise), or curve up into happy `^ ^` arcs.
 *
 * Colors are supplied by the caller so the same renderer draws both the classic
 * black-on-white and the Claude orange theme.
 */
object EyesRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private val rect = RectF()

    /**
     * @param gazeX    look direction on X, range [-1, 1]
     * @param gazeY    look direction on Y, range [-1, 1]
     * @param blink    0 = fully open, 1 = fully closed (used when [face] is NEUTRAL)
     * @param face     the current expression
     * @param eyeColor color of the eyes
     * @param bgColor  color of the rounded square (drawn only if [drawBackground])
     * @param drawBackground draw the rounded square (false = transparent)
     */
    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        gazeX: Float = 0f,
        gazeY: Float = 0f,
        blink: Float = 0f,
        face: Face = Face.NEUTRAL,
        eyeColor: Int = Color.WHITE,
        bgColor: Int = Color.BLACK,
        drawBackground: Boolean = true,
    ) {
        val size = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f

        if (drawBackground) {
            bgPaint.color = bgColor
            val corner = size * 0.24f
            val half = size / 2f
            rect.set(cx - half, cy - half, cx + half, cy + half)
            canvas.drawRoundRect(rect, corner, corner, bgPaint)
        }

        eyePaint.color = eyeColor

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
                drawHappyEye(canvas, leftX, ey, eyeRadius)
                drawHappyEye(canvas, rightX, ey, eyeRadius)
            }
            Face.WINK_LEFT -> {
                // Left eye winks (a dash); right eye smiles (a half-disc).
                drawDashEye(canvas, leftX, ey, eyeRadius)
                drawHappyEye(canvas, rightX, ey, eyeRadius)
            }
            Face.WINK_RIGHT -> {
                drawHappyEye(canvas, leftX, ey, eyeRadius)
                drawDashEye(canvas, rightX, ey, eyeRadius)
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
            drawDashEye(canvas, ex, ey, eyeRadius)
            return
        }
        val save = canvas.save()
        canvas.scale(1f, openFactor, ex, ey)
        canvas.drawCircle(ex, ey, eyeRadius, eyePaint)
        canvas.restoreToCount(save)
    }

    private fun drawDashEye(canvas: Canvas, ex: Float, ey: Float, eyeRadius: Float) {
        // A closed/winking eye: a short dash ("-").
        val lineHalf = eyeRadius * 0.85f
        val lineThick = eyeRadius * 0.16f
        rect.set(ex - lineHalf, ey - lineThick, ex + lineHalf, ey + lineThick)
        canvas.drawRoundRect(rect, lineThick, lineThick, eyePaint)
    }

    private fun drawHappyEye(canvas: Canvas, ex: Float, ey: Float, eyeRadius: Float) {
        // A filled half-disc, flat side down and dome up — a happy, smiling eye.
        val r = eyeRadius * 1.08f
        val baseY = ey + eyeRadius * 0.18f
        rect.set(ex - r, baseY - r, ex + r, baseY + r)
        canvas.drawArc(rect, 180f, 180f, true, eyePaint)
    }
}
