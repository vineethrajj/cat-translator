package com.vineethraj.cattranslator.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** Wraps Android's [SpeechRecognizer] to recognize a single spoken phrase. */
class SpeechToTextHelper(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        stopListening()

        if (!isAvailable()) {
            onError(
                "Speech recognition isn't available on this device. Make sure the Google app " +
                    "is installed and up to date, or use the quick phrase buttons below instead.",
            )
            return
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                onError(errorMessage(error))
            }

            override fun onResults(results: Bundle?) {
                val best = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                if (best != null) {
                    onResult(best)
                } else {
                    onError("Didn't catch that - try again, or use a quick phrase button below.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            onError(e.message ?: "Could not start the speech recognizer. Please try again.")
        }
    }

    fun stopListening() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please try again."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was interrupted. Please try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK -> "Network error. Check your connection and try again."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out. Please try again."
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that - try again, or use a quick phrase button below."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please try again in a moment."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again."
        else -> "Speech recognition failed. Please try again, or use a quick phrase button below."
    }
}
