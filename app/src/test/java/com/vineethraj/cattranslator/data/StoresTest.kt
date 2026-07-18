package com.vineethraj.cattranslator.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StoresTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun entry(ts: Long, label: String = "HUNGRY") = HistoryEntry(
        timestampMs = ts,
        direction = TranslationDirection.CAT_TO_HUMAN,
        label = label,
        text = "Feed me",
        confidencePercent = 70,
    )

    // --- TranslationHistoryStore ---

    @Test
    fun `history round-trips through the file`() {
        val store = TranslationHistoryStore(File(tmp.root, "h.json"))
        store.append(entry(1))
        store.append(entry(2))

        val reloaded = TranslationHistoryStore(File(tmp.root, "h.json")).load()

        assertEquals(2, reloaded.size)
        assertEquals(2L, reloaded[0].timestampMs) // newest first
        assertEquals(1L, reloaded[1].timestampMs)
    }

    @Test
    fun `history trims to the cap, dropping oldest`() {
        val store = TranslationHistoryStore(File(tmp.root, "h.json"), maxEntries = 3)
        (1L..5L).forEach { store.append(entry(it)) }

        val entries = store.load()

        assertEquals(3, entries.size)
        assertEquals(listOf(5L, 4L, 3L), entries.map { it.timestampMs })
    }

    @Test
    fun `corrupt history file loads as empty instead of crashing`() {
        val file = File(tmp.root, "h.json")
        file.writeText("{not valid json!!")

        assertEquals(emptyList<HistoryEntry>(), TranslationHistoryStore(file).load())
    }

    @Test
    fun `clear removes all history`() {
        val store = TranslationHistoryStore(File(tmp.root, "h.json"))
        store.append(entry(1))
        store.clear()
        assertEquals(emptyList<HistoryEntry>(), store.load())
    }

    // --- CatProfileStore ---

    @Test
    fun `first added cat becomes active automatically`() {
        val store = CatProfileStore(File(tmp.root, "p.json"))
        val state = store.add("Whiskers")

        assertEquals(1, state.profiles.size)
        assertEquals("Whiskers", state.activeProfile?.name)
    }

    @Test
    fun `profiles round-trip and rename works`() {
        val file = File(tmp.root, "p.json")
        val store = CatProfileStore(file)
        val id = store.add("Whiskers").profiles.first().id
        store.add("Momo")
        store.rename(id, "Sir Whiskers")

        val reloaded = CatProfileStore(file).load()

        assertEquals(listOf("Sir Whiskers", "Momo"), reloaded.profiles.map { it.name })
        assertEquals(id, reloaded.activeId)
    }

    @Test
    fun `removing the active cat promotes the next one`() {
        val store = CatProfileStore(File(tmp.root, "p.json"))
        val firstId = store.add("Whiskers").profiles.first().id
        store.add("Momo")

        val state = store.remove(firstId)

        assertEquals(1, state.profiles.size)
        assertEquals("Momo", state.activeProfile?.name)
    }

    @Test
    fun `removing the last cat leaves no active profile`() {
        val store = CatProfileStore(File(tmp.root, "p.json"))
        val id = store.add("Whiskers").profiles.first().id
        val state = store.remove(id)

        assertTrue(state.profiles.isEmpty())
        assertNull(state.activeProfile)
    }

    @Test
    fun `blank names are rejected`() {
        val store = CatProfileStore(File(tmp.root, "p.json"))
        val state = store.add("   ")
        assertTrue(state.profiles.isEmpty())
    }

    @Test
    fun `setActive ignores unknown ids`() {
        val store = CatProfileStore(File(tmp.root, "p.json"))
        store.add("Whiskers")
        val state = store.setActive("no-such-id")
        assertNotNull(state.activeProfile)
        assertEquals("Whiskers", state.activeProfile?.name)
    }

    @Test
    fun `corrupt profile file loads as empty state`() {
        val file = File(tmp.root, "p.json")
        file.writeText("garbage")
        assertEquals(ProfilesState(), CatProfileStore(file).load())
    }

    // --- FavoritesStore ---

    @Test
    fun `toggle pins and unpins, preserving pin order`() {
        val store = FavoritesStore(File(tmp.root, "f.json"))
        store.toggle("Dinner time!")
        store.toggle("Come here!")

        assertEquals(listOf("Dinner time!", "Come here!"), store.load())

        store.toggle("Dinner time!")
        assertEquals(listOf("Come here!"), store.load())
    }

    @Test
    fun `corrupt favorites file loads as empty`() {
        val file = File(tmp.root, "f.json")
        file.writeText("!!")
        assertEquals(emptyList<String>(), FavoritesStore(file).load())
    }
}
