package com.vineethraj.cattranslator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vineethraj.cattranslator.speech.CatIntent
import com.vineethraj.cattranslator.speech.CatIntentMapper
import com.vineethraj.cattranslator.speech.SpeechToTextHelper
import com.vineethraj.cattranslator.sound.MeowSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface HumanToCatUiState {
    data object Idle : HumanToCatUiState
    data object Listening : HumanToCatUiState
    data class Result(val recognizedText: String, val intent: CatIntent) : HumanToCatUiState
    data class Error(val message: String) : HumanToCatUiState
}

class HumanToCatViewModel(application: Application) : AndroidViewModel(application) {

    private val speechToText = SpeechToTextHelper(application)
    private val synthesizer = MeowSynthesizer()

    private val _uiState = MutableStateFlow<HumanToCatUiState>(HumanToCatUiState.Idle)
    val uiState: StateFlow<HumanToCatUiState> = _uiState.asStateFlow()

    fun isSpeechRecognitionAvailable(): Boolean = speechToText.isAvailable()

    fun startListening() {
        _uiState.value = HumanToCatUiState.Listening
        try {
            speechToText.startListening(
                onResult = { text ->
                    val intent = CatIntentMapper.map(text)
                    _uiState.value = HumanToCatUiState.Result(text, intent)
                    playSoundSafely(intent)
                },
                onError = { message ->
                    _uiState.value = HumanToCatUiState.Error(message)
                },
            )
        } catch (e: SecurityException) {
            _uiState.value = HumanToCatUiState.Error(
                "Microphone permission was denied. Grant it in Settings and try again.",
            )
        } catch (e: Exception) {
            _uiState.value = HumanToCatUiState.Error(
                e.message ?: "Could not start listening. Please try again.",
            )
        }
    }

    /** Bypasses speech recognition entirely - triggered by a quick-phrase button. */
    fun selectQuickPhrase(label: String, intent: CatIntent) {
        _uiState.value = HumanToCatUiState.Result(label, intent)
        playSoundSafely(intent)
    }

    fun replaySound() {
        val state = _uiState.value
        if (state is HumanToCatUiState.Result) {
            playSoundSafely(state.intent)
        }
    }

    fun reset() {
        speechToText.stopListening()
        _uiState.value = HumanToCatUiState.Idle
    }

    private fun playSoundSafely(intent: CatIntent) {
        runCatching { synthesizer.play(intent) }
    }

    override fun onCleared() {
        super.onCleared()
        speechToText.stopListening()
    }
}
