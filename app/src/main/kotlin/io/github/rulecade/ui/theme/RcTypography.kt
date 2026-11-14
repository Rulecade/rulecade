package io.github.rulecade.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import io.github.rulecade.R

@Immutable
data class RcTypography(
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val label: TextStyle,
    val button: TextStyle,
    val buttonLarge: TextStyle
)

// Asta Sans is the Pleos Connect system UI font, bundled as one variable font (SIL OFL 1.1).
@OptIn(ExperimentalTextApi::class)
private val AstaSans = FontFamily(
    listOf(400, 700, 800).map { weight ->
        Font(
            resId = R.font.asta_sans,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight))
        )
    }
)

// Korean breaks between words (phrases), not in the middle of one.
private val KoreanLineBreak = LineBreak(
    strategy = LineBreak.Strategy.Simple,
    strictness = LineBreak.Strictness.Normal,
    wordBreak = LineBreak.WordBreak.Phrase
)

internal fun textStyle(size: Int, lineHeight: Int, weight: Int) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    lineBreak = KoreanLineBreak
)
