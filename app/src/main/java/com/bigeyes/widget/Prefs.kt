package com.bigeyes.widget

import android.content.Context

/**
 * Remembers the chosen eye color across the widget, the view and the wallpaper.
 * All three run in the same process, so the in-memory [cached] value updates
 * instantly when the color is changed in the app.
 */
object Prefs {
    private const val FILE = "big_eyes"
    private const val KEY_ACCENT = "accent"

    const val WHITE = 0xFFFFFFFF.toInt()
    const val ORANGE = 0xFFD97757.toInt() // Claude's signature warm orange

    private val palette = intArrayOf(WHITE, ORANGE)

    @Volatile
    private var cached: Int? = null

    fun accent(context: Context): Int {
        cached?.let { return it }
        val v = prefs(context).getInt(KEY_ACCENT, WHITE)
        cached = v
        return v
    }

    /** Advance to the next color and persist it. Returns the new color. */
    fun cycleAccent(context: Context): Int {
        val current = accent(context)
        val next = palette[(palette.indexOf(current).coerceAtLeast(0) + 1) % palette.size]
        prefs(context).edit().putInt(KEY_ACCENT, next).apply()
        cached = next
        return next
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
