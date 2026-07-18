package com.vineethraj.cattranslator.data

import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CatProfile(
    val id: String,
    val name: String,
    val photoUri: String? = null,
)

@Serializable
data class ProfilesState(
    val profiles: List<CatProfile> = emptyList(),
    val activeId: String? = null,
) {
    val activeProfile: CatProfile? get() = profiles.firstOrNull { it.id == activeId }
}

/**
 * Persists cat profiles + the active selection as a JSON file in app-private storage.
 * Framework-free (plain [File]) so the logic runs in JVM unit tests; methods synchronized.
 */
class CatProfileStore(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun load(): ProfilesState {
        if (!file.exists()) return ProfilesState()
        return try {
            json.decodeFromString<ProfilesState>(file.readText())
        } catch (e: Exception) {
            ProfilesState()
        }
    }

    @Synchronized
    fun add(name: String, photoUri: String? = null): ProfilesState {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return load()
        val state = load()
        val profile = CatProfile(id = UUID.randomUUID().toString(), name = trimmed, photoUri = photoUri)
        // First cat added becomes active automatically.
        val newState = state.copy(
            profiles = state.profiles + profile,
            activeId = state.activeId ?: profile.id,
        )
        save(newState)
        return newState
    }

    @Synchronized
    fun rename(id: String, newName: String): ProfilesState {
        val trimmed = newName.trim()
        val state = load()
        if (trimmed.isEmpty()) return state
        val newState = state.copy(
            profiles = state.profiles.map { if (it.id == id) it.copy(name = trimmed) else it },
        )
        save(newState)
        return newState
    }

    @Synchronized
    fun setPhoto(id: String, photoUri: String?): ProfilesState {
        val state = load()
        val newState = state.copy(
            profiles = state.profiles.map { if (it.id == id) it.copy(photoUri = photoUri) else it },
        )
        save(newState)
        return newState
    }

    @Synchronized
    fun remove(id: String): ProfilesState {
        val state = load()
        val remaining = state.profiles.filterNot { it.id == id }
        val newState = state.copy(
            profiles = remaining,
            activeId = if (state.activeId == id) remaining.firstOrNull()?.id else state.activeId,
        )
        save(newState)
        return newState
    }

    @Synchronized
    fun setActive(id: String): ProfilesState {
        val state = load()
        if (state.profiles.none { it.id == id }) return state
        val newState = state.copy(activeId = id)
        save(newState)
        return newState
    }

    private fun save(state: ProfilesState) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(state))
        } catch (e: Exception) {
            // Best-effort persistence; never crash over profile data.
        }
    }
}
