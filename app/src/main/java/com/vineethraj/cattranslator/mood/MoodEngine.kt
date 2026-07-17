package com.vineethraj.cattranslator.mood

import com.vineethraj.cattranslator.audio.AcousticFeatures
import com.vineethraj.cattranslator.ml.SoundLabel

data class MoodResult(val mood: CatMood, val confidence: Float, val matchedLabel: String?)

/**
 * Combines YAMNet's raw AudioSet label (what kind of sound it is) with acoustic features (pitch,
 * duration, repetition) to guess a cat's mood/intent. There's no public dataset of cat
 * vocalizations labeled with emotional intent, so no legitimate trained model for that exists -
 * this decision table is our own best-effort heuristic layered on top of a real classification of
 * the sound type.
 */
object MoodEngine {

    private const val CONFIDENT_LABEL_SCORE = 0.15f

    fun infer(labels: List<SoundLabel>, features: AcousticFeatures): MoodResult {
        val topLabel = labels.firstOrNull { it.score >= CONFIDENT_LABEL_SCORE }
            ?: return MoodResult(CatMood.UNKNOWN, 0f, null)

        return when (topLabel.name) {
            "Purr" -> MoodResult(CatMood.CONTENT, topLabel.score, topLabel.name)
            "Growling", "Hiss" -> MoodResult(CatMood.ANNOYED, topLabel.score, topLabel.name)
            "Caterwaul" -> MoodResult(CatMood.DISTRESSED, topLabel.score, topLabel.name)
            "Meow", "Cat" -> MoodResult(inferMeowMood(features), topLabel.score, topLabel.name)
            else -> MoodResult(CatMood.UNKNOWN, 0f, topLabel.name)
        }
    }

    private fun inferMeowMood(features: AcousticFeatures): CatMood = when {
        features.pulseCount >= 3 -> CatMood.HUNGRY
        features.durationSeconds > 1.0f && features.pitchHz in 250f..600f -> CatMood.GREETING
        features.pitchHz > 600f -> CatMood.WANTS_ATTENTION
        features.pulseCount in 1..2 -> CatMood.PLAYFUL
        else -> CatMood.WANTS_ATTENTION
    }
}
