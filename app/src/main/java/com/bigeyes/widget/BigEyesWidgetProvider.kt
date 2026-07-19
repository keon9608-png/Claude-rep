package com.bigeyes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews

/**
 * Home screen "Big Eyes" widget.
 *
 * The provider draws a first frame right away so the widget looks right the
 * moment it is placed, then hands the live animation over to
 * [BigEyesWidgetService], which keeps the eyes moving and blinking on the home
 * screen. Tapping the widget opens the full-screen Big Eyes.
 */
class BigEyesWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        BigEyesWidgetService.start(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            drawStaticFrame(context, appWidgetManager, id)
        }
        // Make sure the animator is running (e.g. after a reboot / re-add).
        BigEyesWidgetService.start(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?,
    ) {
        drawStaticFrame(context, appWidgetManager, appWidgetId)
    }

    override fun onDisabled(context: Context) {
        // Last widget removed — stop animating.
        BigEyesWidgetService.stop(context)
    }

    /** A single open-eyed frame, shown until the service pushes live frames. */
    private fun drawStaticFrame(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val px = 320
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        EyesRenderer.draw(
            Canvas(bitmap), px, px,
            gazeX = 0f, gazeY = 0f, blink = 0f,
            face = Face.NEUTRAL, accent = Prefs.accent(context),
            drawBackground = true,
        )

        val views = RemoteViews(context.packageName, R.layout.widget_big_eyes)
        views.setImageViewBitmap(R.id.widget_image, bitmap)

        val intent = Intent(context, BigEyesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context, appWidgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_image, pending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
