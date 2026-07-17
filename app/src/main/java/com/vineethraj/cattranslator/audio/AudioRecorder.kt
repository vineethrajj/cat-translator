package com.vineethraj.cattranslator.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build

/** Thrown when the microphone could not be opened or produced no usable audio. */
class MicrophoneUnavailableException(message: String) : Exception(message)

/**
 * Records raw mono PCM audio from the microphone at [sampleRateHz] and returns it as
 * samples normalized to [-1, 1]. Callers must hold RECORD_AUDIO before invoking [record]
 * and should call it from a background thread/coroutine, since it blocks for the full
 * recording duration.
 */
class AudioRecorder(private val sampleRateHz: Int = 16_000) {

    /**
     * Audio sources to try, in order of preference. UNPROCESSED (and failing that,
     * VOICE_RECOGNITION) skip the noise-suppression/AGC processing most phones apply to the
     * default MIC source, which otherwise tends to flatten the pitch/energy detail we rely on
     * for classifying cat sounds. Not every device actually honors these sources, so we fall
     * back down the list until one actually initializes.
     */
    private val preferredSources: List<Int> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            add(android.media.MediaRecorder.AudioSource.UNPROCESSED)
        }
        add(android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION)
        add(android.media.MediaRecorder.AudioSource.MIC)
    }

    @SuppressLint("MissingPermission")
    fun record(durationMs: Int): FloatArray {
        val audioRecord = openAudioRecord()
        val totalSamples = (sampleRateHz.toLong() * durationMs / 1000L).toInt()
        val shortBuffer = ShortArray(totalSamples)
        var samplesRead = 0
        var startedRecording = false

        try {
            audioRecord.startRecording()
            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw MicrophoneUnavailableException(
                    "The microphone could not start recording. It may be in use by another app.",
                )
            }
            startedRecording = true

            while (samplesRead < totalSamples) {
                val n = audioRecord.read(shortBuffer, samplesRead, totalSamples - samplesRead)
                if (n <= 0) break
                samplesRead += n
            }
        } finally {
            runCatching {
                if (startedRecording) audioRecord.stop()
            }
            runCatching { audioRecord.release() }
        }

        if (samplesRead < sampleRateHz / 4) {
            throw MicrophoneUnavailableException(
                "Didn't capture enough audio to analyze. Try again a little closer to the mic.",
            )
        }

        return FloatArray(samplesRead) { i -> shortBuffer[i] / 32768f }
    }

    private fun openAudioRecord(): AudioRecord {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).let { if (it > 0) it else sampleRateHz }
        val bufferSizeBytes = minBufferSize * 2

        var lastError: Exception? = null
        for (source in preferredSources) {
            val candidate = try {
                AudioRecord(
                    source,
                    sampleRateHz,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeBytes,
                )
            } catch (e: Exception) {
                lastError = e
                null
            }

            if (candidate != null && candidate.state == AudioRecord.STATE_INITIALIZED) {
                return candidate
            }
            candidate?.release()
        }

        throw MicrophoneUnavailableException(
            "Could not access the microphone. Check that microphone permission is granted and " +
                "no other app is using it.",
        ).apply { lastError?.let { initCause(it) } }
    }
}
