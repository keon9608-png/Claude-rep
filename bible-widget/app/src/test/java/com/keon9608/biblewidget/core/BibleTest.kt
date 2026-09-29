package com.keon9608.biblewidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleTest {

    @Test
    fun has66BooksAnd1189Chapters() {
        assertEquals(66, Bible.books.size)
        assertEquals(929, Bible.books.take(39).sumOf { it.chapters })
        assertEquals(260, Bible.books.drop(39).sumOf { it.chapters })
    }

    @Test
    fun codesAndNamesAreUnique() {
        assertEquals(66, Bible.books.map { it.code }.toSet().size)
        assertEquals(66, Bible.books.map { it.name }.toSet().size)
        assertEquals(66, Bible.books.map { it.short }.toSet().size)
    }

    @Test
    fun nextAndPrevWrapAcrossBooksAndTheWholeBible() {
        assertEquals(ChapterRef(0, 2), ChapterRef(0, 1).next())
        assertEquals(ChapterRef(1, 1), ChapterRef(0, 50).next())
        assertEquals(ChapterRef(0, 50), ChapterRef(1, 1).prev())
        assertEquals(ChapterRef(0, 1), ChapterRef(65, 22).next())
        assertEquals(ChapterRef(65, 22), ChapterRef(0, 1).prev())
    }

    @Test
    fun walkingForwardVisitsEveryChapterOnce() {
        var ref = ChapterRef(0, 1)
        val seen = HashSet<ChapterRef>()
        repeat(1189) {
            assertTrue(seen.add(ref))
            ref = ref.next()
        }
        assertEquals(ChapterRef(0, 1), ref)
    }

    @Test
    fun labels() {
        assertEquals("창세기 1장", ChapterRef(0, 1).label())
        assertEquals("시편 23편", ChapterRef(Bible.byCode("psa")!!.index, 23).label())
        assertEquals("빌립보서 4:19", Bible.parseRef("php 4:19").label())
    }

    @Test
    fun parseRef() {
        assertEquals(VerseRef(42, 3, 16), Bible.parseRef("jhn 3:16"))
        assertEquals(VerseRef(45, 13, 4), Bible.parseRef("1co 13:4"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRefRejectsChapterOutOfRange() {
        Bible.parseRef("jhn 22:1")
    }
}
