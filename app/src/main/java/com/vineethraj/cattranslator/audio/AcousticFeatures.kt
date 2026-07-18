package com.vineethraj.cattranslator.audio

import kotlin.math.sqrt

/** Acoustic properties of a recorded clip, used to layer mood/intent on top of a raw sound-type label. */
data class AcousticFeatures(
    /** Total clip length. With a fixed-length recorder this is ~constant - don't infer mood from it. */
    val durationSeconds: Float,
    /** How long the sound was actually voiced (frames above an energy threshold). */
    val voicedSeconds: Float,
    val rmsEnergy: Float,
    val zeroCrossingRate: Float,
    val pitchHz: Float,
    val pulseCount: Int,
)

object AcousticFeatureExtractor {

    private const val FRAME_SECONDS = 0.02
    private const val PULSE_THRESHOLD_RATIO = 0.3f

    fun extract(samples: FloatArray, sampleRateHz: Int): AcousticFeatures {
        if (samples.isEmpty()) {
            return AcousticFeatures(0f, 0f, 0f, 0f, 0f, 0)
        }
        val envelope = energyEnvelope(samples, sampleRateHz)
        val (pulses, voicedFrames) = pulsesAndVoicedFrames(envelope)
        return AcousticFeatures(
            durationSeconds = samples.size / sampleRateHz.toFloat(),
            voicedSeconds = (voicedFrames * FRAME_SECONDS).toFloat(),
            rmsEnergy = rms(samples),
            zeroCrossingRate = zeroCrossingRate(samples),
            pitchHz = estimatePitchHz(samples, sampleRateHz),
            pulseCount = pulses,
        )
    }

    private fun rms(samples: FloatArray): Float {
        var sumSq = 0.0
        for (s in samples) sumSq += s * s
        return sqrt(sumSq / samples.size).toFloat()
    }

    private fun zeroCrossingRate(samples: FloatArray): Float {
        if (samples.size < 2) return 0f
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i - 1] >= 0f) != (samples[i] >= 0f)) crossings++
        }
        return crossings / (samples.size - 1).toFloat()
    }

    /**
     * Autocorrelation-based pitch estimate over the loudest ~40ms window, restricted to the
     * plausible cat vocalization range (~200Hz-2000Hz) to avoid locking onto DC drift or noise.
     */
    private fun estimatePitchHz(samples: FloatArray, sampleRateHz: Int): Float {
        val windowSize = (sampleRateHz * 0.04).toInt().coerceAtMost(samples.size)
        if (windowSize < 32) return 0f
        val window = loudestWindow(samples, windowSize)

        val minLag = (sampleRateHz / 2000.0).toInt().coerceAtLeast(1)
        val maxLag = (sampleRateHz / 200.0).toInt().coerceAtMost(windowSize - 1)
        if (maxLag <= minLag) return 0f

        var bestLag = -1
        var bestCorrelation = 0f
        for (lag in minLag..maxLag) {
            var correlation = 0f
            for (i in 0 until windowSize - lag) {
                correlation += window[i] * window[i + lag]
            }
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestLag = lag
            }
        }
        if (bestLag <= 0) return 0f
        return sampleRateHz / bestLag.toFloat()
    }

    private fun loudestWindow(samples: FloatArray, windowSize: Int): FloatArray {
        var bestStart = 0
        var bestEnergy = -1.0
        var start = 0
        val step = (windowSize / 2).coerceAtLeast(1)
        while (start + windowSize <= samples.size) {
            var energy = 0.0
            for (i in start until start + windowSize) energy += samples[i] * samples[i]
            if (energy > bestEnergy) {
                bestEnergy = energy
                bestStart = start
            }
            start += step
        }
        return samples.copyOfRange(bestStart, bestStart + windowSize)
    }

    /** Per-frame (~20ms) RMS energy envelope. */
    private fun energyEnvelope(samples: FloatArray, sampleRateHz: Int): FloatArray {
        val frameSize = (sampleRateHz * FRAME_SECONDS).toInt().coerceAtLeast(1)
        val frameCount = samples.size / frameSize
        val envelope = FloatArray(frameCount)
        for (f in 0 until frameCount) {
            var sumSq = 0.0
            val start = f * frameSize
            for (i in start until start + frameSize) sumSq += samples[i] * samples[i]
            envelope[f] = sqrt(sumSq / frameSize).toFloat()
        }
        return envelope
    }

    /**
     * Counts distinct pulses above a relative threshold (approximating meow repetitions) and how
     * many frames were voiced at all - the latter gives the true vocalization length, independent
     * of the fixed recording window.
     */
    private fun pulsesAndVoicedFrames(envelope: FloatArray): Pair<Int, Int> {
        val peakEnergy = envelope.maxOrNull() ?: 0f
        if (peakEnergy <= 0f) return 0 to 0
        val threshold = peakEnergy * PULSE_THRESHOLD_RATIO

        var pulses = 0
        var voicedFrames = 0
        var above = false
        for (e in envelope) {
            if (e >= threshold) {
                voicedFrames++
                if (!above) {
                    pulses++
                    above = true
                }
            } else {
                above = false
            }
        }
        return pulses to voicedFrames
    }
}
