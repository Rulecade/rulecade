package io.github.rulecade.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes
import io.github.rulecade.ui.theme.Spacing

// A pill tab; the selected one takes the accent.
@Composable
fun RcTab(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = RulecadeTheme.colors
    Box(
        Modifier
            .height(Sizes.touchMinimum)
            .background(if (selected) colors.accentSoft else colors.buttonBasicEnabled, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = Spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = RulecadeTheme.typography.label,
            color = if (selected) colors.accent else colors.textSecondary
        )
    }
}
