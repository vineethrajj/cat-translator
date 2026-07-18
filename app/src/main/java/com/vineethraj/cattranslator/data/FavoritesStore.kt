package com.vineethraj.cattranslator.data

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the user's pinned quick-phrase labels (in pin order) as a JSON file.
 * Framework-free (plain [File]) so the logic runs in JVM unit tests; methods synchronized.
 */
class FavoritesStore(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun load(): List<String> {
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun toggle(label: String): List<String> {
        val current = load()
        val updated = if (label in current) current - label else current + label
        try {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(updated))
        } catch (e: Exception) {
            // Best-effort persistence.
        }
        return updated
    }
}
