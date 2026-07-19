package com.bigeyes.widget

import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

/**
 * Holds the "aliveness" state of the eyes — gaze easing and blinking — with no
 * dependency on View or Canvas, so it can be reused by both [BigEyesView]
 * (activity) and [BigEyesWallpaperService] (live wallpaper).
 *
 * Feed it a target with [setTouch] / [setTilt], call [step] once per frame,
 * then read [gazeX], [gazeY] and [blink] to render.
 */
class EyesAnimator {

    var gazeX = 0f; private set
    var gazeY = 0f; private set
    var blink = 0f; private set

    private var targetX = 0f
    private var targetY = 0f
    private var blinkVel = 0f
    private var nextBlinkAt = now() + blinkDelay()
    private var touching = false
    private var lastStep = 0L

    /** Finger position, each axis in [-1, 1]. Takes priority over tilt. */
    fun setTouch(nx: Float, ny: Float) {
        touching = true
        targetX = nx.coerceIn(-1f, 1f)
        targetY = ny.coerceIn(-1f, 1f)
    }

    fun clearTouch() {
        touching = false
    }

    /** Device tilt, each axis in [-1, 1]. Ignored while a finger is down. */
    fun setTilt(nx: Float, ny: Float) {
        if (touching) return
        val cx = nx.coerceIn(-1f, 1f)
        val cy = ny.coerceIn(-1f, 1f)
        if (abs(cx - targetX) > 0.02f) targetX = cx
        if (abs(cy - targetY) > 0.02f) targetY = cy
    }

    /**
     * Advance the animation. Uses real elapsed time (not a fixed frame count)
     * so the eyes move at the *same wall-clock speed* whether they are driven
     * at 60fps (the in-app view) or ~30fps (the home screen widget).
     */
    fun step() {
        val t = now()
        val dt = if (lastStep == 0L) 0.016f else ((t - lastStep).coerceIn(1L, 100L)) / 1000f
        lastStep = t

        // Exponential ease toward the target, framerate-independent.
        val f = 1f - exp(-GAZE_RATE * dt)
        gazeX += (targetX - gazeX) * f
        gazeY += (targetY - gazeY) * f

        if (blink == 0f && blinkVel == 0f && t >= nextBlinkAt) {
            blinkVel = BLINK_RATE
        }
        if (blinkVel != 0f || blink != 0f) {
            blink += blinkVel * dt
            if (blink >= 1f) {
                blink = 1f
                blinkVel = -BLINK_RATE
            } else if (blink <= 0f && blinkVel < 0f) {
                blink = 0f
                blinkVel = 0f
                nextBlinkAt = t + blinkDelay()
            }
        }
    }

    private fun blinkDelay(): Long = 2200L + Random.nextLong(0, 3200L)

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        // Gaze convergence rate (per second). ~12 matches the old 0.18/frame
        // feel at 60fps, but now holds at any framerate.
        const val GAZE_RATE = 12f
        // Blink open/close speed in units per second (0 = open, 1 = shut).
        // ~20 preserves the snappy in-app blink at any framerate.
        const val BLINK_RATE = 20f
    }
}
