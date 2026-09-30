package com.keon9608.biblewidget.ui

import android.app.Activity
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.keon9608.biblewidget.R
import com.keon9608.biblewidget.core.Bible
import com.keon9608.biblewidget.core.BibleRepository
import com.keon9608.biblewidget.core.ChapterRef
import com.keon9608.biblewidget.widget.Font
import com.keon9608.biblewidget.widget.Mode
import com.keon9608.biblewidget.widget.WidgetConfig
import com.keon9608.biblewidget.widget.WidgetPrefs
import com.keon9608.biblewidget.widget.WidgetRenderer
import com.keon9608.biblewidget.widget.WidgetTheme

/**
 * 위젯 설정 화면. 위젯을 처음 놓을 때, 위젯 길게 누르기 → 설정(Android 12+), 위젯 아래쪽의 "창세기 1장"을 누를 때,
 * 앱 첫 화면의 "내 위젯 설정"에서 열린다.
 *
 * 위쪽 미리보기는 실제 배경화면 위에 그려서 투명도를 바로 확인할 수 있다.
 */
class ConfigActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var config: WidgetConfig

    private lateinit var modeGroup: RadioGroup
    private lateinit var themeGroup: RadioGroup
    private lateinit var fontGroup: RadioGroup
    private lateinit var bookSpinner: Spinner
    private lateinit var chapterSpinner: Spinner
    private lateinit var positionSection: View
    private lateinit var transparencyBar: SeekBar
    private lateinit var transparencyLabel: TextView
    private lateinit var sizeBar: SeekBar
    private lateinit var sizeLabel: TextView
    private lateinit var boldSwitch: CompoundButton
    private lateinit var numbersSwitch: CompoundButton
    private lateinit var previewBg: ImageView
    private lateinit var preview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        // 사용자가 취소하거나 뒤로 가기로 나가면 위젯을 추가하지 않는다.
        setResult(RESULT_CANCELED, resultIntent())
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContentView(R.layout.activity_config)
        drawBehindSystemBars(darkStatusIcons = wallpaperWantsDarkText())
        applyInsets()

        val isNewWidget = !WidgetPrefs.exists(this, widgetId)
        config = if (isNewWidget) {
            WidgetPrefs.loadLastUsed(this).copy(revision = 0)
        } else {
            WidgetPrefs.load(this, widgetId)
        }
        findViewById<TextView>(R.id.title).setText(
            if (isNewWidget) R.string.config_title_new else R.string.config_title_edit,
        )

        bindViews()
        applyConfigToViews()
        updatePreview()
    }

    /** 미리보기는 상태 표시줄 아래에서 시작하고, 취소/저장 줄은 내비게이션 바 위에 둔다. */
    private fun applyInsets() {
        val root = findViewById<View>(R.id.root)
        val previewArea = findViewById<View>(R.id.preview_area)
        val bottomBar = findViewById<View>(R.id.bottom_bar)
        root.doOnSystemBarInsets { left, top, right, bottom ->
            root.setPadding(left, 0, right, 0)
            previewArea.setPadding(previewArea.paddingLeft, top + dp(24), previewArea.paddingRight, previewArea.paddingBottom)
            bottomBar.setPadding(bottomBar.paddingLeft, bottomBar.paddingTop, bottomBar.paddingRight, bottom + dp(4))
        }
    }

    /** 배경화면이 밝으면 상태 표시줄 아이콘을 어둡게 (Android 12+에서만 알 수 있음). */
    private fun wallpaperWantsDarkText(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val colors = try {
            WallpaperManager.getInstance(this).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        } catch (e: RuntimeException) {
            null
        } ?: return false
        return colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
    }

    private fun bindViews() {
        modeGroup = findViewById(R.id.mode_group)
        themeGroup = findViewById(R.id.theme_group)
        fontGroup = findViewById(R.id.font_group)
        bookSpinner = findViewById(R.id.book_spinner)
        chapterSpinner = findViewById(R.id.chapter_spinner)
        positionSection = findViewById(R.id.position_section)
        transparencyBar = findViewById(R.id.transparency_bar)
        transparencyLabel = findViewById(R.id.transparency_label)
        sizeBar = findViewById(R.id.size_bar)
        sizeLabel = findViewById(R.id.size_label)
        boldSwitch = findViewById(R.id.bold_switch)
        numbersSwitch = findViewById(R.id.numbers_switch)
        previewBg = findViewById(R.id.preview_bg)
        preview = findViewById(R.id.preview_text)

        bookSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            Bible.books.map { it.name },
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        modeGroup.setOnCheckedChangeListener { _, id ->
            config = config.copy(mode = if (id == R.id.mode_read) Mode.READ else Mode.DAILY)
            positionSection.visibility = if (config.mode == Mode.READ) View.VISIBLE else View.GONE
            updatePreview()
        }
        themeGroup.setOnCheckedChangeListener { _, id ->
            config = config.copy(
                theme = when (id) {
                    R.id.theme_dark -> WidgetTheme.DARK
                    R.id.theme_light -> WidgetTheme.LIGHT
                    else -> WidgetTheme.SYSTEM
                },
            )
            updatePreview()
        }
        fontGroup.setOnCheckedChangeListener { _, id ->
            config = config.copy(font = if (id == R.id.font_serif) Font.SERIF else Font.SANS)
            updatePreview()
        }
        bookSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != config.position.bookIndex) {
                    config = config.copy(position = ChapterRef(position, 1))
                }
                setChapterChoices(config.position)
                updatePreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        chapterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chapter = position + 1
                if (chapter > config.position.book.chapters || chapter == config.position.chapter) return
                config = config.copy(position = ChapterRef(config.position.bookIndex, chapter))
                updatePreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        // 투명도: 0%, 10%, … 100% (11칸)
        transparencyBar.max = 100 / WidgetConfig.TRANSPARENCY_STEP
        transparencyBar.setOnSeekBarChangeListener(onStep { step ->
            config = config.copy(transparency = step * WidgetConfig.TRANSPARENCY_STEP)
            updatePreview()
        })
        // 글자 크기: 12, 14, … 28 (9칸)
        sizeBar.max = (WidgetConfig.MAX_TEXT_SIZE - WidgetConfig.MIN_TEXT_SIZE) / WidgetConfig.TEXT_SIZE_STEP
        sizeBar.setOnSeekBarChangeListener(onStep { step ->
            config = config.copy(textSizeSp = WidgetConfig.MIN_TEXT_SIZE + step * WidgetConfig.TEXT_SIZE_STEP)
            updatePreview()
        })

        boldSwitch.setOnCheckedChangeListener { _, checked ->
            config = config.copy(bold = checked)
            updatePreview()
        }
        numbersSwitch.setOnCheckedChangeListener { _, checked ->
            config = config.copy(showNumbers = checked)
            updatePreview()
        }
        findViewById<View>(R.id.cancel_button).setOnClickListener { finish() }
        findViewById<View>(R.id.save_button).setOnClickListener { save() }
    }

    private fun onStep(block: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) = block(progress)
        override fun onStartTrackingTouch(bar: SeekBar?) = Unit
        override fun onStopTrackingTouch(bar: SeekBar?) = Unit
    }

    private fun applyConfigToViews() {
        modeGroup.check(if (config.mode == Mode.READ) R.id.mode_read else R.id.mode_daily)
        positionSection.visibility = if (config.mode == Mode.READ) View.VISIBLE else View.GONE
        themeGroup.check(
            when (config.theme) {
                WidgetTheme.SYSTEM -> R.id.theme_system
                WidgetTheme.DARK -> R.id.theme_dark
                WidgetTheme.LIGHT -> R.id.theme_light
            },
        )
        fontGroup.check(if (config.font == Font.SERIF) R.id.font_serif else R.id.font_sans)
        bookSpinner.setSelection(config.position.bookIndex, false)
        setChapterChoices(config.position)
        // 이전 버전의 홀수 크기(예: 17)는 가장 가까운 칸으로 맞춘다.
        val sizeStep = (config.textSizeSp - WidgetConfig.MIN_TEXT_SIZE + 1) / WidgetConfig.TEXT_SIZE_STEP
        sizeBar.progress = sizeStep
        config = config.copy(textSizeSp = WidgetConfig.MIN_TEXT_SIZE + sizeStep * WidgetConfig.TEXT_SIZE_STEP)
        transparencyBar.progress = config.transparency / WidgetConfig.TRANSPARENCY_STEP
        boldSwitch.isChecked = config.bold
        numbersSwitch.isChecked = config.showNumbers
    }

    private fun setChapterChoices(position: ChapterRef) {
        val book = position.book
        val labels = (1..book.chapters).map { "$it${book.chapterUnit}" }
        val current = chapterSpinner.adapter
        if (current == null || current.count != labels.size || current.getItem(0) != labels[0]) {
            chapterSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
                .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        }
        chapterSpinner.setSelection(position.chapter - 1, false)
    }

    private fun updatePreview() {
        val dark = when (config.theme) {
            WidgetTheme.DARK -> true
            WidgetTheme.LIGHT -> false
            WidgetTheme.SYSTEM -> isNightMode()
        }
        previewBg.setImageResource(if (dark) R.drawable.bg_dark else R.drawable.bg_light)
        previewBg.imageAlpha = config.backgroundAlpha
        preview.setTextColor(if (dark) Color.WHITE else Color.BLACK)
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.textSizeSp.toFloat())
        preview.typeface = if (config.font == Font.SERIF) Typeface.SERIF else Typeface.SANS_SERIF
        preview.text = previewText()
        transparencyLabel.text = getString(R.string.transparency_value, config.transparency)
        sizeLabel.text = getString(R.string.text_size_value, config.textSizeSp)
    }

    /** 이미 받아 둔 본문이 있으면 그걸로, 없으면 창세기 1:1(개역한글과 같은 문장)로 미리 보여준다. */
    private fun previewText(): CharSequence {
        val chapter = if (config.mode == Mode.READ) config.position else ChapterRef(0, 1)
        val verses = BibleRepository(filesDir).cached(chapter)?.take(3)
        val sample = verses?.map { it.number to it.text } ?: listOf(1 to getString(R.string.preview_sample))
        val out = SpannableStringBuilder()
        sample.forEachIndexed { i, (number, text) ->
            if (i > 0) out.append("\n")
            if (config.showNumbers) {
                val start = out.length
                out.append(if (i == 0) "${chapter.book.name} ${chapter.chapter}:$number" else number.toString())
                out.setSpan(ForegroundColorSpan(WidgetRenderer.DIM_COLOR), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.setSpan(RelativeSizeSpan(0.72f), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                out.append("  ")
            }
            val start = out.length
            out.append(text)
            if (config.bold) out.setSpan(StyleSpan(Typeface.BOLD), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return out
    }

    private fun save() {
        WidgetPrefs.save(this, widgetId, config)
        WidgetPrefs.saveLastUsed(this, config)
        WidgetRenderer.update(this, AppWidgetManager.getInstance(this), widgetId)
        WidgetRenderer.scheduleMidnight(this)
        setResult(RESULT_OK, resultIntent())
        finish()
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

    companion object {
        /** 앱 안에서 이미 있는 위젯의 설정을 열 때 쓰는 액션. */
        const val ACTION_EDIT = "com.keon9608.biblewidget.action.EDIT"
    }
}
