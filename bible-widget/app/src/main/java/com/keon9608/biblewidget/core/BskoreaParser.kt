package com.keon9608.biblewidget.core

/**
 * 대한성서공회 성경읽기 페이지(korbibReadpage.php) HTML에서 절 목록을 뽑아낸다.
 *
 * 페이지는 절마다 `<span><span class="number">1&nbsp;&nbsp;&nbsp;</span>본문…</span>` 형태로 되어 있다.
 * 모든 `<span>`의 전체 텍스트(하위 요소 포함)를 모은 뒤 "숫자 + 공백 + 본문" 모양인 것만 절로 본다.
 * 본문 안의 각주 표시(`1)`)는 지우고, 줄바꿈 뒤에 붙는 각주 설명은 버린다.
 */
object BskoreaParser {

    fun parse(html: String): List<Verse> {
        // 본문 영역(id="tdBible1") 이후만 보면 머리말/메뉴에 있는 숫자 span과 섞일 일이 줄어든다.
        val container = CONTAINER.find(html)
        if (container != null) {
            val verses = parseSpans(html.substring(container.range.first))
            if (verses.isNotEmpty()) return verses
        }
        return parseSpans(html)
    }

    private fun parseSpans(html: String): List<Verse> {
        val seen = HashSet<Int>()
        val verses = ArrayList<Verse>()
        for (raw in collectSpanTexts(html)) {
            val match = VERSE.find(raw.replace(' ', ' ').trim()) ?: continue
            val number = match.groupValues[1].toIntOrNull() ?: continue
            val text = match.groupValues[2]
                .replace(FOOTNOTE_MARK, "")
                .lineSequence().first()
                .replace(SPACES, " ")
                .trim()
            if (text.isEmpty()) continue
            if (seen.add(number)) verses += Verse(number, text)
        }
        verses.sortBy { it.number }
        return verses
    }

    /**
     * 모든 `<span>` 요소의 텍스트를 닫히는 순서대로 돌려준다. 안쪽 span이 바깥 span보다 먼저 나온다.
     * `<script>`/`<style>` 내용과 주석은 무시한다.
     */
    internal fun collectSpanTexts(html: String): List<String> {
        val open = ArrayList<StringBuilder>()
        val result = ArrayList<String>()
        var pos = 0
        var skipUntil: String? = null
        for (tag in TAG.findAll(html)) {
            if (skipUntil == null && tag.range.first > pos) {
                val text = decodeEntities(html.substring(pos, tag.range.first))
                for (buf in open) buf.append(text)
            }
            pos = tag.range.last + 1
            if (tag.value.startsWith("<!")) continue
            val closing = tag.groupValues[1] == "/"
            val name = tag.groupValues[2].lowercase()
            if (skipUntil != null) {
                if (closing && name == skipUntil) skipUntil = null
                continue
            }
            when {
                name == "script" || name == "style" -> if (!closing && !tag.value.endsWith("/>")) skipUntil = name
                name != "span" -> Unit
                closing -> if (open.isNotEmpty()) result += open.removeAt(open.size - 1).toString()
                tag.value.endsWith("/>") -> Unit
                else -> open += StringBuilder()
            }
        }
        if (skipUntil == null && pos < html.length) {
            val text = decodeEntities(html.substring(pos))
            for (buf in open) buf.append(text)
        }
        // 닫히지 않은 span도 버리지 않는다.
        while (open.isNotEmpty()) result += open.removeAt(open.size - 1).toString()
        return result
    }

    internal fun decodeEntities(s: String): String {
        if (s.indexOf('&') < 0) return s
        return ENTITY.replace(s) { m ->
            val body = m.groupValues[1]
            when {
                body.startsWith("#x") || body.startsWith("#X") ->
                    body.substring(2).toIntOrNull(16)?.let(::codePointString)
                body.startsWith("#") -> body.substring(1).toIntOrNull()?.let(::codePointString)
                else -> NAMED[body]
            } ?: m.value
        }
    }

    private fun codePointString(cp: Int): String? =
        if (Character.isValidCodePoint(cp)) String(Character.toChars(cp)) else null

    private val CONTAINER = Regex("""<[a-zA-Z]+\b[^>]*\bid\s*=\s*["']?tdBible1\b[^>]*>""")
    private val TAG = Regex("""<!--.*?-->|<!(?:[^>]*)>|<(/?)([a-zA-Z][a-zA-Z0-9]*)\b[^>]*>""", RegexOption.DOT_MATCHES_ALL)
    private val ENTITY = Regex("""&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);""")
    private val VERSE = Regex("""^(\d+)\s+(.+)$""", RegexOption.DOT_MATCHES_ALL)
    private val FOOTNOTE_MARK = Regex("""\d+\)""")
    private val SPACES = Regex("""[ \t　]+""")
    private val NAMED = mapOf(
        "nbsp" to " ", "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"",
        "apos" to "'", "middot" to "·", "lsquo" to "‘", "rsquo" to "’",
        "ldquo" to "“", "rdquo" to "”", "hellip" to "…", "ndash" to "–", "mdash" to "—",
    )
}
