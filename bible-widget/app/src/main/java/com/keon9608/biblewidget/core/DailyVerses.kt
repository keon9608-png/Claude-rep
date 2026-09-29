package com.keon9608.biblewidget.core

/**
 * 오늘의 말씀 목록. 날짜마다 순서대로 한 구절씩 돌아가며 보여준다.
 *
 * 구절 "위치"만 담고 본문은 담지 않는다. 본문은 기기에서 대한성서공회 사이트(개역개정)에서 받아온다.
 */
object DailyVerses {
    private val REFS = listOf(
        "php 4:19", "psa 23:1", "jhn 3:16", "isa 41:10", "mat 11:28",
        "pro 3:5", "rom 8:28", "jos 1:9", "php 4:13", "psa 46:1",
        "jer 29:11", "2co 5:17", "psa 119:105", "mat 6:33", "isa 40:31",
        "1th 5:16", "1th 5:17", "1th 5:18", "psa 121:1", "psa 121:2",
        "jhn 14:6", "lam 3:22", "lam 3:23", "gal 2:20", "psa 37:5",
        "rom 12:2", "1pe 5:7", "psa 27:1", "jhn 14:27", "heb 11:1",
        "pro 3:6", "mat 7:7", "psa 23:4", "2co 12:9", "isa 43:1",
        "rom 5:8", "psa 16:8", "jhn 15:5", "php 4:6", "php 4:7",
        "deu 31:6", "eph 2:8", "psa 37:4", "mat 5:14", "mat 5:16",
        "isa 26:3", "1co 13:4", "1co 13:13", "psa 91:1", "jhn 16:33",
        "zep 3:17", "gal 5:22", "psa 103:2", "heb 13:8", "jer 33:3",
        "rom 8:39", "psa 55:22", "jhn 11:25", "mic 6:8", "eph 3:20",
        "psa 46:10", "2ti 1:7", "isa 43:19", "mat 28:20", "psa 34:8",
        "jas 1:5", "gen 28:15", "jhn 13:34", "psa 62:1", "col 3:23",
        "pro 16:3", "1jn 4:18", "exo 14:14", "heb 4:16", "psa 118:24",
        "rom 12:12", "isa 30:15", "luk 1:37", "psa 139:23", "1jn 1:9",
        "num 6:24", "num 6:25", "num 6:26", "jhn 8:32", "psa 51:10",
        "gal 6:9", "pro 4:23", "mrk 9:23", "heb 12:2", "psa 73:28",
        "isa 53:5", "rom 1:17", "psa 90:12", "jhn 10:10", "eph 2:10",
        "job 23:10", "mat 6:34", "psa 127:1", "1co 10:13", "zec 4:6",
        "psa 1:1", "psa 1:2", "jhn 1:12", "rom 10:10", "pro 16:9",
        "isa 55:8", "act 1:8", "psa 145:18", "heb 4:12", "mat 22:37",
        "neh 8:10", "1co 16:14", "psa 84:10", "jas 4:8", "2ch 7:14",
        "jhn 15:7", "psa 19:14", "mrk 11:24", "eph 4:32", "hab 3:18",
        "2ti 3:16", "psa 126:5", "mat 5:3", "col 3:15", "1sa 16:7",
        "psa 18:1", "jhn 14:1", "rom 15:13", "isa 60:1", "1pe 2:9",
        "pro 9:10", "mat 11:29", "psa 133:1", "act 16:31", "rev 21:4",
        "deu 31:8", "2co 4:18", "psa 42:5", "jhn 1:1", "php 1:6",
        "ecc 3:1", "mrk 10:27", "psa 150:6", "1jn 4:19", "eph 6:10",
        "mal 3:10", "rom 8:18", "psa 63:1", "luk 6:31", "jas 1:22",
        "pro 18:10", "mat 5:8", "psa 100:4", "1co 15:58", "hos 6:3",
        "jhn 20:29", "rom 6:23", "psa 107:1", "heb 11:6", "isa 12:2",
        "col 3:2", "psa 4:8", "mat 16:24", "1jn 4:8", "gen 1:1",
        "php 3:14", "pro 17:17", "psa 136:1", "act 20:35", "2co 9:8",
        "mic 7:7", "jhn 3:30", "rom 12:18", "psa 30:5", "1pe 3:15",
        "deu 6:5", "gal 6:2", "mat 5:9", "psa 116:12", "rev 3:20",
        "isa 9:6", "luk 2:14", "heb 10:24", "jol 2:28", "eph 5:16",
        "1th 5:11", "jas 1:12", "ecc 12:1", "jos 24:15", "1jn 3:18",
        "php 2:5", "luk 9:23", "1ki 3:9", "rom 8:1", "1jn 5:4",
        "mat 1:21", "2ti 4:7", "rom 3:23", "rev 22:20",
    )

    val all: List<VerseRef> = REFS.map(Bible::parseRef)

    /** [epochDay]는 1970-01-01부터의 일 수 (LocalDate.toEpochDay()). */
    fun forDay(epochDay: Long): VerseRef = all[Math.floorMod(epochDay, all.size.toLong()).toInt()]
}
