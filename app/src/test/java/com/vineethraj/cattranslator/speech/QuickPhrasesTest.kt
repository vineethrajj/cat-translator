package com.vineethraj.cattranslator.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickPhrasesTest {

    @Test
    fun `no pins keeps default order`() {
        assertEquals(QuickPhrases.all, QuickPhrases.ordered(emptyList()))
    }

    @Test
    fun `pinned phrases come first in pin order`() {
        val ordered = QuickPhrases.ordered(listOf("No!", "Dinner time!"))

        assertEquals("No!", ordered[0].label)
        assertEquals("Dinner time!", ordered[1].label)
        assertEquals(QuickPhrases.all.size, ordered.size)
        assertEquals(QuickPhrases.all.toSet(), ordered.toSet())
    }

    @Test
    fun `unknown pinned labels are ignored`() {
        val ordered = QuickPhrases.ordered(listOf("Not a phrase", "Hello!"))

        assertEquals("Hello!", ordered[0].label)
        assertEquals(QuickPhrases.all.size, ordered.size)
    }
}
