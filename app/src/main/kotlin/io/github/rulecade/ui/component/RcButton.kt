package io.github.rulecade.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.rulecade.ui.theme.Motion
import io.github.rulecade.ui.theme.Radius
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes
import io.github.rulecade.ui.theme.Spacing

enum class RcButtonStyle { Basic, Filled, Accent }

private val OUTLINE_WIDTH = 3.dp

private const val DISABLED_ALPHA = 0.4f

// Pleos button interaction: scale to 95% and switch to the pressed color while touched.
@Composable
fun RcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RcButtonStyle = RcButtonStyle.Basic,
    large: Boolean = false,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    iconOnly: Boolean = false,
    stacked: Boolean = false,
    iconTint: Color? = null,
    outline: Color? = null
) {
    val colors = RulecadeTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PRESS_SCALE else 1f,
        animationSpec = tween(Motion.PRESS_DURATION_MILLIS),
        label = "press"
    )
    val (enabledColor, pressedColor, content) = when (style) {
        RcButtonStyle.Basic ->
            Triple(colors.buttonBasicEnabled, colors.buttonBasicPressed, colors.textPrimary)
        RcButtonStyle.Filled ->
            Triple(colors.buttonFilledEnabled, colors.buttonFilledPressed, colors.onButtonFilled)
        RcButtonStyle.Accent -> Triple(colors.accent, colors.accentPressed, colors.onAccent)
    }
    val container = if (pressed) pressedColor else enabledColor
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else DISABLED_ALPHA
            }
            .defaultMinSize(
                minWidth = Sizes.touchMinimum,
                minHeight = if (large) Sizes.buttonLarge else Sizes.button
            )
            .clip(RoundedCornerShape(if (large) Radius.buttonLarge else Radius.button))
            .background(container)
            .then(
                if (outline != null) {
                    Modifier.border(
                        OUTLINE_WIDTH,
                        outline,
                        RoundedCornerShape(if (large) Radius.buttonLarge else Radius.button)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = if (iconOnly) Spacing.medium else Spacing.extraLarge),
        contentAlignment = Alignment.Center
    ) {
        val type = RulecadeTheme.typography
        val label = @Composable {
            if (icon != null) {
                Icon(
                    painterResource(icon),
                    contentDescription = if (iconOnly) text else null,
                    modifier = Modifier.size(Sizes.icon),
                    tint = iconTint ?: content
                )
            }
            if (!iconOnly) {
                Text(
                    text = text,
                    color = content,
                    style = if (large) type.buttonLarge else type.button,
                    maxLines = 1
                )
            }
        }
        if (stacked) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) { label() }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) { label() }
        }
    }
}
