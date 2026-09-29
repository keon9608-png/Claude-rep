package com.keon9608.biblewidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.keon9608.biblewidget.R
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** 위젯의 겉(둥근 사각형 + 스크롤 목록)을 그린다. 목록 안의 글은 [VerseListService]가 채운다. */
object WidgetRenderer {
    const val ACTION_ITEM = "com.keon9608.biblewidget.action.ITEM"
    const val ACTION_RETRY = "com.keon9608.biblewidget.action.RETRY"
    const val ACTION_MIDNIGHT = "com.keon9608.biblewidget.action.MIDNIGHT"

    const val EXTRA_NAV = "com.keon9608.biblewidget.extra.NAV"
    const val NAV_PREV = 1
    const val NAV_NEXT = 2
    const val NAV_SETTINGS = 3
    const val NAV_RETRY = 4

    /** 절 번호·출처 색. 검은 배경과 흰 배경 모두에서 읽히는 회색. */
    const val DIM_COLOR = 0xFF8C8C8C.toInt()

    fun widgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, BibleWidgetProvider::class.java))

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        for (id in widgetIds(context)) update(context, manager, id)
    }

    fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val config = WidgetPrefs.load(context, widgetId)
        val views = RemoteViews(context.packageName, R.layout.widget)

        views.setInt(android.R.id.background, "setBackgroundResource", backgroundRes(config.theme))

        val (horizontal, vertical) = padding(context, manager, widgetId, config)
        views.setViewPadding(R.id.list, horizontal, vertical, horizontal, vertical)

        // 설정/위치/날짜가 바뀌면 URI가 바뀌고, 그러면 런처가 목록을 새로 만들어 맨 위부터 보여준다.
        val adapter = Intent(context, VerseListService::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .setData(adapterUri(widgetId, config))
        @Suppress("DEPRECATION")
        views.setRemoteAdapter(R.id.list, adapter)
        views.setEmptyView(R.id.list, R.id.empty)

        val itemTemplate = Intent(context, BibleWidgetProvider::class.java)
            .setAction(ACTION_ITEM)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        views.setPendingIntentTemplate(
            R.id.list,
            PendingIntent.getBroadcast(
                context, widgetId, itemTemplate,
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag(),
            ),
        )

        val retry = Intent(context, BibleWidgetProvider::class.java)
            .setAction(ACTION_RETRY)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        views.setOnClickPendingIntent(
            R.id.empty,
            PendingIntent.getBroadcast(
                context, widgetId, retry,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        manager.updateAppWidget(widgetId, views)
    }

    private fun adapterUri(widgetId: Int, c: WidgetConfig): Uri {
        val builder = Uri.Builder()
            .scheme("biblewidget")
            .authority("widget")
            .appendPath(widgetId.toString())
            .appendQueryParameter("mode", c.mode.name)
            .appendQueryParameter("theme", c.theme.name)
            .appendQueryParameter("font", c.font.name)
            .appendQueryParameter("size", c.textSizeSp.toString())
            .appendQueryParameter("num", if (c.showNumbers) "1" else "0")
            .appendQueryParameter("rev", c.revision.toString())
        when (c.mode) {
            Mode.DAILY -> builder.appendQueryParameter("day", LocalDate.now().toEpochDay().toString())
            Mode.READ -> builder
                .appendQueryParameter("book", c.position.bookIndex.toString())
                .appendQueryParameter("chap", c.position.chapter.toString())
        }
        return builder.build()
    }

    fun backgroundRes(theme: WidgetTheme): Int = when (theme) {
        WidgetTheme.SYSTEM -> R.drawable.bg_system
        WidgetTheme.DARK -> R.drawable.bg_dark
        WidgetTheme.LIGHT -> R.drawable.bg_light
    }

    /** 시스템 테마일 때는 null: 레이아웃의 낮/밤 색 리소스를 그대로 쓴다. */
    fun textColor(theme: WidgetTheme): Int? = when (theme) {
        WidgetTheme.SYSTEM -> null
        WidgetTheme.DARK -> Color.WHITE
        WidgetTheme.LIGHT -> Color.BLACK
    }

    /**
     * 목록 안쪽 여백(px). 높이가 한 줄짜리(예: 4×1)면 위아래 여백을 늘려 한 줄이 가운데 오게 한다.
     */
    private fun padding(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        config: WidgetConfig,
    ): Pair<Int, Int> {
        val res = context.resources
        val density = res.displayMetrics.density
        val options = manager.getAppWidgetOptions(widgetId)
        // 세로 화면 기준 높이는 MAX_HEIGHT.
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            .takeIf { it > 0 }
            ?: options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val lineDp = config.textSizeSp * res.configuration.fontScale * LINE_HEIGHT_FACTOR
        val horizontalDp = 16f
        val verticalDp = when {
            heightDp <= 0 -> 14f
            heightDp < lineDp * 2 + 2 * 14f + VERSE_GAP_DP -> ((heightDp - lineDp) / 2f).coerceAtLeast(4f)
            else -> 14f
        }
        return (horizontalDp * density).roundToInt() to (verticalDp * density).roundToInt()
    }

    /** 한 줄 높이 ≈ 글자 크기 × 이 값 (줄 간격 포함). item_verse.xml의 줄 간격과 맞춘다. */
    private const val LINE_HEIGHT_FACTOR = 1.7f
    private const val VERSE_GAP_DP = 6f

    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    /** 자정이 지나면 오늘의 말씀을 바꾼다. 정확할 필요는 없어서 부정확 알람을 쓴다. */
    fun scheduleMidnight(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val nextMidnight = LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() + 5_000
        alarm.set(AlarmManager.RTC, nextMidnight, midnightIntent(context))
    }

    fun cancelMidnight(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(midnightIntent(context))
    }

    private fun midnightIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, BibleWidgetProvider::class.java).setAction(ACTION_MIDNIGHT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
