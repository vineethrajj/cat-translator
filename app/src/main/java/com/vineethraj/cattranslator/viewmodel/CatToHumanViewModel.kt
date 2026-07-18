package com.vineethraj.cattranslator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vineethraj.cattranslator.audio.AcousticFeatureExtractor
import com.vineethraj.cattranslator.audio.AudioRecorder
import com.vineethraj.cattranslator.data.HistoryEntry
import com.vineethraj.cattranslator.data.Stores
import com.vineethraj.cattranslator.data.TranslationDirection
import com.vineethraj.cattranslator.ml.CatSoundClassifier
import com.vineethraj.cattranslator.mood.CatMood
import com.vineethraj.cattranslator.mood.MoodEngine
import com.vineethraj.cattranslator.mood.PhraseBank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SAMPLE_RATE_HZ = 16_000
private const val RECORDING_DURATION_MS = 3_000

sealed interface CatToHumanUiState {
    data object Idle : CatToHumanUiState
    data object Recording : CatToHumanUiState
    data object Processing : CatToHumanUiState
    data class Result(
        val mood: CatMood,
        val phrase: String,
        val matchedLabel: String?,
        val confidencePercent: Int,
        val catName: String? = null,
    ) : CatToHumanUiState
    data class Error(val message: String) : CatToHumanUiState
}

class CatToHumanViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(SAMPLE_RATE_HZ)
    private val classifierLazy = lazy { CatSoundClassifier(application) }
    private val classifier by classifierLazy
    private val historyStore = Stores.history(application)
    private val profileStore = Stores.profiles(application)

    private val _uiState = MutableStateFlow<CatToHumanUiState>(CatToHumanUiState.Idle)
    val uiState: StateFlow<CatToHumanUiState> = _uiState.asStateFlow()

    fun recordAndTranslate() {
        if (_uiState.value == CatToHumanUiState.Recording) return
        _uiState.value = CatToHumanUiState.Recording

        viewModelScope.launch {
            try {
                val samples = withContext(Dispatchers.IO) {
                    audioRecorder.record(RECORDING_DURATION_MS)
                }
                _uiState.value = CatToHumanUiState.Processing

                val outcome = withContext(Dispatchers.Default) {
                    val features = AcousticFeatureExtractor.extract(samples, SAMPLE_RATE_HZ)
                    val labels = classifier.classify(samples, SAMPLE_RATE_HZ)
                    val moodResult = MoodEngine.infer(labels, features)
                    TranslationOutcome(
                        mood = moodResult.mood,
                        phrase = PhraseBank.phraseFor(moodResult.mood),
                        matchedLabel = moodResult.matchedLabel,
                        confidence = moodResult.confidence,
                    )
                }

                val activeCat = withContext(Dispatchers.IO) { profileStore.load().activeProfile }
                val confidencePercent = (outcome.confidence * 100).toInt()

                _uiState.value = CatToHumanUiState.Result(
                    mood = outcome.mood,
                    phrase = outcome.phrase,
                    matchedLabel = outcome.matchedLabel,
                    confidencePercent = confidencePercent,
                    catName = activeCat?.name,
                )

                withContext(Dispatchers.IO) {
                    historyStore.append(
                        HistoryEntry(
                            timestampMs = System.currentTimeMillis(),
                            direction = TranslationDirection.CAT_TO_HUMAN,
                            label = outcome.mood.name,
                            text = outcome.phrase,
                            confidencePercent = confidencePercent,
                            catId = activeCat?.id,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                _uiState.value = CatToHumanUiState.Error(
                    "Microphone permission was denied. Grant it in Settings and try again.",
                )
            } catch (e: Throwable) {
                _uiState.value = CatToHumanUiState.Error(
                    e.message ?: "Something went wrong while translating. Please try again.",
                )
            }
        }
    }

    fun reset() {
        _uiState.value = CatToHumanUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        if (classifierLazy.isInitialized()) classifierLazy.value.close()
    }

    private data class TranslationOutcome(
        val mood: CatMood,
        val phrase: String,
        val matchedLabel: String?,
        val confidence: Float,
    )
}
