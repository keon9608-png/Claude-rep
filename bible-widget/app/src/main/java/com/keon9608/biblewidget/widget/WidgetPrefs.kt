package com.keon9608.biblewidget.widget

import android.content.Context
import com.keon9608.biblewidget.core.Bible
import com.keon9608.biblewidget.core.ChapterRef

enum class Mode { DAILY, READ }

enum class WidgetTheme { SYSTEM, DARK, LIGHT }

enum class Font { SANS, SERIF }

/** 위젯 하나의 설정. 위젯마다 따로 저장된다. */
data class WidgetConfig(
    val mode: Mode = Mode.DAILY,
    val theme: WidgetTheme = WidgetTheme.SYSTEM,
    val font: Font = Font.SANS,
    val textSizeSp: Int = DEFAULT_TEXT_SIZE,
    val bold: Boolean = false,
    /** 배경 투명도(%). 0이면 불투명, 100이면 배경 없이 글자만. */
    val transparency: Int = 0,
    val showNumbers: Boolean = true,
    val position: ChapterRef = ChapterRef(0, 1),
    /** 다시 시도할 때 올려서 목록을 새로 불러오게 한다. */
    val revision: Int = 0,
) {
    /** 배경 그림에 쓸 알파값(0–255). */
    val backgroundAlpha: Int get() = Math.round(255 * (100 - transparency) / 100f)

    companion object {
        const val DEFAULT_TEXT_SIZE = 18
        const val MIN_TEXT_SIZE = 12
        const val MAX_TEXT_SIZE = 28
        /** 글자 크기 슬라이더 한 칸 = 2sp. 12…28이면 9칸. */
        const val TEXT_SIZE_STEP = 2
        const val TRANSPARENCY_STEP = 10
    }
}

object WidgetPrefs {
    private const val FILE = "widgets"

    fun load(context: Context, widgetId: Int): WidgetConfig {
        val p = prefs(context)
        val k = "w$widgetId."
        val defaults = WidgetConfig()
        val bookIndex = p.getInt(k + "book", 0).coerceIn(Bible.books.indices)
        val chapter = p.getInt(k + "chapter", 1).coerceIn(1, Bible.books[bookIndex].chapters)
        return WidgetConfig(
            mode = enumOr(p.getString(k + "mode", null), defaults.mode),
            theme = enumOr(p.getString(k + "theme", null), defaults.theme),
            font = enumOr(p.getString(k + "font", null), defaults.font),
            textSizeSp = p.getInt(k + "size", defaults.textSizeSp)
                .coerceIn(WidgetConfig.MIN_TEXT_SIZE, WidgetConfig.MAX_TEXT_SIZE),
            bold = p.getBoolean(k + "bold", defaults.bold),
            transparency = p.getInt(k + "transparency", defaults.transparency).coerceIn(0, 100),
            showNumbers = p.getBoolean(k + "numbers", defaults.showNumbers),
            position = ChapterRef(bookIndex, chapter),
            revision = p.getInt(k + "rev", 0),
        )
    }

    fun save(context: Context, widgetId: Int, config: WidgetConfig) {
        val k = "w$widgetId."
        prefs(context).edit()
            .putString(k + "mode", config.mode.name)
            .putString(k + "theme", config.theme.name)
            .putString(k + "font", config.font.name)
            .putInt(k + "size", config.textSizeSp)
            .putBoolean(k + "bold", config.bold)
            .putInt(k + "transparency", config.transparency)
            .putBoolean(k + "numbers", config.showNumbers)
            .putInt(k + "book", config.position.bookIndex)
            .putInt(k + "chapter", config.position.chapter)
            .putInt(k + "rev", config.revision)
            .apply()
    }

    fun exists(context: Context, widgetId: Int): Boolean = prefs(context).contains("w$widgetId.mode")

    fun delete(context: Context, widgetId: Int) {
        val k = "w$widgetId."
        val p = prefs(context)
        val editor = p.edit()
        p.all.keys.filter { it.startsWith(k) }.forEach(editor::remove)
        editor.apply()
    }

    /** 마지막으로 저장한 설정. 새 위젯의 기본값으로 쓴다. */
    fun loadLastUsed(context: Context): WidgetConfig = load(context, LAST_USED_ID)

    fun saveLastUsed(context: Context, config: WidgetConfig) = save(context, LAST_USED_ID, config)

    private const val LAST_USED_ID = -1

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback
}
