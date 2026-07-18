package com.vineethraj.cattranslator.mood

import com.vineethraj.cattranslator.audio.AcousticFeatures
import com.vineethraj.cattranslator.ml.SoundLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodEngineTest {

    private fun features(
        durationSeconds: Float = 3.0f,
        voicedSeconds: Float = 0.4f,
        rmsEnergy: Float = 0.1f,
        zeroCrossingRate: Float = 0.1f,
        pitchHz: Float = 400f,
        pulseCount: Int = 1,
    ) = AcousticFeatures(durationSeconds, voicedSeconds, rmsEnergy, zeroCrossingRate, pitchHz, pulseCount)

    @Test
    fun `purr maps to content`() {
        val result = MoodEngine.infer(listOf(SoundLabel("Purr", 0.8f)), features())
        assertEquals(CatMood.CONTENT, result.mood)
    }

    @Test
    fun `growling and hiss map to annoyed`() {
        assertEquals(CatMood.ANNOYED, MoodEngine.infer(listOf(SoundLabel("Growling", 0.5f)), features()).mood)
        assertEquals(CatMood.ANNOYED, MoodEngine.infer(listOf(SoundLabel("Hiss", 0.5f)), features()).mood)
    }

    @Test
    fun `caterwaul maps to distressed`() {
        val result = MoodEngine.infer(listOf(SoundLabel("Caterwaul", 0.5f)), features())
        assertEquals(CatMood.DISTRESSED, result.mood)
    }

    @Test
    fun `repeated meow maps to hungry`() {
        val result = MoodEngine.infer(
            listOf(SoundLabel("Meow", 0.5f)),
            features(pulseCount = 3),
        )
        assertEquals(CatMood.HUNGRY, result.mood)
    }

    @Test
    fun `long voiced mid-pitch meow maps to greeting`() {
        val result = MoodEngine.infer(
            listOf(SoundLabel("Meow", 0.5f)),
            features(voicedSeconds = 1.2f, pitchHz = 400f, pulseCount = 1),
        )
        assertEquals(CatMood.GREETING, result.mood)
    }

    @Test
    fun `short meow in a long clip is NOT mistaken for greeting`() {
        // The recorder always captures ~3s; a 0.3s meow inside it must not read as "long".
        val result = MoodEngine.infer(
            listOf(SoundLabel("Meow", 0.5f)),
            features(durationSeconds = 3.0f, voicedSeconds = 0.3f, pitchHz = 400f, pulseCount = 1),
        )
        assertEquals(CatMood.PLAYFUL, result.mood)
    }

    @Test
    fun `high pitched short meow maps to wants attention`() {
        val result = MoodEngine.infer(
            listOf(SoundLabel("Meow", 0.5f)),
            features(voicedSeconds = 0.3f, pitchHz = 700f, pulseCount = 0),
        )
        assertEquals(CatMood.WANTS_ATTENTION, result.mood)
    }

    @Test
    fun `short low-pitch meow with a couple pulses maps to playful`() {
        val result = MoodEngine.infer(
            listOf(SoundLabel("Meow", 0.5f)),
            features(voicedSeconds = 0.3f, pitchHz = 300f, pulseCount = 2),
        )
        assertEquals(CatMood.PLAYFUL, result.mood)
    }

    @Test
    fun `no confident label maps to unknown with zero confidence`() {
        val result = MoodEngine.infer(emptyList(), features())
        assertEquals(CatMood.UNKNOWN, result.mood)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun `unrelated sound label maps to unknown`() {
        val result = MoodEngine.infer(listOf(SoundLabel("Dog", 0.9f)), features())
        assertEquals(CatMood.UNKNOWN, result.mood)
    }
}
