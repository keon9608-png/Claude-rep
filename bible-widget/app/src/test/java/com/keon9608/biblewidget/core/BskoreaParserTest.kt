package com.keon9608.biblewidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BskoreaParserTest {

    // 대한성서공회 성경읽기 페이지와 같은 모양의 HTML (본문은 창세기 1장 일부).
    private val page = """
        <html><head>
        <meta charset="utf-8">
        <script>var tdBible1 = document.getElementById('tdBible1'); if (a < b) { x = "<span>9 가짜</span>"; }</script>
        <style>.number { color: red }</style>
        </head><body>
        <div class="menu"><span>로그인</span></div>
        <div id="tdBible1" class="bible_read">
          <div class="smallTitle">천지 창조</div>
          <span><span class="number">1&nbsp;&nbsp;&nbsp;</span>태초에 하나님이 천지를 창조하시니라</span><br>
          <span><span class="number">2&nbsp;&nbsp;&nbsp;</span>땅이 혼돈하고 공허하며 흑암이 깊음 위에 있고 하나님의 영은 수면 위에 운행하시니라</span><br>
          <span><span class="number">3&nbsp;&nbsp;&nbsp;</span>하나님이 이르시되 빛이 있으라 하시니 빛이 있었고</span><br>
          <!-- <span>4 주석 처리된 가짜</span> -->
          <span><span class="number">4&nbsp;&nbsp;&nbsp;</span>빛이 하나님이 보시기에 좋았더라 하나님이 빛과 어둠을 나누사</span><br>
        </div>
        </body></html>
    """.trimIndent()

    @Test
    fun parsesVersesInOrder() {
        val verses = BskoreaParser.parse(page)
        assertEquals(listOf(1, 2, 3, 4), verses.map { it.number })
        assertEquals("태초에 하나님이 천지를 창조하시니라", verses[0].text)
        assertEquals("하나님이 이르시되 빛이 있으라 하시니 빛이 있었고", verses[2].text)
    }

    @Test
    fun ignoresScriptsCommentsAndNonVerseSpans() {
        val texts = BskoreaParser.parse(page).map { it.text }
        assertTrue(texts.none { "가짜" in it || "로그인" in it })
    }

    @Test
    fun removesFootnoteMarkersAndExplanations() {
        val html = """
            <span><span class="number">16&nbsp;&nbsp;&nbsp;</span>하나님이<sup>1)</sup> 세상을 이처럼 사랑하사
            1) 또는 다른 해석</span>
        """.trimIndent()
        val verses = BskoreaParser.parse(html)
        assertEquals(1, verses.size)
        assertEquals(16, verses[0].number)
        assertEquals("하나님이 세상을 이처럼 사랑하사", verses[0].text)
    }

    @Test
    fun handlesNumberOnItsOwnLine() {
        val html = "<span><span class=\"number\">5&nbsp;</span>\n   빛을 낮이라 부르시고</span>"
        assertEquals(listOf(Verse(5, "빛을 낮이라 부르시고")), BskoreaParser.parse(html))
    }

    @Test
    fun keepsFirstOccurrenceOfEachVerse() {
        val html = "<span>1 첫째</span><span>1 둘째</span><span>2 셋째</span>"
        assertEquals(listOf(Verse(1, "첫째"), Verse(2, "셋째")), BskoreaParser.parse(html))
    }

    @Test
    fun fallsBackToWholePageWithoutContainer() {
        val html = "<body><span>1   태초에</span></body>"
        assertEquals(listOf(Verse(1, "태초에")), BskoreaParser.parse(html))
    }

    @Test
    fun emptyPageGivesNoVerses() {
        assertEquals(emptyList<Verse>(), BskoreaParser.parse("<html><body>없는 장</body></html>"))
    }

    @Test
    fun decodesEntities() {
        assertEquals("a & b < c “d”   한", BskoreaParser.decodeEntities("a &amp; b &lt; c &ldquo;d&rdquo; &nbsp; &#54620;"))
        assertEquals("&unknown;", BskoreaParser.decodeEntities("&unknown;"))
    }
}
