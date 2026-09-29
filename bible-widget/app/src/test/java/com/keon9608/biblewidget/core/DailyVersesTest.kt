package com.keon9608.biblewidget.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyVersesTest {

    @Test
    fun listIsLargeAndHasNoDuplicates() {
        assertTrue(DailyVerses.all.size >= 180)
        assertEquals(DailyVerses.all.size, DailyVerses.all.toSet().size)
    }

    @Test
    fun forDayCyclesAndHandlesAnyDate() {
        val size = DailyVerses.all.size.toLong()
        val today = LocalDate.of(2026, 9, 30).toEpochDay()
        assertEquals(DailyVerses.forDay(today), DailyVerses.forDay(today + size))
        assertEquals(DailyVerses.all.last(), DailyVerses.forDay(-1))
        assertEquals(DailyVerses.all.first(), DailyVerses.forDay(0))
    }

    @Test
    fun consecutiveDaysDiffer() {
        val today = LocalDate.of(2026, 9, 30).toEpochDay()
        for (d in 0 until DailyVerses.all.size) {
            assertTrue(DailyVerses.forDay(today + d) != DailyVerses.forDay(today + d + 1))
        }
    }
}
