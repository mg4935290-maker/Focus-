package com.example.quran.domain

import com.example.quran.db.AyahEntity
import com.example.quran.db.SurahCatalog
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ParsedQuranResult(
    val ayahs: List<AyahEntity>,
    val surahCount: Int,
    val isValid: Boolean,
    val errorMessage: String? = null
)

object QuranTextParser {

    private val refLineRegex = Regex("""^\[?(\d{1,3})\s*[:|]\s*(\d{1,3})\]?\s*(.+)$""")
    private val pipedLineRegex = Regex("""^(\d{1,3})\s*[|\t;]\s*(\d{1,3})\s*[|\t;]\s*(.+)$""")
    private val numberedAyahRegex = Regex("""^\(?(\d{1,3})\)?[\s.\-–:]+(.+)$""")

    fun parseJson(inputStream: InputStream, sourceFileId: String): ParsedQuranResult {
        return try {
            val raw = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
            if (raw.isBlank()) {
                return ParsedQuranResult(emptyList(), 0, false, "الملف فارغ ولا يحتوي على بيانات صالحة.")
            }

            val trimmed = raw.trimStart()
            val collector = LinkedHashMap<Pair<Int, Int>, AyahEntity>()

            when {
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    extractFromRootObject(root, sourceFileId, collector)
                }
                trimmed.startsWith("[") -> {
                    val array = JSONArray(trimmed)
                    extractFromArray(array, null, sourceFileId, collector)
                }
                else -> {
                    return ParsedQuranResult(emptyList(), 0, false, "صيغة ملف JSON غير صالحة.")
                }
            }

            finalizeResult(collector)
        } catch (t: Throwable) {
            ParsedQuranResult(
                emptyList(),
                0,
                false,
                "تعذر قراءة ملف JSON: ${t.localizedMessage ?: "صيغة غير صالحة"}"
            )
        }
    }

    fun parseTxt(inputStream: InputStream, sourceFileId: String): ParsedQuranResult {
        return try {
            val collector = LinkedHashMap<Pair<Int, Int>, AyahEntity>()
            var currentSurah: Int? = null
            val surahNamesArabic = SurahCatalog.allSurahs.associateBy({ it.nameArabic }, { it.number })
            val surahNamesEnglish = SurahCatalog.allSurahs.associateBy({ it.nameEnglish.lowercase() }, { it.number })

            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val clean = line.trimStart('\uFEFF').trim()
                    if (clean.isNotEmpty()) {
                        val piped = pipedLineRegex.matchEntire(clean)
                        val ref = refLineRegex.matchEntire(clean)

                        when {
                            piped != null -> {
                                val surah = piped.groupValues[1].toIntOrNull()
                                val ayah = piped.groupValues[2].toIntOrNull()
                                val text = piped.groupValues[3].trim()
                                addAyahIfValid(collector, surah, ayah, text, null, null, sourceFileId)
                                if (surah in 1..114) currentSurah = surah
                            }
                            ref != null -> {
                                val surah = ref.groupValues[1].toIntOrNull()
                                val ayah = ref.groupValues[2].toIntOrNull()
                                val text = ref.groupValues[3].trim()
                                addAyahIfValid(collector, surah, ayah, text, null, null, sourceFileId)
                                if (surah in 1..114) currentSurah = surah
                            }
                            else -> {
                                val matchedHeader = detectSurahHeader(clean, surahNamesArabic, surahNamesEnglish)
                                if (matchedHeader != null) {
                                    currentSurah = matchedHeader
                                } else if (currentSurah != null) {
                                    val numbered = numberedAyahRegex.matchEntire(clean)
                                    if (numbered != null) {
                                        val ayah = numbered.groupValues[1].toIntOrNull()
                                        val text = numbered.groupValues[2].trim()
                                        addAyahIfValid(collector, currentSurah, ayah, text, null, null, sourceFileId)
                                    }
                                }
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }

            finalizeResult(collector)
        } catch (t: Throwable) {
            ParsedQuranResult(
                emptyList(),
                0,
                false,
                "تعذر قراءة الملف النصي: ${t.localizedMessage ?: "خطأ غير متوقع"}"
            )
        }
    }

    private fun extractFromRootObject(
        root: JSONObject,
        sourceFileId: String,
        collector: MutableMap<Pair<Int, Int>, AyahEntity>
    ) {
        val dataObj = root.optJSONObject("data")
        val surahsArray = root.optJSONArray("surahs")
            ?: dataObj?.optJSONArray("surahs")
            ?: root.optJSONArray("chapters")
            ?: root.optJSONArray("verses")
            ?: root.optJSONArray("ayahs")

        if (surahsArray != null) {
            extractFromArray(surahsArray, root.optInt("number", -1).takeIf { it in 1..114 }, sourceFileId, collector)
            return
        }

        // Map keyed by surah number ("1": [...])
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val surahNum = key.toIntOrNull() ?: continue
            if (surahNum !in 1..114) continue
            val value = root.opt(key)
            when (value) {
                is JSONArray -> extractFromArray(value, surahNum, sourceFileId, collector)
                is JSONObject -> {
                    val innerKeys = value.keys()
                    while (innerKeys.hasNext()) {
                        val ayahKey = innerKeys.next()
                        val ayahNum = ayahKey.toIntOrNull() ?: continue
                        val text = value.optString(ayahKey)
                        addAyahIfValid(collector, surahNum, ayahNum, text, null, null, sourceFileId)
                    }
                }
            }
        }
    }

    private fun extractFromArray(
        array: JSONArray,
        fallbackSurah: Int?,
        sourceFileId: String,
        collector: MutableMap<Pair<Int, Int>, AyahEntity>
    ) {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val nestedAyahs = item.optJSONArray("ayahs") ?: item.optJSONArray("verses")
            if (nestedAyahs != null) {
                val surahNum = item.optInt("number", -1)
                    .takeIf { it in 1..114 }
                    ?: item.optInt("surah", -1).takeIf { it in 1..114 }
                    ?: item.optInt("id", -1).takeIf { it in 1..114 }
                extractFromArray(nestedAyahs, surahNum, sourceFileId, collector)
            } else {
                val surahNum = listOf(
                    item.optInt("surah", -1),
                    item.optInt("surah_number", -1),
                    item.optInt("surahNumber", -1),
                    item.optInt("chapter", -1)
                ).firstOrNull { it in 1..114 } ?: fallbackSurah

                val ayahNum = listOf(
                    item.optInt("ayah", -1),
                    item.optInt("numberInSurah", -1),
                    item.optInt("ayah_number", -1),
                    item.optInt("ayahNumber", -1),
                    item.optInt("verse", -1),
                    item.optInt("id", -1)
                ).firstOrNull { it >= 1 }

                val text = listOf(
                    item.optString("text_uthmani", ""),
                    item.optString("textUthmani", ""),
                    item.optString("uthmani", ""),
                    item.optString("text", ""),
                    item.optString("content", "")
                ).firstOrNull { it.isNotBlank() }.orEmpty()

                val startMs = if (item.has("startTimeMs") && !item.isNull("startTimeMs")) {
                    item.optLong("startTimeMs", -1L).takeIf { it >= 0L }
                } else if (item.has("startMs") && !item.isNull("startMs")) {
                    item.optLong("startMs", -1L).takeIf { it >= 0L }
                } else null

                val endMs = if (item.has("endTimeMs") && !item.isNull("endTimeMs")) {
                    item.optLong("endTimeMs", -1L).takeIf { it >= 0L }
                } else if (item.has("endMs") && !item.isNull("endMs")) {
                    item.optLong("endMs", -1L).takeIf { it >= 0L }
                } else null

                addAyahIfValid(collector, surahNum, ayahNum, text, startMs, endMs, sourceFileId)
            }
        }
    }

    private fun detectSurahHeader(
        line: String,
        arabicNames: Map<String, Int>,
        englishNames: Map<String, Int>
    ): Int? {
        val cleaned = line.removePrefix("سورة").removePrefix("Surah").removePrefix("surah").trim()
        arabicNames[cleaned]?.let { return it }
        englishNames[cleaned.lowercase()]?.let { return it }
        cleaned.toIntOrNull()?.takeIf { it in 1..114 }?.let { return it }
        return null
    }

    private fun addAyahIfValid(
        collector: MutableMap<Pair<Int, Int>, AyahEntity>,
        surahNumber: Int?,
        ayahNumber: Int?,
        rawText: String,
        startTimeMs: Long?,
        endTimeMs: Long?,
        sourceFileId: String
    ) {
        if (surahNumber == null || surahNumber !in 1..114) return
        if (ayahNumber == null || ayahNumber <= 0) return
        // Preserve Quranic text and Uthmani script exactly as provided in source.
        if (rawText.isBlank()) return
        val words = rawText.trim().split(Regex("""\s+""")).count { it.isNotEmpty() }.coerceAtLeast(1)
        collector[surahNumber to ayahNumber] = AyahEntity(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            textUthmani = rawText,
            wordCount = words,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            sourceFileId = sourceFileId
        )
    }

    private fun finalizeResult(collector: Map<Pair<Int, Int>, AyahEntity>): ParsedQuranResult {
        if (collector.isEmpty()) {
            return ParsedQuranResult(
                emptyList(),
                0,
                false,
                "لم يتم العثور على آيات صالحة بصيغة مدعومة داخل الملف."
            )
        }
        val sorted = collector.values.sortedWith(compareBy<AyahEntity> { it.surahNumber }.thenBy { it.ayahNumber })
        val surahCount = sorted.map { it.surahNumber }.distinct().size
        return ParsedQuranResult(
            ayahs = sorted,
            surahCount = surahCount,
            isValid = true,
            errorMessage = null
        )
    }
}
