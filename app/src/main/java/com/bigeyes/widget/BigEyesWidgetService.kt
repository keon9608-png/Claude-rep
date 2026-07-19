package com.bigeyes.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Keeps the home screen Big Eyes widget alive: a lightweight foreground service
 * that re-renders the widget ~30 times a second so the eyes look around and
 * change expression on their own — right on the home screen, no need to open
 * anything.
 *
 * Battery friendly: it only animates while the screen is on, and only pushes a
 * new frame when the picture actually changed.
 */
class BigEyesWidgetService : Service() {

    private val eyes = EyesAnimator()
    private val handler = Handler(Looper.getMainLooper())

    private var screenOn = true

    // Reused across frames to avoid allocating a bitmap every tick. Kept modest
    // so each RemoteViews update stays small over IPC.
    private val bmpSize = 256
    private val bitmap: Bitmap = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)

    private var lastGazeX = Float.NaN
    private var lastGazeY = Float.NaN
    private var lastBlink = Float.NaN
    private var lastFace: Face? = null
    private var lastAccent = 0

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> screenOn = true
                Intent.ACTION_SCREEN_OFF -> screenOn = false
            }
        }
    }

    private val frame = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, FRAME_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground(NOTIF_ID, buildNotification())
        } catch (t: Throwable) {
            stopSelf()
            return
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        handler.post(frame)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (widgetIds().isEmpty()) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(frame)
        runCatching { unregisterReceiver(screenReceiver) }
        bitmap.recycle()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun tick() {
        if (!screenOn) return
        val ids = widgetIds()
        if (ids.isEmpty()) {
            stopSelf()
            return
        }

        eyes.step()

        val gx = eyes.gazeX
        val gy = eyes.gazeY
        val bl = eyes.blink
        val fc = eyes.face
        val accent = Prefs.accent(this)

        // Skip pushing an identical frame — saves battery while nothing changes.
        val changed = lastGazeX.isNaN() ||
            fc != lastFace ||
            accent != lastAccent ||
            abs(gx - lastGazeX) > 0.004f ||
            abs(gy - lastGazeY) > 0.004f ||
            abs(bl - lastBlink) > 0.004f
        if (!changed) return
        lastGazeX = gx; lastGazeY = gy; lastBlink = bl; lastFace = fc; lastAccent = accent

        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        EyesRenderer.draw(canvas, bmpSize, bmpSize, gx, gy, bl, fc, accent, drawBackground = true)

        val views = RemoteViews(packageName, R.layout.widget_big_eyes)
        views.setImageViewBitmap(R.id.widget_image, bitmap)
        views.setOnClickPendingIntent(R.id.widget_image, tapIntent())

        AppWidgetManager.getInstance(this).updateAppWidget(ids, views)
    }

    private fun widgetIds(): IntArray =
        AppWidgetManager.getInstance(this).getAppWidgetIds(
            ComponentName(this, BigEyesWidgetProvider::class.java)
        )

    private fun tapIntent(): PendingIntent {
        val intent = Intent(this, BigEyesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildNotification(): Notification {
        val channelId = "big_eyes"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        channelId, "Big Eyes", NotificationManager.IMPORTANCE_MIN,
                    ).apply { setShowBadge(false) }
                )
            }
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Big Eyes")
            .setContentText("위젯이 홈 화면에서 움직이는 중")
            .setSmallIcon(R.drawable.ic_stat_eyes)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 42
        private const val FRAME_MS = 33L // ~30 fps (widget IPC ceiling)

        fun start(context: Context) {
            val intent = Intent(context, BigEyesWidgetService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, BigEyesWidgetService::class.java)) }
        }
    }
}
