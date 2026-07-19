package com.bigeyes.widget

import kotlin.math.exp
import kotlin.random.Random

/**
 * Drives the "aliveness" of the eyes with no dependency on View or Canvas, so
 * it can be reused by the widget, the full-screen view and the live wallpaper.
 *
 * The eyes are **autonomous**: they look around on their own and change
 * expression every so often (a quick blink, a double blink, a long content
 * squint, or a happy `^ ^`). Call [step] once per frame, then read [gazeX],
 * [gazeY], [blink] and [face] to render.
 *
 * Motion is time-based, so it runs at the same wall-clock speed at any frame
 * rate. A finger ([setTouch]) can temporarily take over where a touch surface
 * exists.
 */
class EyesAnimator {

    var gazeX = 0f; private set
    var gazeY = 0f; private set
    var blink = 0f; private set
    var face = Face.NEUTRAL; private set

    private var targetX = 0f
    private var targetY = 0f
    private var blinkTarget = 0f

    private var touching = false
    private var lastStep = 0L
    private var nextGazeAt = 0L

    private enum class Action { IDLE, BLINK, SQUINT, HAPPY, WINK, SIDE_EYE }
    private var action = Action.IDLE
    private var actionUntil = 0L
    private var blinksLeft = 0
    private var nextActionAt = now() + actionDelay()

    /** Finger position, each axis in [-1, 1]. Takes over the autonomous gaze. */
    fun setTouch(nx: Float, ny: Float) {
        touching = true
        targetX = nx.coerceIn(-1f, 1f)
        targetY = ny.coerceIn(-1f, 1f)
    }

    fun clearTouch() {
        touching = false
        nextGazeAt = now() + 500L
    }

    fun step() {
        val t = now()
        val dt = if (lastStep == 0L) 0.016f else ((t - lastStep).coerceIn(1L, 100L)) / 1000f
        lastStep = t

        updateGaze(t)
        updateAction(t)

        // Ease gaze and blink toward their targets (framerate-independent).
        val gf = 1f - exp(-GAZE_RATE * dt)
        gazeX += (targetX - gazeX) * gf
        gazeY += (targetY - gazeY) * gf

        val bf = 1f - exp(-BLINK_RATE * dt)
        blink += (blinkTarget - blink) * bf
    }

    private fun updateGaze(t: Long) {
        if (touching) return
        if (t >= nextGazeAt) {
            // Look somewhere new — sometimes back toward the centre.
            if (Random.nextFloat() < 0.35f) {
                targetX = 0f; targetY = 0f
            } else {
                targetX = Random.nextDouble(-1.0, 1.0).toFloat()
                targetY = Random.nextDouble(-0.85, 0.9).toFloat()
            }
            nextGazeAt = t + 700L + Random.nextLong(0, 1600L)
        }
    }

    private fun updateAction(t: Long) {
        when (action) {
            Action.IDLE -> {
                if (t >= nextActionAt) startRandomAction(t)
            }
            Action.BLINK -> {
                // Snap shut, then open; repeat for a double blink.
                if (blinkTarget == 1f && blink > 0.85f) blinkTarget = 0f
                if (blinkTarget == 0f && blink < 0.12f) {
                    blinksLeft--
                    if (blinksLeft > 0) blinkTarget = 1f else finishAction(t)
                }
            }
            Action.SQUINT -> {
                if (t >= actionUntil) {
                    blinkTarget = 0f
                    if (blink < 0.12f) finishAction(t)
                }
            }
            Action.HAPPY -> {
                if (t >= actionUntil) {
                    face = Face.NEUTRAL
                    finishAction(t)
                }
            }
            Action.WINK -> {
                if (t >= actionUntil) {
                    face = Face.NEUTRAL
                    finishAction(t)
                }
            }
            Action.SIDE_EYE -> {
                if (t >= actionUntil) {
                    blinkTarget = 0f
                    finishAction(t)
                }
            }
        }
    }

    private fun startRandomAction(t: Long) {
        when (Random.nextInt(100)) {
            in 0..31 -> {                 // single blink
                action = Action.BLINK; blinksLeft = 1; blinkTarget = 1f
            }
            in 32..46 -> {                // double blink
                action = Action.BLINK; blinksLeft = 2; blinkTarget = 1f
            }
            in 47..61 -> {                // long content squint ( - - )
                action = Action.SQUINT; blinkTarget = 0.96f
                actionUntil = t + 900L + Random.nextLong(0, 1200L)
            }
            in 62..75 -> {                // happy ( ^ ^ )
                action = Action.HAPPY; face = Face.HAPPY
                actionUntil = t + 900L + Random.nextLong(0, 900L)
            }
            in 76..87 -> {                // wink ( ◡ - )
                face = if (Random.nextBoolean()) Face.WINK_LEFT else Face.WINK_RIGHT
                action = Action.WINK
                blinkTarget = 0f
                actionUntil = t + 700L + Random.nextLong(0, 500L)
            }
            else -> {                     // side glance ( ` ` )
                val side = if (Random.nextBoolean()) 1f else -1f
                action = Action.SIDE_EYE
                blinkTarget = 0.4f                      // slightly narrowed
                targetX = side * 0.95f; targetY = 0.12f
                actionUntil = t + 800L + Random.nextLong(0, 900L)
                nextGazeAt = actionUntil                // hold the glance
            }
        }
    }

    private fun finishAction(t: Long) {
        action = Action.IDLE
        blinkTarget = 0f
        nextActionAt = t + actionDelay()
    }

    private fun actionDelay(): Long = 1500L + Random.nextLong(0, 2600L)

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        const val GAZE_RATE = 12f   // gaze convergence per second
        const val BLINK_RATE = 22f  // eyelid speed per second
    }
}
