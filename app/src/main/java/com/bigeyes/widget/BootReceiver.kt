package com.bigeyes.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Restart the widget animation after a reboot, if any widget is placed. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, BigEyesWidgetProvider::class.java)
        )
        if (ids.isNotEmpty()) {
            BigEyesWidgetService.start(context)
        }
    }
}
