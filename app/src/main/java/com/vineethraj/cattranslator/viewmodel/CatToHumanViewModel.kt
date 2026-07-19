package com.vineethraj.cattranslator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vineethraj.cattranslator.audio.AcousticFeatureExtractor
import com.vineethraj.cattranslator.audio.AudioRecorder
import com.vineethraj.cattranslator.audio.RetrospectiveSegmentFinder
import com.vineethraj.cattranslator.audio.RollingAudioBuffer
import com.vineethraj.cattranslator.audio.VoiceActivityDetector
import com.vineethraj.cattranslator.data.HistoryEntry
import com.vineethraj.cattranslator.data.Stores
import com.vineethraj.cattranslator.data.TranslationDirection
import com.vineethraj.cattranslator.ml.CatSoundClassifier
import com.vineethraj.cattranslator.mood.CatMood
import com.vineethraj.cattranslator.mood.MoodEngine
import com.vineethraj.cattranslator.mood.PhraseBank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SAMPLE_RATE_HZ = 16_000
private const val ROLLING_BUFFER_SECONDS = 12

/** One recognized cat sound found during a listening session. */
data class Detection(
    val mood: CatMood,
    val phrase: String,
    val matchedLabel: String?,
    val confidencePercent: Int,
    val catName: String? = null,
)

sealed interface CatToHumanUiState {
    data object Idle : CatToHumanUiState

    /** Actively listening; [detections] is newest-first, growing as cat sounds are recognized. */
    data class Listening(val detections: List<Detection> = emptyList()) : CatToHumanUiState

    /** Briefly shown after Stop, only when live detection found nothing this session. */
    data object Analyzing : CatToHumanUiState

    /** The user stopped the session; [detections] is the final tally. */
    data class Stopped(val detections: List<Detection>, val triedRetrospectiveScan: Boolean = false) :
        CatToHumanUiState

    data class Error(val message: String) : CatToHumanUiState
}

/**
 * Listens continuously instead of recording a blind fixed window: raw audio is fed through a
 * [VoiceActivityDetector] to find the start/end of each sound, and only finished segments are
 * run through the classifier. Non-cat sounds (human speech, background noise) simply produce no
 * reaction - [MoodEngine] returns [CatMood.UNKNOWN] for them and they're silently discarded.
 *
 * As a safety net, the last [ROLLING_BUFFER_SECONDS] of raw audio are always kept in a
 * [RollingAudioBuffer]. If a whole session ends with zero live detections (the onset threshold
 * can be a bit strict for a quiet meow, a noisy room, etc.), stopping triggers one retrospective,
 * more lenient pass over that buffer via [RetrospectiveSegmentFinder] before giving up.
 */
class CatToHumanViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(SAMPLE_RATE_HZ)
    private val classifierLazy = lazy { CatSoundClassifier(application) }
    private val classifier by classifierLazy
    private val historyStore = Stores.history(application)
    private val profileStore = Stores.profiles(application)
    private val vad = VoiceActivityDetector(sampleRateHz = SAMPLE_RATE_HZ)
    private val rollingBuffer = RollingAudioBuffer(maxSamples = SAMPLE_RATE_HZ * ROLLING_BUFFER_SECONDS)

    private val _uiState = MutableStateFlow<CatToHumanUiState>(CatToHumanUiState.Idle)
    val uiState: StateFlow<CatToHumanUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var consumerJob: Job? = null
    private var segmentChannel: Channel<FloatArray>? = null

    fun startListening() {
        if (listenJob?.isActive == true) return
        rollingBuffer.clear()
        _uiState.value = CatToHumanUiState.Listening()

        val channel = Channel<FloatArray>(Channel.UNLIMITED)
        segmentChannel = channel

        // Segments are classified one at a time on their own coroutine, so a slow classify()
        // call never blocks the audio read loop from capturing the next chunk.
        consumerJob = viewModelScope.launch(Dispatchers.Default) {
            for (segment in channel) {
                processLiveSegment(segment)
            }
        }

        listenJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    audioRecorder.listenContinuously(chunkMs = 20) { chunk ->
                        rollingBuffer.write(chunk)
                        vad.accept(chunk)?.let { channel.trySend(it) }
                    }
                }
            } catch (e: CancellationException) {
                // Stopping is a normal, expected way for this loop to end - salvage whatever
                // sound was still in progress before rethrowing to complete the cancellation.
                vad.flush()?.let { channel.trySend(it) }
                throw e
            } catch (e: SecurityException) {
                _uiState.value = CatToHumanUiState.Error(
                    "Microphone permission was denied. Grant it in Settings and try again.",
                )
            } catch (e: Throwable) {
                _uiState.value = CatToHumanUiState.Error(
                    e.message ?: "Something went wrong while listening. Please try again.",
                )
            }
        }
    }

    /**
     * Stops listening. Sequenced deliberately: cancel-and-join the recording loop (so its
     * cancellation-time flush has already been enqueued), close the segment channel, then join
     * the consumer (so every queued segment - including that flush - has finished classifying)
     * *before* checking whether anything was found. Without this order, a meow that finished
     * right as Stop was tapped could be dropped by pure timing, which would make the "found
     * nothing, fall back to a retrospective scan" decision below unreliable.
     */
    fun stopListening() {
        val job = listenJob
        val consumer = consumerJob
        val channel = segmentChannel
        listenJob = null
        consumerJob = null
        segmentChannel = null

        viewModelScope.launch {
            job?.cancelAndJoin()
            channel?.close()
            consumer?.join()

            val detections = currentDetections()
            if (detections.isNotEmpty()) {
                _uiState.value = CatToHumanUiState.Stopped(detections)
                return@launch
            }

            _uiState.value = CatToHumanUiState.Analyzing
            val detection = try {
                val buffered = withContext(Dispatchers.Default) { rollingBuffer.snapshot() }
                val candidate = withContext(Dispatchers.Default) {
                    RetrospectiveSegmentFinder.findBestSegment(buffered, SAMPLE_RATE_HZ)
                }
                candidate?.let { classifySegmentOnly(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // The retrospective scan is a best-effort safety net - a failure here should
                // still land on a normal "nothing found" result, not an error/crash.
                null
            }
            _uiState.value = CatToHumanUiState.Stopped(
                detections = listOfNotNull(detection),
                triedRetrospectiveScan = true,
            )
        }
    }

    fun reset() {
        listenJob?.cancel()
        listenJob = null
        consumerJob?.cancel()
        consumerJob = null
        segmentChannel?.close()
        segmentChannel = null
        _uiState.value = CatToHumanUiState.Idle
    }

    private fun currentDetections(): List<Detection> = when (val state = _uiState.value) {
        is CatToHumanUiState.Listening -> state.detections
        is CatToHumanUiState.Stopped -> state.detections
        else -> emptyList()
    }

    private suspend fun processLiveSegment(segment: FloatArray) {
        try {
            val detection = classifySegmentOnly(segment) ?: return
            when (val current = _uiState.value) {
                is CatToHumanUiState.Listening ->
                    _uiState.value = current.copy(detections = listOf(detection) + current.detections)
                is CatToHumanUiState.Stopped ->
                    _uiState.value = current.copy(detections = listOf(detection) + current.detections)
                else -> Unit
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // A single bad segment shouldn't end the whole listening session.
        }
    }

    /** Classifies one finished segment and records it to history; doesn't touch UI state. */
    private suspend fun classifySegmentOnly(segment: FloatArray): Detection? {
        val features = AcousticFeatureExtractor.extract(segment, SAMPLE_RATE_HZ)
        val labels = classifier.classify(segment, SAMPLE_RATE_HZ)
        val moodResult = MoodEngine.infer(labels, features)
        // Not a cat sound (or too uncertain to say) - ignore it and keep listening.
        if (moodResult.mood == CatMood.UNKNOWN) return null

        val activeCat = withContext(Dispatchers.IO) { profileStore.load().activeProfile }
        val confidencePercent = (moodResult.confidence * 100).toInt()
        val detection = Detection(
            mood = moodResult.mood,
            phrase = PhraseBank.phraseFor(moodResult.mood),
            matchedLabel = moodResult.matchedLabel,
            confidencePercent = confidencePercent,
            catName = activeCat?.name,
        )

        withContext(Dispatchers.IO) {
            historyStore.append(
                HistoryEntry(
                    timestampMs = System.currentTimeMillis(),
                    direction = TranslationDirection.CAT_TO_HUMAN,
                    label = detection.mood.name,
                    text = detection.phrase,
                    confidencePercent = confidencePercent,
                    catId = activeCat?.id,
                ),
            )
        }
        return detection
    }

    override fun onCleared() {
        super.onCleared()
        listenJob?.cancel()
        consumerJob?.cancel()
        segmentChannel?.close()
        if (classifierLazy.isInitialized()) classifierLazy.value.close()
    }
}
