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
        speechToText.startListening(
            onResult = { text ->
                val intent = CatIntentMapper.map(text)
                _uiState.value = HumanToCatUiState.Result(text, intent)
                synthesizer.play(intent)
            },
            onError = { message ->
                _uiState.value = HumanToCatUiState.Error(message)
            },
        )
    }

    fun replaySound() {
        val state = _uiState.value
        if (state is HumanToCatUiState.Result) {
            synthesizer.play(state.intent)
        }
    }

    fun reset() {
        _uiState.value = HumanToCatUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        speechToText.stopListening()
    }
}
