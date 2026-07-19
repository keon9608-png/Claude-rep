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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Keeps the home screen Big Eyes widget alive: a lightweight foreground service
 * that re-renders the widget a few times a second so the eyes follow the
 * phone's tilt and blink on their own — right on the home screen, no need to
 * open anything.
 *
 * Battery friendly: it only animates while the screen is on, and only pushes a
 * new frame when the picture actually changed (so a perfectly still phone
 * barely does any work).
 */
class BigEyesWidgetService : Service(), SensorEventListener {

    private val eyes = EyesAnimator()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var screenOn = true

    // Reused across frames to avoid allocating a bitmap every tick.
    private val bmpSize = 320
    private val bitmap: Bitmap = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)

    private var lastGazeX = Float.NaN
    private var lastGazeY = Float.NaN
    private var lastBlink = Float.NaN

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    registerSensor()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    screenOn = false
                    sensorManager.unregisterListener(this@BigEyesWidgetService)
                }
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

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Going foreground can be rejected if we were started from the
        // background (Android 12+). Fail quietly instead of crashing; the app
        // will start us again from the foreground next time it is opened.
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
        registerSensor()
        handler.post(frame)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If there are no widgets left, there's nothing to animate.
        if (widgetIds().isEmpty()) {
            stopSelf()
        }
        // Don't let the system silently restart us in the background (where we
        // can't go foreground). The app re-starts us from the foreground.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(frame)
        runCatching { sensorManager.unregisterListener(this) }
        runCatching { unregisterReceiver(screenReceiver) }
        bitmap.recycle()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerSensor() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun tick() {
        if (!screenOn) return
        val ids = widgetIds()
        if (ids.isEmpty()) {
            stopSelf()
            return
        }

        eyes.step()

        // Skip pushing an identical frame — saves battery while the phone is still.
        val gx = eyes.gazeX
        val gy = eyes.gazeY
        val bl = eyes.blink
        val changed = lastGazeX.isNaN() ||
            abs(gx - lastGazeX) > 0.004f ||
            abs(gy - lastGazeY) > 0.004f ||
            abs(bl - lastBlink) > 0.004f
        if (!changed) return
        lastGazeX = gx; lastGazeY = gy; lastBlink = bl

        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        EyesRenderer.draw(canvas, bmpSize, bmpSize, gx, gy, bl, drawBackground = true)

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

    override fun onSensorChanged(event: SensorEvent) {
        eyes.setTilt(-event.values[0] / 6f, (event.values[1] - 3f) / 6f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun buildNotification(): Notification {
        val channelId = "big_eyes"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "Big Eyes",
                        NotificationManager.IMPORTANCE_MIN,
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
        private const val FRAME_MS = 66L // ~15 fps

        /** Best-effort start; safe to call repeatedly. */
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
            runCatching {
                context.stopService(Intent(context, BigEyesWidgetService::class.java))
            }
        }
    }
}
