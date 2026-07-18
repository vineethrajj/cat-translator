package com.vineethraj.cattranslator.mood

import com.vineethraj.cattranslator.data.HistoryEntry
import com.vineethraj.cattranslator.data.TranslationDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodTrendsTest {

    private val now = 10L * MoodTrends.WINDOW_MS

    private fun entry(
        ageMs: Long,
        label: String,
        direction: TranslationDirection = TranslationDirection.CAT_TO_HUMAN,
    ) = HistoryEntry(
        timestampMs = now - ageMs,
        direction = direction,
        label = label,
        text = "x",
    )

    @Test
    fun `counts moods within the window, most frequent first`() {
        val entries = listOf(
            entry(1000, "HUNGRY"),
            entry(2000, "HUNGRY"),
            entry(3000, "CONTENT"),
        )

        val trends = MoodTrends.aggregate(entries, now)

        assertEquals(listOf(CatMood.HUNGRY to 2, CatMood.CONTENT to 1), trends.map { it.mood to it.count })
    }

    @Test
    fun `entries older than the window are excluded`() {
        val entries = listOf(
            entry(1000, "CONTENT"),
            entry(MoodTrends.WINDOW_MS + 1000, "HUNGRY"),
        )

        val trends = MoodTrends.aggregate(entries, now)

        assertEquals(listOf(CatMood.CONTENT), trends.map { it.mood })
    }

    @Test
    fun `human-to-cat entries and unknown moods are excluded`() {
        val entries = listOf(
            entry(1000, "FOOD", direction = TranslationDirection.HUMAN_TO_CAT),
            entry(2000, "UNKNOWN"),
            entry(3000, "PLAYFUL"),
        )

        val trends = MoodTrends.aggregate(entries, now)

        assertEquals(listOf(CatMood.PLAYFUL), trends.map { it.mood })
    }

    @Test
    fun `unparseable labels are skipped without crashing`() {
        val entries = listOf(entry(1000, "NOT_A_MOOD"), entry(2000, "CONTENT"))
        val trends = MoodTrends.aggregate(entries, now)
        assertEquals(listOf(CatMood.CONTENT), trends.map { it.mood })
    }

    @Test
    fun `empty history aggregates to empty`() {
        assertTrue(MoodTrends.aggregate(emptyList(), now).isEmpty())
    }
}
