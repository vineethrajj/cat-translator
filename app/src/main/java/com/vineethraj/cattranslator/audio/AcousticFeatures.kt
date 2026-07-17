package com.vineethraj.cattranslator.audio

import kotlin.math.sqrt

/** Acoustic properties of a recorded clip, used to layer mood/intent on top of a raw sound-type label. */
data class AcousticFeatures(
    val durationSeconds: Float,
    val rmsEnergy: Float,
    val zeroCrossingRate: Float,
    val pitchHz: Float,
    val pulseCount: Int,
)

object AcousticFeatureExtractor {

    fun extract(samples: FloatArray, sampleRateHz: Int): AcousticFeatures {
        if (samples.isEmpty()) {
            return AcousticFeatures(0f, 0f, 0f, 0f, 0)
        }
        return AcousticFeatures(
            durationSeconds = samples.size / sampleRateHz.toFloat(),
            rmsEnergy = rms(samples),
            zeroCrossingRate = zeroCrossingRate(samples),
            pitchHz = estimatePitchHz(samples, sampleRateHz),
            pulseCount = countPulses(samples, sampleRateHz),
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

    /** Counts distinct energy-envelope pulses above a relative threshold, approximating meow repetition count. */
    private fun countPulses(samples: FloatArray, sampleRateHz: Int): Int {
        val frameSize = (sampleRateHz * 0.02).toInt().coerceAtLeast(1)
        val frameCount = samples.size / frameSize
        if (frameCount == 0) return 0

        val envelope = FloatArray(frameCount)
        for (f in 0 until frameCount) {
            var sumSq = 0.0
            val start = f * frameSize
            for (i in start until start + frameSize) sumSq += samples[i] * samples[i]
            envelope[f] = sqrt(sumSq / frameSize).toFloat()
        }

        val peakEnergy = envelope.maxOrNull() ?: 0f
        if (peakEnergy <= 0f) return 0
        val threshold = peakEnergy * 0.3f

        var pulses = 0
        var above = false
        for (e in envelope) {
            if (e >= threshold && !above) {
                pulses++
                above = true
            } else if (e < threshold) {
                above = false
            }
        }
        return pulses
    }
}
