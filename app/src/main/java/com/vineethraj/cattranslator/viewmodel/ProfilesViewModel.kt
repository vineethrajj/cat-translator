package com.vineethraj.cattranslator.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vineethraj.cattranslator.data.ProfilesState
import com.vineethraj.cattranslator.data.Stores
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilesViewModel(application: Application) : AndroidViewModel(application) {

    private val store = Stores.profiles(application)

    private val _state = MutableStateFlow(ProfilesState())
    val state: StateFlow<ProfilesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) { store.load() }
        }
    }

    fun add(name: String) = mutate { store.add(name) }

    fun rename(id: String, newName: String) = mutate { store.rename(id, newName) }

    /**
     * Copies the picked photo into app-private storage as a small thumbnail and stores its path.
     * Copying (rather than keeping the content URI) avoids photo-picker permission grants
     * lapsing after a reboot.
     */
    fun setPhotoFromUri(id: String, uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) {
                try {
                    val source = app.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    } ?: return@withContext store.load()

                    val scale = max(source.width, source.height) / 512f
                    val thumb = if (scale > 1f) {
                        Bitmap.createScaledBitmap(
                            source,
                            (source.width / scale).toInt().coerceAtLeast(1),
                            (source.height / scale).toInt().coerceAtLeast(1),
                            true,
                        )
                    } else {
                        source
                    }

                    val photoFile = File(app.filesDir, "cat-photo-$id.jpg")
                    photoFile.outputStream().use { out ->
                        thumb.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    store.setPhoto(id, photoFile.absolutePath)
                } catch (e: Exception) {
                    store.load()
                }
            }
        }
    }

    fun remove(id: String) = mutate { store.remove(id) }

    fun setActive(id: String) = mutate { store.setActive(id) }

    private fun mutate(operation: () -> ProfilesState) {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.IO) { operation() }
        }
    }
}
