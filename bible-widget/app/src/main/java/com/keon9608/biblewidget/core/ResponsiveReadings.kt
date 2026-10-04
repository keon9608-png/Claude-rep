package com.keon9608.biblewidget.core

/** 한 장 안의 절 범위. [to]가 null이면 장 끝까지. */
data class PassageRange(val chapter: ChapterRef, val from: Int = 1, val to: Int? = null) {
    fun contains(verse: Int): Boolean = verse >= from && (to == null || verse <= to)
}

/**
 * 교독문 한 편 (새찬송가 번호).
 *
 * 본문은 담지 않고 성경 범위만 담는다. 본문은 성경과 똑같이 기기에서 개역개정으로 받아온다.
 * [ranges]가 비어 있으면 아직 성경 범위를 모르는 교독문이라 고를 수 없다.
 */
data class ResponsiveReading(val number: Int, val title: String, val ranges: List<PassageRange>) {
    val isAvailable: Boolean get() = ranges.isNotEmpty()

    /** 예: "1. 시편 1편" */
    fun label(): String = "$number. $title"
}

object ResponsiveReadings {
    // all을 만들 때 쓰므로 맨 위에 둔다 (object 안의 값은 위에서부터 차례로 만들어진다).
    private val RANGE = Regex("""([1-3]?[a-z]{2,3})\s+(\d+)(?::(\d+)-(\d+))?""")

    /**
     * 새찬송가 교독문 1–137번의 제목과 성경 범위.
     * 1–87번은 목차의 장 전체를 쓴다(찬송가는 긴 장에서 일부 절만 고른 경우가 있다).
     * 88–137번(절기·행사)은 여러 곳을 엮은 것이라 범위를 확인하기 전까지 비워 둔다.
     */
    private val ENTRIES: List<Pair<String, String?>> = listOf(
        "시편 1편" to "psa 1", "시편 2편" to "psa 2", "시편 4편" to "psa 4", "시편 5편" to "psa 5",
        "시편 8편" to "psa 8", "시편 10편" to "psa 10", "시편 13편" to "psa 13", "시편 14편" to "psa 14",
        "시편 15편" to "psa 15", "시편 16편" to "psa 16", "시편 17편" to "psa 17", "시편 19편" to "psa 19",
        "시편 23편" to "psa 23", "시편 24편" to "psa 24", "시편 27편" to "psa 27", "시편 28편" to "psa 28",
        "시편 29편" to "psa 29", "시편 31편" to "psa 31", "시편 32편" to "psa 32", "시편 33편" to "psa 33",
        "시편 34편" to "psa 34", "시편 37편" to "psa 37", "시편 43편" to "psa 43", "시편 46편" to "psa 46",
        "시편 47편" to "psa 47", "시편 50편" to "psa 50", "시편 51편" to "psa 51", "시편 63편" to "psa 63",
        "시편 65편" to "psa 65", "시편 67편" to "psa 67", "시편 68편" to "psa 68", "시편 71편" to "psa 71",
        "시편 72편" to "psa 72", "시편 81편" to "psa 81", "시편 84편" to "psa 84", "시편 90편" to "psa 90",
        "시편 91편" to "psa 91", "시편 92편" to "psa 92", "시편 95편" to "psa 95", "시편 96편" to "psa 96",
        "시편 97편" to "psa 97", "시편 98편" to "psa 98", "시편 99편" to "psa 99", "시편 100편" to "psa 100",
        "시편 103편" to "psa 103", "시편 104편" to "psa 104", "시편 105편" to "psa 105", "시편 106편" to "psa 106",
        "시편 108편" to "psa 108", "시편 116편" to "psa 116", "시편 118편" to "psa 118", "시편 119편" to "psa 119",
        "시편 121편" to "psa 121", "시편 126편" to "psa 126", "시편 127편" to "psa 127", "시편 128편" to "psa 128",
        "시편 130편" to "psa 130", "시편 133편" to "psa 133", "시편 136편" to "psa 136", "시편 139편" to "psa 139",
        "시편 142편" to "psa 142", "시편 143편" to "psa 143", "시편 145편" to "psa 145", "시편 148편" to "psa 148",
        "시편 149편" to "psa 149", "시편 150편" to "psa 150",
        "잠언 3장" to "pro 3",
        // 이사야 40장은 두 편으로 나뉜다. 나누는 절은 찬송가로 확인하지 못해 앞뒤 절반으로 둔다.
        "이사야 40장(1)" to "isa 40:1-11", "이사야 40장(2)" to "isa 40:12-31",
        "이사야 42장" to "isa 42", "이사야 55장" to "isa 55", "이사야 58장" to "isa 58", "이사야 65장" to "isa 65",
        "마태복음 5장" to "mat 5", "마태복음 6장" to "mat 6",
        "요한복음 1장" to "jhn 1", "요한복음 3장" to "jhn 3", "요한복음 14장" to "jhn 14", "요한복음 15장" to "jhn 15",
        "고린도후서 4장" to "2co 4", "에베소서 4장" to "eph 4", "빌립보서 2장" to "php 2", "빌립보서 4장" to "php 4",
        "히브리서 11장" to "heb 11", "요한1서 4장" to "1jn 4", "요한계시록 14장" to "rev 14", "요한계시록 21장" to "rev 21",
        "세례(침례)(1)" to null, "세례(침례)(2)" to null, "세례(침례)(3)" to null,
        "성찬(1)" to null, "성찬(2)" to null, "새해(1)" to null, "새해(2)" to null,
        "가정주일" to null, "어린이주일" to null, "청년주일" to null, "어버이주일" to null,
        "나라 사랑(1)" to null, "나라 사랑(2)" to null, "나라 사랑(3)" to null, "나라 사랑(4)" to null,
        "나라 사랑(5)" to null, "종교개혁주일" to null, "감사절(1)" to null, "감사절(2)" to null,
        "임직식(1)" to null, "임직식(2)" to null, "헌당예배" to null, "선교주일" to null, "성서주일" to null,
        "교회교육주일" to null, "자연과 환경" to null, "이웃 사랑" to null,
        "구주 강림(1)" to null, "구주 강림(2)" to null, "구주 강림(3)" to null, "구주 강림(4)" to null,
        "성탄절(1)" to null, "성탄절(2)" to null, "주현절(1)" to null, "주현절(2)" to null, "주현절(3)" to null,
        "사순절(1)" to null, "사순절(2)" to null, "사순절(3)" to null, "사순절(4)" to null, "사순절(5)" to null,
        "종려주일" to null, "고난주간(1)" to null, "고난주간(2)" to null, "고난주간(3)" to null,
        "부활절(1)" to null, "부활절(2)" to null, "성령 강림(1)" to null, "성령 강림(2)" to null,
        "삼위일체" to null,
    )

    val all: List<ResponsiveReading> = ENTRIES.mapIndexed { i, (title, spec) ->
        ResponsiveReading(i + 1, title, spec?.let(::parseRanges).orEmpty())
    }

    /** 설정에서 고를 수 있는 교독문 (성경 범위를 아는 것). */
    val available: List<ResponsiveReading> = all.filter { it.isAvailable }

    /** 번호로 찾되, 고를 수 없는 번호면 첫 교독문. */
    fun forNumber(number: Int): ResponsiveReading =
        all.getOrNull(number - 1)?.takeIf { it.isAvailable } ?: available.first()

    /** 고를 수 있는 것 중 다음 교독문. 마지막 다음은 처음. */
    fun next(number: Int): ResponsiveReading = step(number, +1)

    fun prev(number: Int): ResponsiveReading = step(number, -1)

    private fun step(number: Int, delta: Int): ResponsiveReading {
        val i = available.indexOf(forNumber(number))
        return available[Math.floorMod(i + delta, available.size)]
    }

    /** "psa 1", "isa 40:1-11", 여러 곳은 "isa 9:2-7; luk 2:8-14" */
    internal fun parseRanges(spec: String): List<PassageRange> = spec.split(';').map { part ->
        val m = RANGE.matchEntire(part.trim()) ?: throw IllegalArgumentException("bad range: $part")
        val book = Bible.byCode(m.groupValues[1]) ?: throw IllegalArgumentException("unknown book: $part")
        val chapter = ChapterRef(book.index, m.groupValues[2].toInt())
        val from = m.groupValues[3].toIntOrNull() ?: 1
        val to = m.groupValues[4].toIntOrNull()
        require(to == null || from <= to) { "bad range: $part" }
        PassageRange(chapter, from, to)
    }

}
