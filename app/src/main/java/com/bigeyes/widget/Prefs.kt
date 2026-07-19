package com.bigeyes.widget

import android.content.Context
import android.graphics.Color

/**
 * Remembers the chosen color theme across the widget, the view and the
 * wallpaper. All three run in the same process, so the in-memory [cached] value
 * updates instantly when the theme is changed in the app.
 */
object Prefs {
    private const val FILE = "big_eyes"
    private const val KEY_THEME = "theme"

    /** A theme is a background color plus an eye color. */
    data class Theme(val bg: Int, val eye: Int)

    private const val ORANGE = 0xFFD97757.toInt() // Claude's signature warm orange

    // Classic: black square, white eyes. Claude: orange square, white eyes.
    val themes = listOf(
        Theme(bg = Color.BLACK, eye = Color.WHITE),
        Theme(bg = ORANGE, eye = Color.WHITE),
    )

    @Volatile
    private var cachedIndex: Int? = null

    fun theme(context: Context): Theme = themes[index(context)]

    /** Advance to the next theme and persist it. Returns the new theme. */
    fun cycleTheme(context: Context): Theme {
        val next = (index(context) + 1) % themes.size
        prefs(context).edit().putInt(KEY_THEME, next).apply()
        cachedIndex = next
        return themes[next]
    }

    private fun index(context: Context): Int {
        cachedIndex?.let { return it }
        val v = prefs(context).getInt(KEY_THEME, 0).coerceIn(0, themes.size - 1)
        cachedIndex = v
        return v
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
