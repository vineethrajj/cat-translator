package com.vineethraj.cattranslator.audio

/**
 * Fixed-capacity circular buffer of the most recent audio samples, kept alongside the live
 * [VoiceActivityDetector] so a "nothing detected automatically" session can still fall back to
 * analyzing the last few seconds of raw audio (see [RetrospectiveSegmentFinder]).
 *
 * Not thread-safe - all access must come from a single caller at a time (in practice: writes
 * happen on the audio-read loop, and a snapshot is only taken after that loop has fully stopped).
 */
class RollingAudioBuffer(private val maxSamples: Int) {

    private val buffer = FloatArray(maxSamples)
    private var writePos = 0
    private var filled = 0

    fun write(chunk: FloatArray) {
        for (sample in chunk) {
            buffer[writePos] = sample
            writePos = (writePos + 1) % maxSamples
            if (filled < maxSamples) filled++
        }
    }

    /** Returns the buffered audio in chronological order (oldest first). */
    fun snapshot(): FloatArray {
        if (filled < maxSamples) {
            return buffer.copyOfRange(0, filled)
        }
        val result = FloatArray(maxSamples)
        val tailLength = maxSamples - writePos
        System.arraycopy(buffer, writePos, result, 0, tailLength)
        System.arraycopy(buffer, 0, result, tailLength, writePos)
        return result
    }

    fun clear() {
        writePos = 0
        filled = 0
    }
}
