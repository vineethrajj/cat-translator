package com.vineethraj.cattranslator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vineethraj.cattranslator.data.CatProfile
import com.vineethraj.cattranslator.data.HistoryEntry
import com.vineethraj.cattranslator.data.Stores
import com.vineethraj.cattranslator.sound.MeowSynthesizer
import com.vineethraj.cattranslator.speech.CatIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyStore = Stores.history(application)
    private val profileStore = Stores.profiles(application)
    private val synthesizer = MeowSynthesizer()

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    private val _profilesById = MutableStateFlow<Map<String, CatProfile>>(emptyMap())
    val profilesById: StateFlow<Map<String, CatProfile>> = _profilesById.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val (history, profiles) = withContext(Dispatchers.IO) {
                historyStore.load() to profileStore.load()
            }
            _entries.value = history
            _profilesById.value = profiles.profiles.associateBy { it.id }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { historyStore.clear() }
            _entries.value = emptyList()
        }
    }

    /** Replays the synthesized sound for a human->cat history entry. */
    fun replay(entry: HistoryEntry) {
        val intent = runCatching { CatIntent.valueOf(entry.label) }.getOrNull() ?: return
        runCatching { synthesizer.play(intent) }
    }
}
