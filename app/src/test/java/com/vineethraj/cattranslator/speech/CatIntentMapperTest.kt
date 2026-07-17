package com.vineethraj.cattranslator.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class CatIntentMapperTest {

    @Test
    fun `maps food keywords`() {
        assertEquals(CatIntent.FOOD, CatIntentMapper.map("are you hungry? dinner time"))
    }

    @Test
    fun `maps multi-word praise phrase`() {
        assertEquals(CatIntent.PRAISE, CatIntentMapper.map("who's a good boy"))
    }

    @Test
    fun `maps summon phrase`() {
        assertEquals(CatIntent.SUMMON, CatIntentMapper.map("come here kitty"))
    }

    @Test
    fun `maps scold keyword`() {
        assertEquals(CatIntent.SCOLD, CatIntentMapper.map("no, get off the counter"))
    }

    @Test
    fun `maps play keyword`() {
        assertEquals(CatIntent.PLAY, CatIntentMapper.map("let's play with the toy"))
    }

    @Test
    fun `maps greeting phrase`() {
        assertEquals(CatIntent.GREETING, CatIntentMapper.map("good morning sleepyhead"))
    }

    @Test
    fun `maps affection keyword`() {
        assertEquals(CatIntent.AFFECTION, CatIntentMapper.map("I love you so much"))
    }

    @Test
    fun `unrelated speech maps to unknown`() {
        assertEquals(CatIntent.UNKNOWN, CatIntentMapper.map("the weather is nice today"))
    }

    @Test
    fun `short keyword does not match inside unrelated words`() {
        assertEquals(CatIntent.UNKNOWN, CatIntentMapper.map("it started to snow outside"))
        assertEquals(CatIntent.UNKNOWN, CatIntentMapper.map("nobody knows the answer"))
    }
}
