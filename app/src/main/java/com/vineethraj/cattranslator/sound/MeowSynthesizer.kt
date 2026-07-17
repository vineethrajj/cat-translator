package com.vineethraj.cattranslator.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.vineethraj.cattranslator.speech.CatIntent
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedurally synthesizes a short cat-like sound (meow/purr/chirp/growl contour) per [CatIntent]
 * and plays it through the speaker. There's no way to license real cat-sound recordings for this
 * project, so these are generated tones shaped with a cat-like pitch envelope, not bundled audio.
 */
class MeowSynthesizer {

    private val sampleRateHz = 44_100

    fun play(intent: CatIntent) {
        val samples = generate(intent)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.setNotificationMarkerPosition(samples.size)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) {
                track.release()
            }

            override fun onPeriodicNotification(track: AudioTrack) = Unit
        })
        audioTrack.play()
    }

    private fun generate(intent: CatIntent): ShortArray {
        val notes = profileFor(intent)
        val gapSamples = (0.08f * sampleRateHz).toInt()
        val noteSamples = notes.map { synthesizeNote(it) }
        val totalSamples = noteSamples.sumOf { it.size } + gapSamples * (notes.size - 1).coerceAtLeast(0)

        val output = ShortArray(totalSamples)
        var offset = 0
        for ((index, note) in noteSamples.withIndex()) {
            note.copyInto(output, offset)
            offset += note.size
            if (index != noteSamples.lastIndex) offset += gapSamples
        }
        return output
    }

    private fun profileFor(intent: CatIntent): List<Note> = when (intent) {
        CatIntent.FOOD -> listOf(
            Note(durationMs = 260, startHz = 600f, endHz = 300f, vibratoHz = 6f, vibratoDepth = 0.03f),
            Note(durationMs = 260, startHz = 620f, endHz = 300f, vibratoHz = 6f, vibratoDepth = 0.03f),
        )
        CatIntent.PRAISE -> listOf(
            Note(durationMs = 300, startHz = 450f, endHz = 750f, vibratoHz = 10f, vibratoDepth = 0.05f),
        )
        CatIntent.SUMMON -> listOf(
            Note(durationMs = 400, startHz = 520f, endHz = 260f, vibratoHz = 5f, vibratoDepth = 0.03f),
        )
        CatIntent.SCOLD -> listOf(
            Note(durationMs = 600, startHz = 180f, endHz = 150f, vibratoHz = 14f, vibratoDepth = 0.15f, noiseAmount = 0.35f),
        )
        CatIntent.DISMISS -> listOf(
            Note(durationMs = 160, startHz = 500f, endHz = 350f, vibratoHz = 8f, vibratoDepth = 0.05f, noiseAmount = 0.1f),
            Note(durationMs = 160, startHz = 450f, endHz = 300f, vibratoHz = 8f, vibratoDepth = 0.05f, noiseAmount = 0.1f),
        )
        CatIntent.PLAY -> listOf(
            Note(durationMs = 140, startHz = 700f, endHz = 900f, vibratoHz = 12f, vibratoDepth = 0.04f),
            Note(durationMs = 140, startHz = 750f, endHz = 950f, vibratoHz = 12f, vibratoDepth = 0.04f),
            Note(durationMs = 140, startHz = 800f, endHz = 1000f, vibratoHz = 12f, vibratoDepth = 0.04f),
        )
        CatIntent.GREETING -> listOf(
            Note(durationMs = 500, startHz = 350f, endHz = 600f, vibratoHz = 8f, vibratoDepth = 0.06f),
        )
        CatIntent.AFFECTION -> listOf(
            Note(durationMs = 800, startHz = 220f, endHz = 240f, vibratoHz = 30f, vibratoDepth = 0.25f),
        )
        CatIntent.UNKNOWN -> listOf(
            Note(durationMs = 350, startHz = 400f, endHz = 300f, vibratoHz = 6f, vibratoDepth = 0.03f),
        )
    }

    private data class Note(
        val durationMs: Int,
        val startHz: Float,
        val endHz: Float,
        val vibratoHz: Float,
        val vibratoDepth: Float,
        val noiseAmount: Float = 0f,
    )

    private fun synthesizeNote(note: Note): ShortArray {
        val sampleCount = (sampleRateHz * note.durationMs / 1000f).toInt()
        val output = ShortArray(sampleCount)
        var phase = 0.0
        val random = Random(note.hashCode())

        val attackSamples = (sampleCount * 0.08f).toInt().coerceAtLeast(1)
        val releaseSamples = (sampleCount * 0.15f).toInt().coerceAtLeast(1)

        for (i in 0 until sampleCount) {
            val t = i / sampleCount.toFloat()
            val baseFreq = note.startHz + (note.endHz - note.startHz) * t
            val vibrato = 1f + note.vibratoDepth *
                sin(2.0 * PI * note.vibratoHz * (i / sampleRateHz.toFloat())).toFloat()
            val instantFreq = baseFreq * vibrato

            phase += 2.0 * PI * instantFreq / sampleRateHz
            var value = sin(phase).toFloat()

            if (note.noiseAmount > 0f) {
                value = value * (1f - note.noiseAmount) + (random.nextFloat() * 2f - 1f) * note.noiseAmount
            }

            val envelope = when {
                i < attackSamples -> i / attackSamples.toFloat()
                i > sampleCount - releaseSamples -> (sampleCount - i) / releaseSamples.toFloat()
                else -> 1f
            }

            val amplitude = 0.7f * envelope
            output[i] = (value * amplitude * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return output
    }
}
