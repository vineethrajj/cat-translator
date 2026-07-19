package com.vineethraj.cattranslator.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RollingAudioBufferTest {

    @Test
    fun `snapshot before filling returns only what was written, in order`() {
        val buffer = RollingAudioBuffer(maxSamples = 10)
        buffer.write(floatArrayOf(1f, 2f, 3f))

        assertArrayEquals(floatArrayOf(1f, 2f, 3f), buffer.snapshot(), 0f)
    }

    @Test
    fun `snapshot returns empty before any writes`() {
        val buffer = RollingAudioBuffer(maxSamples = 10)
        assertEquals(0, buffer.snapshot().size)
    }

    @Test
    fun `once full, snapshot keeps chronological order after wraparound`() {
        val buffer = RollingAudioBuffer(maxSamples = 5)
        buffer.write(floatArrayOf(1f, 2f, 3f, 4f, 5f))
        buffer.write(floatArrayOf(6f, 7f)) // overwrites the oldest two samples (1, 2)

        assertArrayEquals(floatArrayOf(3f, 4f, 5f, 6f, 7f), buffer.snapshot(), 0f)
    }

    @Test
    fun `writes larger than capacity still leave only the most recent samples`() {
        val buffer = RollingAudioBuffer(maxSamples = 4)
        buffer.write(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f))

        assertArrayEquals(floatArrayOf(4f, 5f, 6f, 7f), buffer.snapshot(), 0f)
    }

    @Test
    fun `clear resets to empty and drops old data`() {
        val buffer = RollingAudioBuffer(maxSamples = 5)
        buffer.write(floatArrayOf(1f, 2f, 3f))
        buffer.clear()

        assertEquals(0, buffer.snapshot().size)

        buffer.write(floatArrayOf(9f))
        assertArrayEquals(floatArrayOf(9f), buffer.snapshot(), 0f)
    }

    @Test
    fun `many small writes wrap around correctly`() {
        val buffer = RollingAudioBuffer(maxSamples = 6)
        repeat(10) { i -> buffer.write(floatArrayOf(i.toFloat())) }

        // Last 6 values written were 4,5,6,7,8,9
        assertArrayEquals(floatArrayOf(4f, 5f, 6f, 7f, 8f, 9f), buffer.snapshot(), 0f)
    }
}
