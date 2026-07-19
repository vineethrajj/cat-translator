package com.vineethraj.cattranslator.audio

/**
 * A second, more lenient pass over already-recorded audio, used when the live
 * [VoiceActivityDetector] didn't catch anything during a listening session. Feeds a buffered
 * snapshot through a fresh [VoiceActivityDetector] configured with a lower energy floor (to catch
 * a quiet meow the live threshold missed) and returns the **most recent** candidate segment -
 * this only runs right after the user taps Stop, so whatever happened last is the most likely
 * match for "I just heard it, then I stopped."
 */
object RetrospectiveSegmentFinder {

    const val DEFAULT_LENIENT_ENERGY_THRESHOLD = 0.008f
    private const val SCAN_CHUNK_MS = 20

    fun findBestSegment(
        buffered: FloatArray,
        sampleRateHz: Int,
        energyThreshold: Float = DEFAULT_LENIENT_ENERGY_THRESHOLD,
    ): FloatArray? {
        if (buffered.isEmpty()) return null

        val detector = VoiceActivityDetector(
            sampleRateHz = sampleRateHz,
            energyThreshold = energyThreshold,
        )

        val segments = mutableListOf<FloatArray>()
        val chunkSamples = (sampleRateHz * SCAN_CHUNK_MS / 1000).coerceAtLeast(1)
        var offset = 0
        while (offset < buffered.size) {
            val end = (offset + chunkSamples).coerceAtMost(buffered.size)
            detector.accept(buffered.copyOfRange(offset, end))?.let { segments.add(it) }
            offset = end
        }
        detector.flush()?.let { segments.add(it) }

        return segments.lastOrNull()
    }
}
