package com.vineethraj.cattranslator.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * Records raw mono PCM audio from the microphone at [sampleRateHz] and returns it as
 * samples normalized to [-1, 1]. Callers must hold RECORD_AUDIO before invoking [record]
 * and should call it from a background thread/coroutine, since it blocks for the full
 * recording duration.
 */
class AudioRecorder(private val sampleRateHz: Int = 16_000) {

    @SuppressLint("MissingPermission")
    fun record(durationMs: Int): FloatArray {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(sampleRateHz)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize * 2,
        )

        val totalSamples = (sampleRateHz.toLong() * durationMs / 1000L).toInt()
        val shortBuffer = ShortArray(totalSamples)
        var samplesRead = 0

        try {
            audioRecord.startRecording()
            while (samplesRead < totalSamples) {
                val n = audioRecord.read(shortBuffer, samplesRead, totalSamples - samplesRead)
                if (n <= 0) break
                samplesRead += n
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }

        return FloatArray(samplesRead) { i -> shortBuffer[i] / 32768f }
    }
}
