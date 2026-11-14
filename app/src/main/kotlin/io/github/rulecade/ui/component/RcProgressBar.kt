package io.github.rulecade.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rulecade.ui.theme.RulecadeTheme

private val BAR_HEIGHT = 12.dp

@Composable
fun RcProgressBar(share: Float, modifier: Modifier = Modifier) {
    val colors = RulecadeTheme.colors
    Box(
        modifier
            .height(BAR_HEIGHT)
            .background(colors.surfaceAccent, CircleShape)
    ) {
        if (share > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(share.coerceAtMost(1f))
                    .fillMaxHeight()
                    .background(colors.accent, CircleShape)
            )
        }
    }
}
