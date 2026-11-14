package io.github.rulecade.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

// Pleos design guide: text and icons need a contrast ratio of at least 4.5:1.
class ContrastTest {

    @Test
    fun lightThemeTextIsReadable() = assertReadable(LightColors)

    @Test
    fun darkThemeTextIsReadable() = assertReadable(DarkColors)

    private fun assertReadable(c: RcColors) {
        val accentSoftCard = c.accentSoft.compositeOver(c.surfaceCard)
        val goldSoftTile = c.gradeGoldSoft.compositeOver(c.surfaceTile)
        val pairs = mapOf(
            "text_primary on background_primary" to (c.textPrimary to c.backgroundPrimary),
            "text_primary on background_popup" to (c.textPrimary to c.backgroundPopup),
            "text_primary on surface_card" to (c.textPrimary to c.surfaceCard),
            "text_secondary on surface_card" to (c.textSecondary to c.surfaceCard),
            "text_tertiary on surface_card" to (c.textTertiary to c.surfaceCard),
            "text_tertiary on background_primary" to (c.textTertiary to c.backgroundPrimary),
            "accent on background_primary" to (c.accent to c.backgroundPrimary),
            "accent on surface_card" to (c.accent to c.surfaceCard),
            "accent on accent_soft" to (c.accent to accentSoftCard),
            "on_accent on accent" to (c.onAccent to c.accent),
            "on_button_filled on button_filled" to (c.onButtonFilled to c.buttonFilledEnabled),
            "grade_silver on surface_tile" to (c.gradeSilver to c.surfaceTile),
            "grade_gold on surface_tile" to (c.gradeGold to c.surfaceTile),
            "grade_gold on grade_gold_soft" to (c.gradeGold to goldSoftTile),
            "wrong on surface_card" to (c.wrong to c.surfaceCard)
        )
        pairs.forEach { (name, pair) ->
            val ratio = contrast(pair.first.compositeOver(pair.second), pair.second)
            assertTrue("$name is $ratio:1", ratio >= MIN_CONTRAST)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val MIN_CONTRAST = 4.5
    }
}
