package io.github.rulecade.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelFontTest {

    @Test
    fun glyphsAreFiveBySeven() {
        PixelFont.glyphs.forEach { (char, rows) ->
            assertEquals("$char rows", PixelFont.ROWS, rows.size)
            rows.forEach { assertEquals("$char columns", PixelFont.COLUMNS, it.length) }
        }
    }

    @Test
    fun wordmarkDotsStayInsideItsBox() {
        val text = "Rulecade"
        val dots = PixelFont.dots(text)
        assertEquals(47, PixelFont.width(text))
        assertTrue(dots.isNotEmpty())
        assertTrue(
            dots.all { (x, y) ->
                x in 0 until PixelFont.width(text) &&
                    y in 0 until PixelFont.ROWS
            }
        )
    }
}
