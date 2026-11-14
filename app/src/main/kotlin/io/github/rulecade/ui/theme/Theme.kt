package io.github.rulecade.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalRcColors = staticCompositionLocalOf { DarkColors }
private val LocalRcTypography = staticCompositionLocalOf { RulecadeTypography }

@Composable
fun RulecadeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(
        LocalRcColors provides colors,
        LocalRcTypography provides RulecadeTypography,
        LocalContentColor provides colors.textPrimary,
        LocalTextStyle provides RulecadeTypography.bodyLarge,
        content = content
    )
}

object RulecadeTheme {
    val colors: RcColors
        @Composable @ReadOnlyComposable
        get() = LocalRcColors.current

    val typography: RcTypography
        @Composable @ReadOnlyComposable
        get() = LocalRcTypography.current
}
