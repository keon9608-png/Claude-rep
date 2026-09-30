package com.keon9608.biblewidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.keon9608.biblewidget.R
import com.keon9608.biblewidget.core.BibleRepository
import com.keon9608.biblewidget.core.ChapterRef
import com.keon9608.biblewidget.core.DailyVerses
import com.keon9608.biblewidget.core.Verse
import com.keon9608.biblewidget.core.VerseRef
import java.time.LocalDate
import kotlin.concurrent.thread

/** 위젯의 스크롤 목록에 들어갈 줄(절)을 만들어 준다. */
class VerseListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        VerseListFactory(
            applicationContext,
            intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
        )
}

private sealed interface Row {
    data class VerseRow(val verse: Verse, val chapter: ChapterRef?) : Row
    data class DailyRow(val ref: VerseRef, val text: String) : Row
    data class FooterRow(val chapter: ChapterRef) : Row
    object ErrorRow : Row
}

private class VerseListFactory(
    private val context: Context,
    private val widgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {

    @Volatile private var rows: List<Row> = emptyList()
    @Volatile private var loaded = false
    @Volatile private var config = WidgetConfig()

    private val repository = BibleRepository(context.filesDir)

    override fun onCreate() = Unit

    // onDataSetChanged/getCount/getViewAt은 바인더 스레드에서 불리므로 네트워크를 써도 된다.
    override fun onDataSetChanged() = load()

    override fun getCount(): Int {
        if (!loaded) load()
        return rows.size
    }

    @Synchronized
    private fun load() {
        val config = WidgetPrefs.load(context, widgetId)
        this.config = config
        rows = when (config.mode) {
            Mode.DAILY -> {
                val today = LocalDate.now().toEpochDay()
                val ref = DailyVerses.forDay(today)
                val verse = repository.chapter(ref.chapterRef)?.firstOrNull { it.number == ref.verse }
                prefetch(DailyVerses.forDay(today + 1).chapterRef)
                if (verse == null) listOf(Row.ErrorRow) else listOf(Row.DailyRow(ref, verse.text))
            }
            Mode.READ -> {
                val chapter = config.position
                val verses = repository.chapter(chapter)
                prefetch(chapter.next())
                val body = verses?.mapIndexed { i, v -> Row.VerseRow(v, chapter.takeIf { i == 0 }) }
                    ?: listOf(Row.ErrorRow)
                body + Row.FooterRow(chapter)
            }
        }
        loaded = true
    }

    /** 다음에 읽을 장을 미리 받아 둔다(잠금화면에서 오프라인일 때 대비). */
    private fun prefetch(chapter: ChapterRef) {
        if (repository.cached(chapter) != null) return
        thread(isDaemon = true, name = "bible-prefetch") { repository.chapter(chapter) }
    }

    override fun getViewAt(position: Int): RemoteViews? {
        val row = rows.getOrNull(position) ?: return blankRow()
        return when (row) {
            is Row.VerseRow -> textRow(verseText(row))
            is Row.DailyRow -> textRow(dailyText(row))
            is Row.ErrorRow -> textRow(context.getString(R.string.load_failed)).apply {
                setOnClickFillInIntent(R.id.text, navIntent(WidgetRenderer.NAV_RETRY))
            }
            is Row.FooterRow -> footerRow(row.chapter)
        }
    }

    private fun textRow(text: CharSequence): RemoteViews {
        val layout = if (config.font == Font.SERIF) R.layout.item_verse_serif else R.layout.item_verse
        return RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.text, text)
            setTextViewTextSize(R.id.text, TypedValue.COMPLEX_UNIT_SP, config.textSizeSp.toFloat())
            WidgetRenderer.textColor(config.theme)?.let { setTextColor(R.id.text, it) }
        }
    }

    /** "1  태초에 …". 장의 첫 절은 번호 대신 "창세기 1:1"처럼 위치를 붙인다. */
    private fun verseText(row: Row.VerseRow): CharSequence {
        val out = SpannableStringBuilder()
        if (config.showNumbers) {
            val label = row.chapter?.let { "${it.book.name} ${it.chapter}:${row.verse.number}" }
                ?: row.verse.number.toString()
            out.appendDim(label).append("  ")
        }
        return out.appendBody(row.verse.text)
    }

    /** "…채우시리라  빌립보서 4:19". 출처는 맨 끝에 작게 붙여 윗줄을 차지하지 않게 한다. */
    private fun dailyText(row: Row.DailyRow): CharSequence {
        val out = SpannableStringBuilder().appendBody(row.text)
        if (config.showNumbers) out.append("  ").appendDim(row.ref.label())
        return out
    }

    /** 본문. "글자 굵게"를 켜면 본문만 굵게 한다(절 번호·출처는 그대로). */
    private fun SpannableStringBuilder.appendBody(text: String): SpannableStringBuilder {
        val start = length
        append(text)
        if (config.bold) setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }

    private fun SpannableStringBuilder.appendDim(text: String): SpannableStringBuilder {
        val start = length
        append(text)
        setSpan(ForegroundColorSpan(WidgetRenderer.DIM_COLOR), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(RelativeSizeSpan(0.72f), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return this
    }

    private fun footerRow(chapter: ChapterRef): RemoteViews =
        RemoteViews(context.packageName, R.layout.item_footer).apply {
            val size = config.textSizeSp * 0.8f
            setTextViewText(R.id.label, chapter.label())
            for (id in intArrayOf(R.id.prev, R.id.label, R.id.next)) {
                setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, size)
            }
            setOnClickFillInIntent(R.id.prev, navIntent(WidgetRenderer.NAV_PREV))
            setOnClickFillInIntent(R.id.label, navIntent(WidgetRenderer.NAV_SETTINGS))
            setOnClickFillInIntent(R.id.next, navIntent(WidgetRenderer.NAV_NEXT))
        }

    private fun navIntent(nav: Int) = Intent().putExtra(WidgetRenderer.EXTRA_NAV, nav)

    private fun blankRow() = RemoteViews(context.packageName, R.layout.item_loading)

    override fun getLoadingView(): RemoteViews = blankRow()

    override fun getViewTypeCount(): Int = 3

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun onDestroy() = Unit
}
