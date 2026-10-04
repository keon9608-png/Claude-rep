package com.keon9608.biblewidget.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.keon9608.biblewidget.R
import com.keon9608.biblewidget.core.ResponsiveReadings
import com.keon9608.biblewidget.widget.BibleWidgetProvider
import com.keon9608.biblewidget.widget.Mode
import com.keon9608.biblewidget.widget.WidgetPrefs
import com.keon9608.biblewidget.widget.WidgetRenderer
import com.keon9608.biblewidget.widget.WidgetTheme

/** 앱 첫 화면: 사용법, 홈 화면에 위젯 추가, 놓인 위젯마다 설정 카드. */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawBehindSystemBars()
        val root = findViewById<View>(R.id.root)
        root.doOnSystemBarInsets { left, top, right, bottom -> root.setPadding(left, top, right, bottom) }

        val manager = AppWidgetManager.getInstance(this)
        val pinButton = findViewById<View>(R.id.pin_button)
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

        ids.forEachIndexed { i, id ->
            val config = WidgetPrefs.load(this, id)
            val what = when (config.mode) {
                Mode.DAILY -> getString(R.string.mode_daily)
                Mode.READ -> config.position.label()
                Mode.RESPONSIVE -> getString(R.string.reading_prefix, config.reading) + " " +
                    ResponsiveReadings.forNumber(config.reading).title
            }
            val themeName = getString(
                when (config.theme) {
                    WidgetTheme.SYSTEM -> R.string.theme_system
                    WidgetTheme.DARK -> R.string.theme_dark
                    WidgetTheme.LIGHT -> R.string.theme_light
                },
            )
            val row = layoutInflater.inflate(R.layout.item_widget_row, list, false)
            row.findViewById<TextView>(R.id.row_title).text = getString(R.string.widget_row_title, i + 1, what)
            row.findViewById<TextView>(R.id.row_detail).text =
                getString(R.string.widget_row_detail, themeName, config.transparency, config.textSizeSp)
            row.setOnClickListener {
                startActivity(
                    Intent(this, ConfigActivity::class.java)
                        .setAction(ConfigActivity.ACTION_EDIT)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                )
            }
            list.addView(row)
        }
    }
}
