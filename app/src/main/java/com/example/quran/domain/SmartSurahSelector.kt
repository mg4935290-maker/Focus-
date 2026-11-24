package com.example.quran.domain

import com.example.quran.db.QuranAudioTrackEntity
import com.example.quran.db.SurahEntity
import kotlin.math.abs
import kotlin.math.roundToInt

enum class QuranSessionMode {
    READING,
    LISTENING,
    COMBINED
}

data class SurahDurationEstimate(
    val surah: SurahEntity,
    val estimatedMinutes: Double,
    val remainingAyahs: Int,
    val hasExactAudioDuration: Boolean
)

data class SmartSelectionResult(
    val targetMinutes: Int,
    val selectedSurahs: List<SurahDurationEstimate>,
    val totalAyahs: Int,
    val totalWords: Int,
    val estimatedMinutes: Double,
    val differenceMinutes: Double,
    val isApproximate: Boolean
)

object SmartSurahSelector {

    /**
     * Multi-factor optimization selecting 1 Surah or a cohesive set of Surahs
     * closest to the user's target duration. Never relies solely on ayah count.
     */
    fun selectSurahs(
        surahs: List<SurahEntity>,
        targetMinutes: Int,
        readingSpeedWpm: Double,
        mode: QuranSessionMode,
        audioTracks: List<QuranAudioTrackEntity>,
        preferUncompleted: Boolean = true,
        onlySurahsWithImportedText: Boolean = false,
        variantIndex: Int = 0
    ): SmartSelectionResult {
        if (surahs.isEmpty() || targetMinutes <= 0) {
            return SmartSelectionResult(
                targetMinutes = targetMinutes,
                selectedSurahs = emptyList(),
                totalAyahs = 0,
                totalWords = 0,
                estimatedMinutes = 0.0,
                differenceMinutes = targetMinutes.toDouble(),
                isApproximate = true
            )
        }

        val confirmedTracksBySurah = audioTracks
            .filter { it.surahNumber != null && it.isSurahConfirmed && it.durationMs > 0L }
            .groupBy { it.surahNumber!! }

        val effectiveWpm = readingSpeedWpm.coerceIn(55.0, 220.0)
        val pool = surahs
            .filter { !onlySurahsWithImportedText || it.hasImportedText }
            .ifEmpty { surahs }

        val estimates = pool.map { surah ->
            val completedAyahs = if (!surah.isCompleted && surah.lastReadAyah in 1 until surah.ayahCount) {
                surah.lastReadAyah
            } else {
                0
            }
            val remainingRatio = ((surah.ayahCount - completedAyahs).toDouble() / surah.ayahCount.coerceAtLeast(1))
                .coerceIn(0.15, 1.0)
            val remainingAyahs = (surah.ayahCount - completedAyahs).coerceAtLeast(1)
            val remainingWords = (surah.wordCount * remainingRatio).coerceAtLeast(5.0)

            // Word + Ayah pause factor (brief waqf between ayahs ~0.7s)
            val readingMinutes = (remainingWords / effectiveWpm) + (remainingAyahs * 0.012)

            val trackDurationMin = confirmedTracksBySurah[surah.number]
                ?.firstOrNull()
                ?.durationMs
                ?.let { it / 60_000.0 }

            val estMin = when (mode) {
                QuranSessionMode.LISTENING -> trackDurationMin ?: (readingMinutes * 0.95)
                QuranSessionMode.COMBINED -> trackDurationMin ?: readingMinutes
                QuranSessionMode.READING -> {
                    val historicalAvgMin = if (surah.timesRead > 0 && surah.totalReadMs > 0L) {
                        (surah.totalReadMs.toDouble() / surah.timesRead.toDouble()) / 60_000.0
                    } else null
                    if (historicalAvgMin != null && completedAyahs == 0) {
                        (readingMinutes * 0.6) + (historicalAvgMin * 0.4)
                    } else {
                        readingMinutes
                    }
                }
            }.coerceAtLeast(0.3)

            SurahDurationEstimate(
                surah = surah,
                estimatedMinutes = estMin,
                remainingAyahs = remainingAyahs,
                hasExactAudioDuration = trackDurationMin != null && mode != QuranSessionMode.READING
            )
        }

        // Order pool cleanly with deterministic variant offset for "Re-select" exploration
        val orderedPool = buildList {
            val baseSorted = if (preferUncompleted) {
                estimates.sortedWith(
                    compareBy<SurahDurationEstimate> { it.surah.isCompleted }
                        .thenByDescending { it.surah.lastReadAyah in 1 until it.surah.ayahCount }
                        .thenBy { it.surah.number }
                )
            } else {
                estimates.sortedBy { it.surah.number }
            }
            if (baseSorted.isNotEmpty()) {
                val shift = (variantIndex * 5) % baseSorted.size
                addAll(baseSorted.drop(shift))
                addAll(baseSorted.take(shift))
            }
        }

        val chosen = optimizeSubset(orderedPool, targetMinutes.toDouble(), variantIndex)
            .sortedBy { it.surah.number }

        val totalEst = chosen.sumOf { it.estimatedMinutes }
        val totalAyahs = chosen.sumOf { it.remainingAyahs }
        val totalWords = chosen.sumOf { it.surah.wordCount }
        val hasCalibratedHistory = chosen.any {
            it.hasExactAudioDuration || it.surah.timesRead > 0
        }

        return SmartSelectionResult(
            targetMinutes = targetMinutes,
            selectedSurahs = chosen,
            totalAyahs = totalAyahs,
            totalWords = totalWords,
            estimatedMinutes = totalEst,
            differenceMinutes = totalEst - targetMinutes.toDouble(),
            isApproximate = !hasCalibratedHistory
        )
    }

    private fun optimizeSubset(
        candidates: List<SurahDurationEstimate>,
        targetMinutes: Double,
        variantIndex: Int
    ): List<SurahDurationEstimate> {
        // Step 1: Check best single Surah first
        val singleCandidates = candidates
            .sortedBy { abs(it.estimatedMinutes - targetMinutes) }

        val bestSingle = singleCandidates.firstOrNull()
        val singleTolerance = maxOf(1.2, targetMinutes * 0.12)
        if (bestSingle != null &&
            abs(bestSingle.estimatedMinutes - targetMinutes) <= singleTolerance &&
            variantIndex % 2 == 0
        ) {
            return listOf(bestSingle)
        }

        // Step 2: Bounded DP Knapsack at 0.5-minute resolution up to 1.35x target
        val stepMin = 0.5
        val targetUnits = (targetMinutes / stepMin).roundToInt().coerceAtLeast(1)
        val maxUnits = ((targetMinutes * 1.35) / stepMin).roundToInt().coerceIn(targetUnits + 2, 360)

        // Filter candidates reasonably sized for this target (or keep shortest if all exceed)
        val filtered = candidates.filter { it.estimatedMinutes <= targetMinutes * 1.35 }
            .ifEmpty { candidates.sortedBy { it.estimatedMinutes }.take(5) }

        val dpScore = DoubleArray(maxUnits + 1) { Double.POSITIVE_INFINITY }
        val dpCount = IntArray(maxUnits + 1) { Int.MAX_VALUE }
        val parentUnit = IntArray(maxUnits + 1) { -1 }
        val parentItem = IntArray(maxUnits + 1) { -1 }
        dpScore[0] = 0.0
        dpCount[0] = 0

        // Limit subset search window to keep execution under a few milliseconds on main/IO thread
        val window = filtered.take(64)

        for (idx in window.indices) {
            val item = window[idx]
            val units = (item.estimatedMinutes / stepMin).roundToInt().coerceAtLeast(1)
            if (units > maxUnits) continue
            // Small penalty per added Surah to avoid picking 25 tiny Surahs when 2-4 medium Surahs fit well
            val itemPenalty = if (item.surah.isCompleted) 0.35 else 0.18

            for (u in maxUnits downTo units) {
                val prev = u - units
                if (dpScore[prev].isFinite()) {
                    val newCount = dpCount[prev] + 1
                    if (newCount > 8) continue
                    val newPenalty = dpScore[prev] + itemPenalty
                    val existingPenalty = dpScore[u]
                    if (newPenalty < existingPenalty - 1e-6) {
                        dpScore[u] = newPenalty
                        dpCount[u] = newCount
                        parentUnit[u] = prev
                        parentItem[u] = idx
                    }
                }
            }
        }

        var bestUnit = -1
        var bestObjective = Double.POSITIVE_INFINITY
        for (u in 1..maxUnits) {
            if (!dpScore[u].isFinite()) continue
            val durationMin = u * stepMin
            val objective = abs(durationMin - targetMinutes) + dpScore[u]
            if (objective < bestObjective) {
                bestObjective = objective
                bestUnit = u
            }
        }

        if (bestUnit <= 0) {
            return listOfNotNull(bestSingle)
        }

        val reconstructed = mutableListOf<SurahDurationEstimate>()
        var cursor = bestUnit
        val usedIndices = mutableSetOf<Int>()
        while (cursor > 0) {
            val itemIdx = parentItem[cursor]
            val prev = parentUnit[cursor]
            if (itemIdx < 0 || prev < 0 || !usedIndices.add(itemIdx)) break
            reconstructed.add(window[itemIdx])
            cursor = prev
        }

        if (reconstructed.isEmpty()) {
            return listOfNotNull(bestSingle)
        }

        // Compare final subset against bestSingle on pure time closeness + cleanliness
        val subsetError = abs(reconstructed.sumOf { it.estimatedMinutes } - targetMinutes)
        val singleError = bestSingle?.let { abs(it.estimatedMinutes - targetMinutes) } ?: Double.POSITIVE_INFINITY
        return if (bestSingle != null && singleError <= subsetError + 0.5 && variantIndex % 3 == 0) {
            listOf(bestSingle)
        } else {
            reconstructed
        }
    }
}
