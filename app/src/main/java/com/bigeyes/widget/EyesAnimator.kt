package com.bigeyes.widget

import kotlin.math.abs
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

    /** Advance one frame. */
    fun step() {
        gazeX += (targetX - gazeX) * 0.18f
        gazeY += (targetY - gazeY) * 0.18f

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

    private fun blinkDelay(): Long = 2200L + Random.nextLong(0, 3200L)

    private fun now(): Long = System.currentTimeMillis()
}
