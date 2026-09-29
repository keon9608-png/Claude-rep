package com.keon9608.biblewidget.core

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset
import javax.net.ssl.SSLException

/**
 * 장 단위로 본문을 가져온다. 한 번 받은 장은 [baseDir] 아래에 저장해 두고 다시 받지 않는다.
 *
 * 본문 출처: 대한성서공회 성경읽기 (https://www.bskorea.or.kr/bible/korbibReadpage.php), 기본 역본은 개역개정(GAE).
 */
class BibleRepository(
    baseDir: File,
    private val version: String = VERSION_GAE,
    private val fetcher: (String) -> String = ::httpGet,
) {
    private val dir = File(baseDir, "bible/$version")

    /** 저장된 장이 있으면 그것을, 없으면 사이트에서 받아 저장한 뒤 돌려준다. 실패하면 null. */
    fun chapter(ref: ChapterRef, allowNetwork: Boolean = true): List<Verse>? {
        cached(ref)?.let { return it }
        if (!allowNetwork) return null
        val verses = try {
            BskoreaParser.parse(fetcher(url(ref)))
        } catch (e: IOException) {
            return null
        }
        if (verses.isEmpty()) return null
        save(ref, verses)
        return verses
    }

    fun cached(ref: ChapterRef): List<Verse>? {
        val file = file(ref)
        if (!file.isFile) return null
        val verses = try {
            file.readLines(Charsets.UTF_8).mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) return@mapNotNull null
                val number = line.substring(0, tab).toIntOrNull() ?: return@mapNotNull null
                Verse(number, line.substring(tab + 1))
            }
        } catch (e: IOException) {
            return null
        }
        return verses.ifEmpty { null }
    }

    private fun save(ref: ChapterRef, verses: List<Verse>) {
        val file = file(ref)
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.${System.nanoTime()}.tmp")
        try {
            tmp.writeText(
                verses.joinToString("\n") { "${it.number}\t${it.text.replace('\t', ' ').replace('\n', ' ')}" },
                Charsets.UTF_8,
            )
            if (!tmp.renameTo(file)) tmp.delete()
        } catch (e: IOException) {
            tmp.delete()
        }
    }

    private fun file(ref: ChapterRef) = File(dir, "${ref.book.code}/${ref.chapter}.txt")

    fun url(ref: ChapterRef): String =
        "$BASE_URL?version=$version&book=${ref.book.code}&chap=${ref.chapter}"

    companion object {
        const val VERSION_GAE = "GAE"
        const val BASE_URL = "https://www.bskorea.or.kr/bible/korbibReadpage.php"

        /**
         * GET 요청. HTTPS 인증서 문제로 실패하면(일부 기기는 중간 인증서를 못 찾음) HTTP로 한 번 더 시도한다.
         * 공개된 성경 본문만 받으므로 개인정보가 오가지 않는다.
         */
        fun httpGet(url: String): String {
            var lastError: IOException = IOException("request failed: $url")
            repeat(2) {
                try {
                    return getOnce(url)
                } catch (e: IOException) {
                    lastError = e
                }
            }
            if (lastError is SSLException && url.startsWith("https://")) {
                return getOnce("http://" + url.removePrefix("https://"))
            }
            throw lastError
        }

        private fun getOnce(url: String): String {
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) BibleWidget/1.0")
                conn.setRequestProperty("Accept-Language", "ko-KR,ko;q=0.9")
                val code = conn.responseCode
                if (code !in 200..299) throw IOException("HTTP $code")
                val charset = charsetOf(conn.contentType)
                return conn.inputStream.use { it.readBytes().toString(charset) }
            } finally {
                conn.disconnect()
            }
        }

        internal fun charsetOf(contentType: String?): Charset {
            val name = contentType
                ?.split(';')
                ?.map { it.trim() }
                ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
                ?.substringAfter('=')
                ?.trim('"', ' ')
            return try {
                if (name.isNullOrEmpty()) Charsets.UTF_8 else Charset.forName(name)
            } catch (e: IllegalArgumentException) {
                Charsets.UTF_8
            }
        }
    }
}
