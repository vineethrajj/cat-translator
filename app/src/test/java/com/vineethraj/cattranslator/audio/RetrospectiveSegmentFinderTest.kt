package com.vineethraj.cattranslator.audio

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrospectiveSegmentFinderTest {

    private val sampleRateHz = 16_000

    private fun silence(durationMs: Int): FloatArray = FloatArray(sampleRateHz * durationMs / 1000)

    private fun tone(durationMs: Int, freqHz: Float, amplitude: Float): FloatArray {
        val n = sampleRateHz * durationMs / 1000
        return FloatArray(n) { i -> (amplitude * sin(2.0 * PI * freqHz * i / sampleRateHz)).toFloat() }
    }

    @Test
    fun `empty buffer returns null`() {
        assertNull(RetrospectiveSegmentFinder.findBestSegment(FloatArray(0), sampleRateHz))
    }

    @Test
    fun `pure silence returns null`() {
        val buffered = silence(3000)
        assertNull(RetrospectiveSegmentFinder.findBestSegment(buffered, sampleRateHz))
    }

    @Test
    fun `a quiet meow the live threshold would miss is still found by the lenient pass`() {
        // Amplitude chosen so its RMS sits below the live VAD's default threshold (0.02) but
        // above the lenient retrospective default (0.008).
        val quietAmplitude = 0.018f
        val buffered = silence(500) + tone(300, 500f, quietAmplitude) + silence(500)

        val liveDetector = VoiceActivityDetector(sampleRateHz = sampleRateHz) // default (strict) threshold
        var liveFound = false
        var offset = 0
        val chunk = sampleRateHz * 20 / 1000
        while (offset < buffered.size) {
            val end = (offset + chunk).coerceAtMost(buffered.size)
            if (liveDetector.accept(buffered.copyOfRange(offset, end)) != null) liveFound = true
            offset = end
        }
        if (liveDetector.flush() != null) liveFound = true
        assertTrue("test setup assumption failed: live threshold caught the quiet tone too", !liveFound)

        val found = RetrospectiveSegmentFinder.findBestSegment(buffered, sampleRateHz)
        assertNotNull("expected the lenient pass to find the quiet tone", found)
    }

    @Test
    fun `returns the most recent candidate when several sounds are in the buffer`() {
        val buffered = silence(300) +
            tone(300, 500f, 0.4f) + // earlier, shorter burst
            silence(2000) + // well past the hangover window - these are separate segments
            tone(700, 500f, 0.4f) + // later, longer burst
            silence(500)

        val found = RetrospectiveSegmentFinder.findBestSegment(buffered, sampleRateHz)

        assertNotNull(found)
        val durationMs = found!!.size * 1000.0 / sampleRateHz
        assertTrue(
            "expected the longer, later burst (~700ms) but got ${durationMs}ms",
            durationMs > 500.0,
        )
    }
}
