package com.vineethraj.cattranslator.audio

import kotlin.math.sqrt

/**
 * Detects the start and end of sound bursts in a continuous stream of small PCM chunks, so a
 * "listening" session can react only to actual sounds instead of blindly recording a fixed
 * window. Feed consecutive chunks via [accept]; when it returns a non-null segment, that's a
 * complete burst ready to be classified. Deciding *what* the sound is (cat vs. not) is left to
 * the existing classifier - this only finds *when* a sound happened.
 *
 * Not thread-safe - call [accept] from a single reader loop.
 */
class VoiceActivityDetector(
    private val sampleRateHz: Int,
    private val onsetMs: Int = 100,
    private val hangoverMs: Int = 700,
    private val minSegmentMs: Int = 150,
    private val maxSegmentMs: Int = 8000,
    private val energyThreshold: Float = 0.02f,
) {
    private enum class State { SILENCE, ONSET_PENDING, VOICED, HANGOVER }

    private var state = State.SILENCE
    private val buffer = ArrayList<Float>()
    private var aboveMsAccum = 0.0
    private var belowMsAccum = 0.0

    /** Feed the next chunk of samples. Returns a finished segment if one just completed. */
    fun accept(chunk: FloatArray): FloatArray? {
        if (chunk.isEmpty()) return null
        val chunkMs = chunk.size * 1000.0 / sampleRateHz
        val isLoud = rms(chunk) >= energyThreshold

        when (state) {
            State.SILENCE -> {
                if (isLoud) {
                    state = State.ONSET_PENDING
                    aboveMsAccum = chunkMs
                    buffer.clear()
                    appendChunk(chunk)
                }
                return null
            }

            State.ONSET_PENDING -> {
                if (isLoud) {
                    aboveMsAccum += chunkMs
                    appendChunk(chunk)
                    if (aboveMsAccum >= onsetMs) state = State.VOICED
                } else {
                    // Too short to be a real sound (a click/pop) - discard and reset.
                    reset()
                }
                return null
            }

            State.VOICED -> {
                appendChunk(chunk)
                if (!isLoud) {
                    state = State.HANGOVER
                    belowMsAccum = chunkMs
                }
                return finishIfOverCap()
            }

            State.HANGOVER -> {
                if (isLoud) {
                    // Sound resumed inside the hangover window - still one continuous segment
                    // (this is what lets rapid repeated meows land in a single clip).
                    appendChunk(chunk)
                    state = State.VOICED
                    belowMsAccum = 0.0
                    return finishIfOverCap()
                }

                // Don't append this chunk yet: if it's the one that completes the hangover, the
                // buffer must still reflect only the *previously appended* silence so the trim
                // amount below matches what's actually buffered.
                val priorBelowMs = belowMsAccum
                belowMsAccum += chunkMs
                return if (belowMsAccum >= hangoverMs) {
                    finishSegment(trimTrailingSilenceMs = priorBelowMs)
                } else {
                    appendChunk(chunk)
                    finishIfOverCap()
                }
            }
        }
    }

    /** Call when the listening session stops, to salvage any in-progress sound. */
    fun flush(): FloatArray? {
        val result = if (state == State.VOICED || state == State.HANGOVER) {
            finishSegment(trimTrailingSilenceMs = 0.0)
        } else {
            null
        }
        reset()
        return result
    }

    private fun finishIfOverCap(): FloatArray? {
        val durationMs = buffer.size * 1000.0 / sampleRateHz
        return if (durationMs >= maxSegmentMs) finishSegment(trimTrailingSilenceMs = 0.0) else null
    }

    private fun finishSegment(trimTrailingSilenceMs: Double): FloatArray? {
        val trimSamples = (trimTrailingSilenceMs / 1000.0 * sampleRateHz).toInt().coerceIn(0, buffer.size)
        val segmentSize = buffer.size - trimSamples
        val segment = if (segmentSize > 0) buffer.subList(0, segmentSize).toFloatArray() else FloatArray(0)
        reset()

        val segmentMs = segment.size * 1000.0 / sampleRateHz
        return if (segmentMs >= minSegmentMs) segment else null
    }

    private fun appendChunk(chunk: FloatArray) {
        for (sample in chunk) buffer.add(sample)
    }

    private fun reset() {
        state = State.SILENCE
        buffer.clear()
        aboveMsAccum = 0.0
        belowMsAccum = 0.0
    }

    private fun rms(samples: FloatArray): Float {
        var sumSq = 0.0
        for (s in samples) sumSq += s * s
        return sqrt(sumSq / samples.size).toFloat()
    }
}
