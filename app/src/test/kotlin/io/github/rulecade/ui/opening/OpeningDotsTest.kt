package io.github.rulecade.ui.opening

import io.github.rulecade.ui.component.PixelFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningDotsTest {

    private val dots = openingDots()

    @Test
    fun everyWordmarkDotLightsUpOnce() {
        val expected = PixelFont.dots(WORDMARK).toSet()
        val actual = dots.map { it.x to it.y }
        assertEquals(expected.size, actual.size)
        assertEquals(expected, actual.toSet())
    }

    @Test
    fun wordmarkIsCompleteBeforeLanding() {
        val last = dots.maxOf { it.start } + OpeningTimeline.POP
        assertTrue("last dot settles at $last ms", last <= OpeningTimeline.LAND_START)
    }

    @Test
    fun rLightsUpBeforeTheRestOfTheWord() {
        val (r, rest) = dots.partition { it.x < PixelFont.COLUMNS }
        assertTrue(r.maxOf { it.start } < rest.minOf { it.start })
        assertEquals(r.sortedBy { it.start }, r)
    }
}
