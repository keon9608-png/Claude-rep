package com.keon9608.biblewidget.core

/** 성경 한 권. [code]는 대한성서공회 성경읽기 페이지의 book 파라미터 값. */
data class Book(
    val index: Int,
    val code: String,
    val name: String,
    val short: String,
    val chapters: Int,
) {
    /** 시편은 "장" 대신 "편"을 쓴다. */
    val chapterUnit: String get() = if (code == "psa") "편" else "장"
}

/** 한 장의 위치. */
data class ChapterRef(val bookIndex: Int, val chapter: Int) {
    init {
        require(bookIndex in Bible.books.indices) { "bookIndex out of range: $bookIndex" }
        require(chapter in 1..Bible.books[bookIndex].chapters) { "chapter out of range: $chapter" }
    }

    val book: Book get() = Bible.books[bookIndex]

    /** 다음 장. 요한계시록 22장 다음은 창세기 1장. */
    fun next(): ChapterRef = when {
        chapter < book.chapters -> ChapterRef(bookIndex, chapter + 1)
        else -> ChapterRef((bookIndex + 1) % Bible.books.size, 1)
    }

    /** 이전 장. 창세기 1장 이전은 요한계시록 22장. */
    fun prev(): ChapterRef = when {
        chapter > 1 -> ChapterRef(bookIndex, chapter - 1)
        else -> {
            val prevBook = Math.floorMod(bookIndex - 1, Bible.books.size)
            ChapterRef(prevBook, Bible.books[prevBook].chapters)
        }
    }

    /** 예: "창세기 1장", "시편 23편" */
    fun label(): String = "${book.name} $chapter${book.chapterUnit}"
}

/** 한 절의 위치. */
data class VerseRef(val bookIndex: Int, val chapter: Int, val verse: Int) {
    val chapterRef: ChapterRef get() = ChapterRef(bookIndex, chapter)
    val book: Book get() = Bible.books[bookIndex]

    /** 예: "빌립보서 4:19" */
    fun label(): String = "${book.name} $chapter:$verse"
}

data class Verse(val number: Int, val text: String)

object Bible {
    val books: List<Book> = listOf(
        // 구약
        Triple("gen", "창세기", "창") to 50,
        Triple("exo", "출애굽기", "출") to 40,
        Triple("lev", "레위기", "레") to 27,
        Triple("num", "민수기", "민") to 36,
        Triple("deu", "신명기", "신") to 34,
        Triple("jos", "여호수아", "수") to 24,
        Triple("jdg", "사사기", "삿") to 21,
        Triple("rut", "룻기", "룻") to 4,
        Triple("1sa", "사무엘상", "삼상") to 31,
        Triple("2sa", "사무엘하", "삼하") to 24,
        Triple("1ki", "열왕기상", "왕상") to 22,
        Triple("2ki", "열왕기하", "왕하") to 25,
        Triple("1ch", "역대상", "대상") to 29,
        Triple("2ch", "역대하", "대하") to 36,
        Triple("ezr", "에스라", "스") to 10,
        Triple("neh", "느헤미야", "느") to 13,
        Triple("est", "에스더", "에") to 10,
        Triple("job", "욥기", "욥") to 42,
        Triple("psa", "시편", "시") to 150,
        Triple("pro", "잠언", "잠") to 31,
        Triple("ecc", "전도서", "전") to 12,
        Triple("sng", "아가", "아") to 8,
        Triple("isa", "이사야", "사") to 66,
        Triple("jer", "예레미야", "렘") to 52,
        Triple("lam", "예레미야애가", "애") to 5,
        Triple("ezk", "에스겔", "겔") to 48,
        Triple("dan", "다니엘", "단") to 12,
        Triple("hos", "호세아", "호") to 14,
        Triple("jol", "요엘", "욜") to 3,
        Triple("amo", "아모스", "암") to 9,
        Triple("oba", "오바댜", "옵") to 1,
        Triple("jon", "요나", "욘") to 4,
        Triple("mic", "미가", "미") to 7,
        Triple("nam", "나훔", "나") to 3,
        Triple("hab", "하박국", "합") to 3,
        Triple("zep", "스바냐", "습") to 3,
        Triple("hag", "학개", "학") to 2,
        Triple("zec", "스가랴", "슥") to 14,
        Triple("mal", "말라기", "말") to 4,
        // 신약
        Triple("mat", "마태복음", "마") to 28,
        Triple("mrk", "마가복음", "막") to 16,
        Triple("luk", "누가복음", "눅") to 24,
        Triple("jhn", "요한복음", "요") to 21,
        Triple("act", "사도행전", "행") to 28,
        Triple("rom", "로마서", "롬") to 16,
        Triple("1co", "고린도전서", "고전") to 16,
        Triple("2co", "고린도후서", "고후") to 13,
        Triple("gal", "갈라디아서", "갈") to 6,
        Triple("eph", "에베소서", "엡") to 6,
        Triple("php", "빌립보서", "빌") to 4,
        Triple("col", "골로새서", "골") to 4,
        Triple("1th", "데살로니가전서", "살전") to 5,
        Triple("2th", "데살로니가후서", "살후") to 3,
        Triple("1ti", "디모데전서", "딤전") to 6,
        Triple("2ti", "디모데후서", "딤후") to 4,
        Triple("tit", "디도서", "딛") to 3,
        Triple("phm", "빌레몬서", "몬") to 1,
        Triple("heb", "히브리서", "히") to 13,
        Triple("jas", "야고보서", "약") to 5,
        Triple("1pe", "베드로전서", "벧전") to 5,
        Triple("2pe", "베드로후서", "벧후") to 3,
        Triple("1jn", "요한일서", "요일") to 5,
        Triple("2jn", "요한이서", "요이") to 1,
        Triple("3jn", "요한삼서", "요삼") to 1,
        Triple("jud", "유다서", "유") to 1,
        Triple("rev", "요한계시록", "계") to 22,
    ).mapIndexed { i, (names, chapters) ->
        Book(i, names.first, names.second, names.third, chapters)
    }

    private val byCode: Map<String, Book> = books.associateBy { it.code }

    fun byCode(code: String): Book? = byCode[code.lowercase()]

    /** "jhn 3:16" 형식의 참조를 해석한다. */
    fun parseRef(ref: String): VerseRef {
        val match = REF.matchEntire(ref.trim()) ?: throw IllegalArgumentException("bad ref: $ref")
        val (code, chapter, verse) = match.destructured
        val book = byCode(code) ?: throw IllegalArgumentException("unknown book: $code")
        val chapterNum = chapter.toInt()
        require(chapterNum in 1..book.chapters) { "chapter out of range: $ref" }
        return VerseRef(book.index, chapterNum, verse.toInt())
    }

    private val REF = Regex("""([1-3]?[a-z]{2,3})\s+(\d+):(\d+)""")
}
