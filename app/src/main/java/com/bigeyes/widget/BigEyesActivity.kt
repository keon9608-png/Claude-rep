package com.bigeyes.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen Big Eyes. Opening the app also (reliably, from the foreground)
 * starts [BigEyesWidgetService] so the home screen widget begins animating.
 * A small button switches the eye color (white / Claude orange) everywhere.
 */
class BigEyesActivity : AppCompatActivity() {

    private lateinit var eyesView: BigEyesView
    private lateinit var root: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this).apply { setBackgroundColor(Prefs.theme(this@BigEyesActivity).bg) }
        this.root = root

        eyesView = BigEyesView(this)
        root.addView(
            eyesView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val colorButton = TextView(this).apply {
            text = getString(R.string.change_color)
            setTextColor(Color.WHITE)
            alpha = 0.6f
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            val pad = dp(14)
            setPadding(pad, dp(10), pad, dp(10))
            setOnClickListener { cycleColor() }
        }
        root.addView(
            colorButton,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(28)
            },
        )

        setContentView(root)

        // Kick the widget animator to life while we're in the foreground.
        BigEyesWidgetService.start(this)
    }

    private fun cycleColor() {
        val theme = Prefs.cycleTheme(this)
        root.setBackgroundColor(theme.bg)
        eyesView.invalidate()
        refreshWidgets()
    }

    /** Ask every placed widget to redraw with the new color right away. */
    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, BigEyesWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            val intent = Intent(this, BigEyesWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
