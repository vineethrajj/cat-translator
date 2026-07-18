package com.vineethraj.cattranslator.data

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class TranslationDirection { CAT_TO_HUMAN, HUMAN_TO_CAT }

@Serializable
data class HistoryEntry(
    val timestampMs: Long,
    val direction: TranslationDirection,
    /** Enum name of the CatMood (cat->human) or CatIntent (human->cat). */
    val label: String,
    /** The translated phrase (cat->human) or what the user said (human->cat). */
    val text: String,
    val confidencePercent: Int? = null,
    val catId: String? = null,
)

/**
 * Persists translation history as a JSON file in app-private storage. Newest entries first.
 * Kept deliberately framework-free (plain [File]) so the logic runs in JVM unit tests.
 * All methods are synchronized; callers should still invoke from a background dispatcher.
 */
class TranslationHistoryStore(private val file: File, private val maxEntries: Int = 200) {

    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun load(): List<HistoryEntry> {
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString<List<HistoryEntry>>(file.readText())
        } catch (e: Exception) {
            // A corrupt file must never take the app down; history is best-effort data.
            emptyList()
        }
    }

    @Synchronized
    fun append(entry: HistoryEntry): List<HistoryEntry> {
        val updated = (listOf(entry) + load()).take(maxEntries)
        save(updated)
        return updated
    }

    @Synchronized
    fun clear() {
        runCatching { file.delete() }
    }

    private fun save(entries: List<HistoryEntry>) {
        try {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(entries))
            if (!tmp.renameTo(file)) {
                file.writeText(json.encodeToString(entries))
                tmp.delete()
            }
        } catch (e: Exception) {
            // Best-effort persistence: losing a history write is acceptable, crashing is not.
        }
    }
}
