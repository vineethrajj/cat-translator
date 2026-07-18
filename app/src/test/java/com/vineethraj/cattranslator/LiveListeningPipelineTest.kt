package com.vineethraj.cattranslator

import com.vineethraj.cattranslator.audio.AcousticFeatureExtractor
import com.vineethraj.cattranslator.audio.VoiceActivityDetector
import com.vineethraj.cattranslator.ml.SoundLabel
import com.vineethraj.cattranslator.mood.CatMood
import com.vineethraj.cattranslator.mood.MoodEngine
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the full "listen continuously, react only to cat sounds" pipeline: raw audio chunks
 * -> [VoiceActivityDetector] -> (simulated classifier label) -> [MoodEngine] - the same chain
 * [com.vineethraj.cattranslator.viewmodel.CatToHumanViewModel] drives during a live session. The
 * one stage that can't run in a JVM test is the real YAMNet classifier (Android-only native
 * library), so each produced segment is paired with the label the classifier would plausibly
 * emit for that kind of sound.
 */
class LiveListeningPipelineTest {

    private val sampleRateHz = 16_000
    private val chunkMs = 20
    private val chunkSamples = sampleRateHz * chunkMs / 1000

    private fun silenceChunks(durationMs: Int): List<FloatArray> =
        List(durationMs / chunkMs) { FloatArray(chunkSamples) }

    /** Broadband noise, reused both as quiet room tone (low amplitude) and a loud non-cat burst. */
    private fun noiseChunks(durationMs: Int, amplitude: Float, seed: Int): List<FloatArray> {
        val random = Random(seed)
        return List(durationMs / chunkMs) {
            FloatArray(chunkSamples) { (random.nextFloat() * 2f - 1f) * amplitude }
        }
    }

    private fun meowChunks(durationMs: Int, startHz: Float, endHz: Float): List<FloatArray> {
        val totalSamples = sampleRateHz * durationMs / 1000
        var phase = 0.0
        val chunks = mutableListOf<FloatArray>()
        var sampleIndex = 0
        while (sampleIndex < totalSamples) {
            val remaining = (totalSamples - sampleIndex).coerceAtMost(chunkSamples)
            val chunk = FloatArray(remaining)
            for (i in 0 until remaining) {
                val t = (sampleIndex + i) / totalSamples.toFloat()
                val freq = startHz + (endHz - startHz) * t
                phase += 2.0 * PI * freq / sampleRateHz
                val envelope = when {
                    t < 0.1f -> t / 0.1f
                    t > 0.85f -> (1f - t) / 0.15f
                    else -> 1f
                }
                chunk[i] = (sin(phase) * envelope * 0.8).toFloat()
            }
            chunks.add(chunk)
            sampleIndex += remaining
        }
        return chunks
    }

    private fun feedAll(detector: VoiceActivityDetector, chunkGroups: List<List<FloatArray>>): List<FloatArray> {
        val segments = mutableListOf<FloatArray>()
        for (group in chunkGroups) {
            for (chunk in group) {
                detector.accept(chunk)?.let { segments.add(it) }
            }
        }
        return segments
    }

    @Test
    fun `a meow surrounded by quiet background produces one segment recognized as a cat sound`() {
        val detector = VoiceActivityDetector(sampleRateHz = sampleRateHz)
        val segments = feedAll(
            detector,
            listOf(
                noiseChunks(1000, amplitude = 0.005f, seed = 1), // below VAD threshold: room tone
                meowChunks(400, startHz = 600f, endHz = 380f),
                silenceChunks(1000),
            ),
        )

        assertEquals(1, segments.size)

        val features = AcousticFeatureExtractor.extract(segments[0], sampleRateHz)
        val mood = MoodEngine.infer(listOf(SoundLabel("Meow", 0.7f)), features).mood
        assertTrue("expected a recognized mood but got $mood", mood != CatMood.UNKNOWN)
    }

    @Test
    fun `a loud non-cat burst is segmented by VAD but discarded after classification`() {
        val detector = VoiceActivityDetector(sampleRateHz = sampleRateHz)
        val segments = feedAll(
            detector,
            listOf(
                silenceChunks(500),
                noiseChunks(600, amplitude = 0.4f, seed = 2), // loud "human speech"-like burst
                silenceChunks(1000),
            ),
        )

        assertEquals(1, segments.size) // VAD reacts to any loud sound...

        val features = AcousticFeatureExtractor.extract(segments[0], sampleRateHz)
        // ...but the classifier says it's not a cat sound, so the mood engine must discard it.
        val mood = MoodEngine.infer(listOf(SoundLabel("Speech", 0.9f)), features).mood
        assertEquals(CatMood.UNKNOWN, mood)
    }

    @Test
    fun `background room noise alone never triggers a segment`() {
        val detector = VoiceActivityDetector(sampleRateHz = sampleRateHz)
        val segments = feedAll(detector, listOf(noiseChunks(3000, amplitude = 0.005f, seed = 3)))
        assertTrue(segments.isEmpty())
    }

    @Test
    fun `two meows in one sitting are captured as a single segment for the hungry heuristic`() {
        val detector = VoiceActivityDetector(sampleRateHz = sampleRateHz)
        val segments = feedAll(
            detector,
            listOf(
                silenceChunks(300),
                meowChunks(250, 600f, 380f),
                silenceChunks(300),
                meowChunks(250, 620f, 380f),
                silenceChunks(250),
                meowChunks(250, 600f, 380f),
                silenceChunks(1000),
            ),
        )

        assertEquals(1, segments.size)
        val features = AcousticFeatureExtractor.extract(segments[0], sampleRateHz)
        val mood = MoodEngine.infer(listOf(SoundLabel("Meow", 0.7f)), features).mood
        assertEquals(CatMood.HUNGRY, mood)
    }

    @Test
    fun `a long gap between two meows produces two separate segments, not one hungry burst`() {
        val detector = VoiceActivityDetector(sampleRateHz = sampleRateHz)
        val segments = feedAll(
            detector,
            listOf(
                silenceChunks(300),
                meowChunks(300, 600f, 380f),
                silenceChunks(2000), // well past the hangover window
                meowChunks(300, 600f, 380f),
                silenceChunks(1000),
            ),
        )

        assertEquals(2, segments.size)
    }
}
