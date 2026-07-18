package com.vineethraj.cattranslator.mood

import com.vineethraj.cattranslator.data.HistoryEntry
import com.vineethraj.cattranslator.data.TranslationDirection

data class MoodCount(val mood: CatMood, val count: Int)

/** Aggregates cat-mood detections from history for the dashboard trend card. */
object MoodTrends {

    const val WINDOW_MS: Long = 7L * 24 * 60 * 60 * 1000

    /**
     * Counts cat->human mood detections in the last 7 days before [nowMs], most frequent first.
     * UNKNOWN results are excluded - "we couldn't tell" isn't a mood trend.
     */
    fun aggregate(entries: List<HistoryEntry>, nowMs: Long): List<MoodCount> {
        val cutoff = nowMs - WINDOW_MS
        return entries
            .asSequence()
            .filter { it.direction == TranslationDirection.CAT_TO_HUMAN }
            .filter { it.timestampMs in cutoff..nowMs }
            .mapNotNull { entry -> runCatching { CatMood.valueOf(entry.label) }.getOrNull() }
            .filter { it != CatMood.UNKNOWN }
            .groupingBy { it }
            .eachCount()
            .map { (mood, count) -> MoodCount(mood, count) }
            .sortedByDescending { it.count }
    }
}
