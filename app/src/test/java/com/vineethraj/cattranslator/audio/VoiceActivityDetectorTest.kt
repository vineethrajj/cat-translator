package com.vineethraj.cattranslator.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceActivityDetectorTest {

    private val sampleRateHz = 16_000
    private val chunkMs = 20

    private fun chunk(loud: Boolean): FloatArray {
        val n = sampleRateHz * chunkMs / 1000
        val amplitude = if (loud) 0.5f else 0f
        // Alternate sign so RMS is nonzero for "loud" chunks without needing a real waveform.
        return FloatArray(n) { i -> if (i % 2 == 0) amplitude else -amplitude }
    }

    private fun newDetector(
        onsetMs: Int = 100,
        hangoverMs: Int = 700,
        minSegmentMs: Int = 150,
        maxSegmentMs: Int = 8000,
    ) = VoiceActivityDetector(
        sampleRateHz = sampleRateHz,
        onsetMs = onsetMs,
        hangoverMs = hangoverMs,
        minSegmentMs = minSegmentMs,
        maxSegmentMs = maxSegmentMs,
    )

    private fun feed(detector: VoiceActivityDetector, loudMsRuns: List<Pair<Boolean, Int>>): List<FloatArray> {
        val segments = mutableListOf<FloatArray>()
        for ((loud, durationMs) in loudMsRuns) {
            var remaining = durationMs
            while (remaining > 0) {
                detector.accept(chunk(loud))?.let { segments.add(it) }
                remaining -= chunkMs
            }
        }
        return segments
    }

    @Test
    fun `pure silence emits nothing`() {
        val detector = newDetector()
        val segments = feed(detector, listOf(false to 1000))
        assertTrue(segments.isEmpty())
    }

    @Test
    fun `a single clean burst emits exactly one segment`() {
        val detector = newDetector()
        val segments = feed(
            detector,
            listOf(false to 200, true to 300, false to 800),
        )
        assertEquals(1, segments.size)
        val durationMs = segments[0].size * 1000.0 / sampleRateHz
        // Voiced portion (~300ms) plus onset ramp-up, minus the trimmed trailing hangover.
        assertTrue("expected ~300ms segment but got ${durationMs}ms", durationMs in 250.0..400.0)
    }

    @Test
    fun `a click shorter than the onset threshold is discarded`() {
        val detector = newDetector(onsetMs = 100)
        val segments = feed(
            detector,
            listOf(false to 200, true to 40, false to 500),
        )
        assertTrue(segments.isEmpty())
    }

    @Test
    fun `two bursts with a short gap merge into one segment`() {
        val detector = newDetector(hangoverMs = 700)
        val segments = feed(
            detector,
            listOf(false to 200, true to 200, false to 300, true to 200, false to 800),
        )
        assertEquals(1, segments.size)
        val durationMs = segments[0].size * 1000.0 / sampleRateHz
        // Should span from the first burst's start through the second burst's end (minus trim),
        // i.e. noticeably longer than either burst alone.
        assertTrue("expected a merged segment >400ms but got ${durationMs}ms", durationMs > 400.0)
    }

    @Test
    fun `two bursts with a long gap produce two separate segments`() {
        val detector = newDetector(hangoverMs = 300)
        val segments = feed(
            detector,
            listOf(false to 200, true to 200, false to 500, true to 200, false to 500),
        )
        assertEquals(2, segments.size)
    }

    @Test
    fun `continuous sound is force-cut at the max segment cap`() {
        val detector = newDetector(maxSegmentMs = 1000)
        val segments = feed(detector, listOf(true to 2500))
        assertTrue("expected at least one forced cut, got ${segments.size}", segments.isNotEmpty())
        for (segment in segments) {
            val durationMs = segment.size * 1000.0 / sampleRateHz
            assertTrue("segment $durationMs ms exceeds cap", durationMs <= 1000.0 + chunkMs)
        }
    }

    @Test
    fun `a burst right at the minimum length is not dropped`() {
        val detector = newDetector(onsetMs = 100, minSegmentMs = 100, hangoverMs = 200)
        val segments = feed(detector, listOf(false to 200, true to 120, false to 500))
        assertEquals(1, segments.size)
    }

    @Test
    fun `flush salvages an in-progress sound when the session stops mid-meow`() {
        val detector = newDetector()
        feed(detector, listOf(false to 200, true to 300))
        val flushed = detector.flush()
        assertTrue(flushed != null && flushed.isNotEmpty())
    }

    @Test
    fun `flush during silence returns null`() {
        val detector = newDetector()
        feed(detector, listOf(false to 500))
        assertNull(detector.flush())
    }

    @Test
    fun `detector is reusable for a new segment after finishing one`() {
        val detector = newDetector(hangoverMs = 300)
        val first = feed(detector, listOf(false to 200, true to 200, false to 500))
        val second = feed(detector, listOf(true to 200, false to 500))
        assertEquals(1, first.size)
        assertEquals(1, second.size)
    }
}
