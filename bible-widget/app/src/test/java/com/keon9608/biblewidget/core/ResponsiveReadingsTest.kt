package com.keon9608.biblewidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveReadingsTest {

    @Test
    fun has137NumberedReadings() {
        assertEquals(137, ResponsiveReadings.all.size)
        assertEquals((1..137).toList(), ResponsiveReadings.all.map { it.number })
    }

    @Test
    fun titlesMatchTheHymnalIndex() {
        val byNumber = ResponsiveReadings.all.associate { it.number to it.title }
        assertEquals("시편 1편", byNumber[1])
        assertEquals("시편 23편", byNumber[13])
        assertEquals("시편 119편", byNumber[52])
        assertEquals("시편 150편", byNumber[66])
        assertEquals("잠언 3장", byNumber[67])
        assertEquals("이사야 40장(1)", byNumber[68])
        assertEquals("요한복음 1장", byNumber[76])
        assertEquals("요한계시록 21장", byNumber[87])
        assertEquals("세례(침례)(1)", byNumber[88])
        assertEquals("나라 사랑(5)", byNumber[103])
        assertEquals("자연과 환경", byNumber[113])
        assertEquals("성탄절(1)", byNumber[119])
        assertEquals("삼위일체", byNumber[137])
    }

    @Test
    fun scriptureReadingsPointAtTheChapterInTheirTitle() {
        val scripture = ResponsiveReadings.all.take(87)
        assertTrue(scripture.all { it.isAvailable })
        for (reading in scripture) {
            val chapter = reading.ranges.single().chapter
            val expected = "${chapter.book.name} ${chapter.chapter}${chapter.book.chapterUnit}"
            assertTrue("${reading.label()} vs $expected", reading.title.replace("요한1서", "요한일서").startsWith(expected))
        }
    }

    @Test
    fun seasonalReadingsAreNotSelectableYet() {
        assertTrue(ResponsiveReadings.all.drop(87).none { it.isAvailable })
        assertEquals(87, ResponsiveReadings.available.size)
    }

    @Test
    fun isaiah40IsSplitWithoutGapOrOverlap() {
        val first = ResponsiveReadings.forNumber(68).ranges.single()
        val second = ResponsiveReadings.forNumber(69).ranges.single()
        assertEquals(first.chapter, second.chapter)
        assertEquals(1, first.from)
        assertEquals(first.to!! + 1, second.from)
        assertTrue(second.contains(31))
        assertFalse(first.contains(12))
    }

    @Test
    fun navigationSkipsUnavailableAndWraps() {
        assertEquals(2, ResponsiveReadings.next(1).number)
        assertEquals(87, ResponsiveReadings.prev(1).number)
        assertEquals(1, ResponsiveReadings.next(87).number)
        // 고를 수 없는 번호는 첫 교독문으로
        assertEquals(1, ResponsiveReadings.forNumber(120).number)
        assertEquals(1, ResponsiveReadings.forNumber(0).number)
    }

    @Test
    fun parsesMultipleRanges() {
        val ranges = ResponsiveReadings.parseRanges("isa 9:2-7; luk 2")
        assertEquals(2, ranges.size)
        assertEquals(PassageRange(ChapterRef(22, 9), 2, 7), ranges[0])
        assertEquals(PassageRange(ChapterRef(41, 2), 1, null), ranges[1])
    }
}
