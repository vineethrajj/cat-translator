package com.vineethraj.cattranslator.ml

import android.content.Context
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier.AudioClassifierOptions
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.core.BaseOptions

/** A single AudioSet label (e.g. "Meow", "Purr", "Growling") with its confidence score. */
data class SoundLabel(val name: String, val score: Float)

/** Thrown when the on-device classifier fails to load or fails to process a clip. */
class SoundClassificationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Wraps MediaPipe's on-device Audio Classifier task running Google's pretrained YAMNet model
 * (bundled at `assets/yamnet.tflite`) to identify what kind of sound was recorded, out of
 * AudioSet's 521 classes. This only tells us the sound *type* (e.g. Meow vs Purr vs Growling) -
 * [com.vineethraj.cattranslator.mood.MoodEngine] combines that with acoustic features to guess mood.
 */
class CatSoundClassifier(context: Context) : AutoCloseable {

    private val classifier: AudioClassifier = try {
        AudioClassifier.createFromOptions(
            context.applicationContext,
            AudioClassifierOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET_PATH).build())
                .setRunningMode(RunningMode.AUDIO_CLIPS)
                .setMaxResults(MAX_RESULTS)
                .setScoreThreshold(MIN_SCORE)
                .build(),
        )
    } catch (e: Exception) {
        throw SoundClassificationException("Could not load the sound classifier model.", e)
    }

    /** Classifies [samples] (mono PCM at [sampleRateHz]), returning labels sorted by descending score. */
    fun classify(samples: FloatArray, sampleRateHz: Int): List<SoundLabel> {
        val minSamples = sampleRateHz / 4
        if (samples.size < minSamples) {
            throw SoundClassificationException("Not enough audio captured to classify.")
        }

        val result = try {
            val format = AudioData.AudioDataFormat.builder()
                .setNumOfChannels(1)
                .setSampleRate(sampleRateHz.toFloat())
                .build()
            val audioData = AudioData.create(format, samples.size)
            audioData.load(samples)
            classifier.classify(audioData)
        } catch (e: Exception) {
            throw SoundClassificationException("The sound classifier failed to process this clip.", e)
        }

        // YAMNet classifies the clip in ~1s windows, so merge windows by each label's best score.
        val bestScoreByLabel = HashMap<String, Float>()
        for (classificationResult in result.classificationResults()) {
            for (classifications in classificationResult.classifications()) {
                for (category in classifications.categories()) {
                    val existing = bestScoreByLabel[category.categoryName()]
                    if (existing == null || category.score() > existing) {
                        bestScoreByLabel[category.categoryName()] = category.score()
                    }
                }
            }
        }
        return bestScoreByLabel.entries
            .map { SoundLabel(it.key, it.value) }
            .sortedByDescending { it.score }
    }

    override fun close() {
        runCatching { classifier.close() }
    }

    private companion object {
        const val MODEL_ASSET_PATH = "yamnet.tflite"
        const val MAX_RESULTS = 5
        const val MIN_SCORE = 0.1f
    }
}
