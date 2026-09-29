package com.keon9608.biblewidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class BibleRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val html = "<div id=\"tdBible1\"><span><span class=\"number\">1&nbsp;</span>태초에\t하나님이</span>" +
        "<span><span class=\"number\">2&nbsp;</span>땅이 혼돈하고</span></div>"

    @Test
    fun fetchesOnceThenServesFromCache() {
        val requested = mutableListOf<String>()
        val repo = BibleRepository(tmp.root) { url -> requested += url; html }
        val gen1 = ChapterRef(0, 1)

        val first = repo.chapter(gen1)
        val second = repo.chapter(gen1)

        assertEquals(listOf(Verse(1, "태초에 하나님이"), Verse(2, "땅이 혼돈하고")), first)
        assertEquals(first, second)
        assertEquals(listOf("https://www.bskorea.or.kr/bible/korbibReadpage.php?version=GAE&book=gen&chap=1"), requested)

        // 새 저장소 객체도 디스크 캐시를 읽는다.
        val offline = BibleRepository(tmp.root) { throw IOException("offline") }
        assertEquals(first, offline.chapter(gen1))
    }

    @Test
    fun returnsNullOnNetworkErrorOrEmptyPage() {
        val failing = BibleRepository(tmp.root) { throw IOException("offline") }
        assertNull(failing.chapter(ChapterRef(0, 1)))

        val empty = BibleRepository(tmp.root) { "<html></html>" }
        assertNull(empty.chapter(ChapterRef(0, 1)))
        assertNull(empty.cached(ChapterRef(0, 1)))
    }

    @Test
    fun noNetworkWhenNotAllowed() {
        val repo = BibleRepository(tmp.root) { throw AssertionError("should not fetch") }
        assertNull(repo.chapter(ChapterRef(0, 1), allowNetwork = false))
    }

    @Test
    fun charsetFromContentType() {
        assertEquals(Charsets.UTF_8, BibleRepository.charsetOf(null))
        assertEquals(Charsets.UTF_8, BibleRepository.charsetOf("text/html"))
        assertEquals("EUC-KR", BibleRepository.charsetOf("text/html; charset=euc-kr").name())
        assertEquals(Charsets.UTF_8, BibleRepository.charsetOf("text/html; charset=\"bogus-charset\""))
    }
}
