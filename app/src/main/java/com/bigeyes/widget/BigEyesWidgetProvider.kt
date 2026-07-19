package com.bigeyes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews
import kotlin.random.Random

/**
 * Home screen "Big Eyes" widget.
 *
 * A home screen widget cannot run a smooth 60fps animation the way an activity
 * can (the launcher only lets us push occasional bitmap updates), so the widget
 * shows the eyes with a gentle random glance that refreshes on each system
 * update. Tap it to open the fully animated, finger-following Big Eyes.
 */
class BigEyesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            renderWidget(context, appWidgetManager, id)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?,
    ) {
        // Re-render when the widget is resized so the eyes stay crisp.
        renderWidget(context, appWidgetManager, appWidgetId)
    }

    private fun renderWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        // Render at a fixed, high-enough resolution; the launcher scales it.
        val px = 512
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // A subtle random glance so the eyes don't look frozen dead-center.
        val gazeX = Random.nextDouble(-0.5, 0.5).toFloat()
        val gazeY = Random.nextDouble(-0.35, 0.45).toFloat()

        EyesRenderer.draw(
            canvas = canvas,
            width = px,
            height = px,
            gazeX = gazeX,
            gazeY = gazeY,
            blink = 0f,
            drawBackground = true,
        )

        val views = RemoteViews(context.packageName, R.layout.widget_big_eyes)
        views.setImageViewBitmap(R.id.widget_image, bitmap)

        // Tap opens the fully animated Big Eyes.
        val intent = Intent(context, BigEyesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_image, pending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
