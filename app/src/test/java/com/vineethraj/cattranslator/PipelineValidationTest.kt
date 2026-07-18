package com.vineethraj.cattranslator

import com.vineethraj.cattranslator.audio.AcousticFeatureExtractor
import com.vineethraj.cattranslator.mood.CatMood
import com.vineethraj.cattranslator.mood.MoodEngine
import com.vineethraj.cattranslator.ml.SoundLabel
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end validation of the translation pipeline using synthesized cat-sound stand-ins:
 * realistic 3-second "recordings" (matching the app's fixed recording window) are pushed through
 * the real [AcousticFeatureExtractor] and [MoodEngine].
 *
 * The one stage that cannot run here is YAMNet itself (Android-only native library), so each
 * scenario injects the label YAMNet would produce for that sound type. Everything downstream of
 * the model - feature math and the mood decision table - is exercised for real.
 */
class PipelineValidationTest {

    private val sampleRateHz = 16_000
    private val clipSeconds = 3.0f

    /** A meow-like burst: pitch glide with vibrato and an attack/release envelope. */
    private fun meowBurst(durationSeconds: Float, startHz: Float, endHz: Float): FloatArray {
        val n = (sampleRateHz * durationSeconds).toInt()
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i / n.toFloat()
            val freq = startHz + (endHz - startHz) * t
            val vibrato = 1f + 0.04f * sin(2.0 * PI * 8.0 * i / sampleRateHz).toFloat()
            phase += 2.0 * PI * freq * vibrato / sampleRateHz
            val envelope = when {
                t < 0.1f -> t / 0.1f
                t > 0.8f -> (1f - t) / 0.2f
                else -> 1f
            }
            out[i] = (sin(phase) * envelope * 0.8).toFloat()
        }
        return out
    }

    /** A purr-like sound: low-frequency amplitude-modulated rumble. */
    private fun purr(durationSeconds: Float): FloatArray {
        val n = (sampleRateHz * durationSeconds).toInt()
        return FloatArray(n) { i ->
            val carrier = sin(2.0 * PI * 220.0 * i / sampleRateHz)
            val flutter = 0.5 + 0.5 * sin(2.0 * PI * 26.0 * i / sampleRateHz)
            (carrier * flutter * 0.3).toFloat()
        }
    }

    /** A hiss-like sound: broadband noise. */
    private fun hiss(durationSeconds: Float): FloatArray {
        val random = Random(42)
        val n = (sampleRateHz * durationSeconds).toInt()
        return FloatArray(n) { (random.nextFloat() * 2f - 1f) * 0.5f }
    }

    /** Embeds bursts (with gaps) into a fixed-length "recording", like the app's 3s window. */
    private fun recordingWith(vararg bursts: FloatArray, gapSeconds: Float = 0.25f): FloatArray {
        val clip = FloatArray((sampleRateHz * clipSeconds).toInt())
        var offset = sampleRateHz / 2
        for (burst in bursts) {
            val end = (offset + burst.size).coerceAtMost(clip.size)
            burst.copyInto(clip, offset, 0, end - offset)
            offset = end + (gapSeconds * sampleRateHz).toInt()
            if (offset >= clip.size) break
        }
        return clip
    }

    private fun translate(clip: FloatArray, yamnetLabel: String): CatMood {
        val features = AcousticFeatureExtractor.extract(clip, sampleRateHz)
        return MoodEngine.infer(listOf(SoundLabel(yamnetLabel, 0.7f)), features).mood
    }

    @Test
    fun `triple short meow reads as hungry`() {
        val burst = meowBurst(0.25f, 620f, 380f)
        val clip = recordingWith(burst, burst, burst)
        assertEquals(CatMood.HUNGRY, translate(clip, "Meow"))
    }

    @Test
    fun `single long mid-pitch meow reads as greeting`() {
        val clip = recordingWith(meowBurst(1.4f, 420f, 380f))
        assertEquals(CatMood.GREETING, translate(clip, "Meow"))
    }

    @Test
    fun `single short high meow reads as wants attention`() {
        val clip = recordingWith(meowBurst(0.35f, 900f, 750f))
        assertEquals(CatMood.WANTS_ATTENTION, translate(clip, "Meow"))
    }

    @Test
    fun `single short low meow reads as playful`() {
        val clip = recordingWith(meowBurst(0.3f, 380f, 300f))
        assertEquals(CatMood.PLAYFUL, translate(clip, "Meow"))
    }

    @Test
    fun `purr reads as content regardless of features`() {
        val clip = recordingWith(purr(2.0f))
        assertEquals(CatMood.CONTENT, translate(clip, "Purr"))
    }

    @Test
    fun `hiss reads as annoyed`() {
        val clip = recordingWith(hiss(0.8f))
        assertEquals(CatMood.ANNOYED, translate(clip, "Hiss"))
    }

    @Test
    fun `caterwaul reads as distressed`() {
        val clip = recordingWith(meowBurst(2.0f, 800f, 400f))
        assertEquals(CatMood.DISTRESSED, translate(clip, "Caterwaul"))
    }

    @Test
    fun `non-cat sound reads as unknown even with strong features`() {
        val clip = recordingWith(meowBurst(0.5f, 500f, 400f))
        assertEquals(CatMood.UNKNOWN, translate(clip, "Speech"))
    }

    @Test
    fun `silence with low-confidence label reads as unknown`() {
        val clip = FloatArray((sampleRateHz * clipSeconds).toInt())
        val features = AcousticFeatureExtractor.extract(clip, sampleRateHz)
        val mood = MoodEngine.infer(listOf(SoundLabel("Meow", 0.05f)), features).mood
        assertEquals(CatMood.UNKNOWN, mood)
    }

    @Test
    fun `feature extraction stays sane on a realistic meow`() {
        val clip = recordingWith(meowBurst(0.4f, 600f, 400f))
        val features = AcousticFeatureExtractor.extract(clip, sampleRateHz)

        assertEquals(clipSeconds, features.durationSeconds, 0.05f)
        assertTrue("voiced ${features.voicedSeconds}", features.voicedSeconds in 0.2f..0.7f)
        assertTrue("pitch ${features.pitchHz}", features.pitchHz in 250f..900f)
        assertEquals(1, features.pulseCount)
    }
}
