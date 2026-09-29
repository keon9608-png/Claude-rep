package com.keon9608.biblewidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.keon9608.biblewidget.core.ChapterRef
import com.keon9608.biblewidget.ui.ConfigActivity

class BibleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        for (id in widgetIds) WidgetRenderer.update(context, manager, id)
        WidgetRenderer.scheduleMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        newOptions: Bundle,
    ) {
        // 크기가 바뀌면 여백을 다시 계산한다.
        WidgetRenderer.update(context, manager, widgetId)
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        for (id in widgetIds) WidgetPrefs.delete(context, id)
    }

    override fun onEnabled(context: Context) {
        WidgetRenderer.scheduleMidnight(context)
    }

    override fun onDisabled(context: Context) {
        WidgetRenderer.cancelMidnight(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetRenderer.ACTION_ITEM -> onItemClick(context, intent)
            WidgetRenderer.ACTION_RETRY -> retry(context, widgetId(intent))
            WidgetRenderer.ACTION_MIDNIGHT,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                WidgetRenderer.updateAll(context)
                WidgetRenderer.scheduleMidnight(context)
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun onItemClick(context: Context, intent: Intent) {
        val id = widgetId(intent)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        when (intent.getIntExtra(WidgetRenderer.EXTRA_NAV, 0)) {
            WidgetRenderer.NAV_PREV -> move(context, id) { it.prev() }
            WidgetRenderer.NAV_NEXT -> move(context, id) { it.next() }
            WidgetRenderer.NAV_RETRY -> retry(context, id)
            WidgetRenderer.NAV_SETTINGS -> context.startActivity(
                Intent(context, ConfigActivity::class.java)
                    .setAction(ConfigActivity.ACTION_EDIT)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        }
    }

    private fun move(context: Context, id: Int, step: (ChapterRef) -> ChapterRef) {
        val config = WidgetPrefs.load(context, id)
        WidgetPrefs.save(context, id, config.copy(position = step(config.position)))
        WidgetRenderer.update(context, AppWidgetManager.getInstance(context), id)
    }

    private fun retry(context: Context, id: Int) {
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val config = WidgetPrefs.load(context, id)
        WidgetPrefs.save(context, id, config.copy(revision = config.revision + 1))
        WidgetRenderer.update(context, AppWidgetManager.getInstance(context), id)
    }

    private fun widgetId(intent: Intent): Int =
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
}
