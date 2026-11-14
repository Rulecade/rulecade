package io.github.rulecade.ui.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FireworksTest {

    private fun frameAt(t: Int) = Fireworks.Frame().also { Fireworks.fill(t.toFloat(), it) }

    private fun Fireworks.Frame.sparks() = counts.sum()

    @Test
    fun threeBurstsOfFortyDots() {
        assertEquals(120, Fireworks.sparkCount)
    }

    @Test
    fun decorationStaysOutOfTheContent() {
        assertTrue(Fireworks.decor.size >= 8)
        Fireworks.decor.forEach { d -> assertTrue(!Fireworks.inContent(d.x, d.y)) }
        assertEquals(Fireworks.decor.size, Fireworks.settlerCount)
    }

    @Test
    fun nothingShowsBeforeTheFirstBurst() {
        val frame = frameAt(100)
        assertEquals(0, frame.sparks())
        assertEquals(0, frame.settled)
    }

    @Test
    fun sparksAreGoneAndSettlersLandWhenItEnds() {
        val frame = frameAt(Fireworks.END_MILLIS)
        assertEquals(0, frame.sparks())
        assertEquals(Fireworks.settlerCount, frame.settled)
        Fireworks.decor.forEachIndexed { i, d ->
            assertEquals(d.x + d.size / 2f, frame.settlerX[i], 0.01f)
            assertEquals(d.y + d.size / 2f, frame.settlerY[i], 0.01f)
            assertEquals(d.size, frame.settlerSize[i], 0.01f)
            assertEquals(Fireworks.DECOR_ALPHA, frame.settlerAlpha[i], 0.001f)
        }
    }

    @Test
    fun framesAreRepeatable() {
        val a = frameAt(700)
        val b = frameAt(700)
        assertTrue(a.sparks() > 0)
        assertTrue(a.counts.contentEquals(b.counts))
        a.points.indices.forEach { assertTrue(a.points[it].contentEquals(b.points[it])) }
    }
}
