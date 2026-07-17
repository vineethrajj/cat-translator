package com.vineethraj.cattranslator.audio

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcousticFeaturesTest {

    private val sampleRateHz = 16_000

    private fun sineWave(freqHz: Float, durationSeconds: Float, amplitude: Float = 0.8f): FloatArray {
        val sampleCount = (sampleRateHz * durationSeconds).toInt()
        return FloatArray(sampleCount) { i ->
            (amplitude * sin(2.0 * PI * freqHz * i / sampleRateHz)).toFloat()
        }
    }

    @Test
    fun `extract on empty samples returns zeroed features`() {
        val features = AcousticFeatureExtractor.extract(FloatArray(0), sampleRateHz)

        assertEquals(0f, features.durationSeconds)
        assertEquals(0f, features.rmsEnergy)
        assertEquals(0, features.pulseCount)
    }

    @Test
    fun `duration matches sample count and sample rate`() {
        val samples = sineWave(freqHz = 440f, durationSeconds = 0.5f)
        val features = AcousticFeatureExtractor.extract(samples, sampleRateHz)

        assertEquals(0.5f, features.durationSeconds, 0.01f)
    }

    @Test
    fun `pitch estimate is close to the true tone frequency`() {
        val samples = sineWave(freqHz = 440f, durationSeconds = 0.3f)
        val features = AcousticFeatureExtractor.extract(samples, sampleRateHz)

        assertTrue(
            "expected pitch near 440Hz but got ${features.pitchHz}",
            kotlin.math.abs(features.pitchHz - 440f) < 25f,
        )
    }

    @Test
    fun `silence has near-zero energy`() {
        val samples = FloatArray(sampleRateHz / 2)
        val features = AcousticFeatureExtractor.extract(samples, sampleRateHz)

        assertEquals(0f, features.rmsEnergy, 0.001f)
    }

    @Test
    fun `counts distinct pulses separated by silence`() {
        val burst = sineWave(freqHz = 500f, durationSeconds = 0.15f)
        val silence = FloatArray((sampleRateHz * 0.15f).toInt())
        val samples = burst + silence + burst + silence + burst

        val features = AcousticFeatureExtractor.extract(samples, sampleRateHz)

        assertEquals(3, features.pulseCount)
    }
}
