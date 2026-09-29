package com.keon9608.biblewidget.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.keon9608.biblewidget.R
import com.keon9608.biblewidget.widget.BibleWidgetProvider
import com.keon9608.biblewidget.widget.Mode
import com.keon9608.biblewidget.widget.WidgetPrefs
import com.keon9608.biblewidget.widget.WidgetRenderer
import com.keon9608.biblewidget.widget.WidgetTheme

/** 앱 첫 화면: 위젯 추가 방법, 홈 화면에 바로 추가, 놓인 위젯들의 설정 열기. */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val manager = AppWidgetManager.getInstance(this)
        val pinButton = findViewById<Button>(R.id.pin_button)
        if (manager.isRequestPinAppWidgetSupported) {
            pinButton.setOnClickListener {
                manager.requestPinAppWidget(ComponentName(this, BibleWidgetProvider::class.java), null, null)
            }
        } else {
            pinButton.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        showWidgets()
    }

    private fun showWidgets() {
        val list = findViewById<LinearLayout>(R.id.widget_list)
        list.removeAllViews()
        val ids = WidgetRenderer.widgetIds(this)
        findViewById<View>(R.id.widget_list_empty).visibility = if (ids.isEmpty()) View.VISIBLE else View.GONE

        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        val pad = (16 * resources.displayMetrics.density).toInt()

        ids.forEachIndexed { i, id ->
            val config = WidgetPrefs.load(this, id)
            val what = when (config.mode) {
                Mode.DAILY -> getString(R.string.mode_daily)
                Mode.READ -> config.position.label()
            }
            val themeName = getString(
                when (config.theme) {
                    WidgetTheme.SYSTEM -> R.string.theme_system
                    WidgetTheme.DARK -> R.string.theme_dark
                    WidgetTheme.LIGHT -> R.string.theme_light
                },
            )
            list.addView(
                TextView(this).apply {
                    text = getString(R.string.widget_row, i + 1, what, themeName)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setPadding(0, pad, 0, pad)
                    setBackgroundResource(outValue.resourceId)
                    setOnClickListener {
                        startActivity(
                            Intent(this@MainActivity, ConfigActivity::class.java)
                                .setAction(ConfigActivity.ACTION_EDIT)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                        )
                    }
                },
            )
        }
    }
}
