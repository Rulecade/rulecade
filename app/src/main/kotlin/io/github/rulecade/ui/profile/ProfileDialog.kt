package io.github.rulecade.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.rulecade.R
import io.github.rulecade.progress.SaveData
import io.github.rulecade.ui.component.RcButton
import io.github.rulecade.ui.component.RcButtonStyle
import io.github.rulecade.ui.theme.Radius
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Spacing

private val POPUP_WIDTH = 760.dp
private val POPUP_PADDING = 56.dp
private val TILE_WIDTH = 176.dp
private val AVATAR = 88.dp
private val LINE = 3.dp

// One profile as the picker shows it.
data class ProfileSummary(val id: String, val name: String, val licenses: Int)

// "누가 풀까요?": pick who plays, or add a profile. With someone in the passenger seat it says
// so and invites them to play on their own profile.
@Composable
fun ProfileDialog(
    profiles: List<ProfileSummary>,
    active: String,
    passenger: Boolean,
    canAdd: Boolean,
    onPick: (String) -> Unit,
    onAdd: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    var adding by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (adding) adding = false else onClose() }
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.dim)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = {}),
        // While typing a name, the popup sits at the top: the Pleos keyboard's window takes
        // touches a little above the keys and would swallow the buttons of a centered popup.
        contentAlignment = if (adding) Alignment.TopCenter else Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(max = POPUP_WIDTH)
                .padding(Spacing.large)
                .background(colors.backgroundPopup, RoundedCornerShape(Radius.popup))
                .padding(POPUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
        ) {
            if (adding) {
                NewProfile(
                    onCreate = { name ->
                        adding = false
                        onAdd(name)
                    },
                    onCancel = { adding = false }
                )
                return@Column
            }
            Text(stringResource(R.string.profile_title), style = type.headlineSmall)
            if (passenger) {
                Text(
                    stringResource(R.string.profile_passenger),
                    style = type.bodyLarge,
                    color = colors.accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.accentSoft, RoundedCornerShape(Radius.button))
                        .padding(Spacing.medium)
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                profiles.forEach { profile ->
                    Tile(
                        avatar = profile.name.take(1),
                        name = profile.name,
                        note = stringResource(R.string.profile_licenses, profile.licenses),
                        selected = profile.id == active,
                        onClick = { onPick(profile.id) }
                    )
                }
                if (canAdd) {
                    Tile(
                        avatar = "+",
                        name = stringResource(R.string.profile_new),
                        note = "",
                        selected = false,
                        onClick = { adding = true }
                    )
                }
            }
            RcButton(
                text = stringResource(R.string.profile_close),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                large = true
            )
        }
    }
}

@Composable
private fun Tile(
    avatar: String,
    name: String,
    note: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    val shape = RoundedCornerShape(Radius.card)
    Column(
        Modifier
            .width(TILE_WIDTH)
            .background(if (selected) colors.accentSoft else colors.buttonBasicEnabled, shape)
            .border(LINE, if (selected) colors.accent else Color.Transparent, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
    ) {
        Box(
            Modifier.size(AVATAR).background(colors.surfaceAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, style = type.titleLarge, color = colors.accent)
        }
        Text(name, style = type.titleSmall, maxLines = 1)
        Text(note, style = type.bodyMedium, color = colors.textTertiary)
    }
}

// A name typed while parked, up to six characters.
@Composable
private fun NewProfile(onCreate: (String) -> Unit, onCancel: () -> Unit) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    var name by rememberSaveable { mutableStateOf("") }
    val ready = name.isNotBlank()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.large)) {
        Text(stringResource(R.string.profile_new), style = type.headlineSmall)
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.surfaceTile, RoundedCornerShape(Radius.button))
                .padding(horizontal = Spacing.large, vertical = Spacing.medium)
        ) {
            if (name.isEmpty()) {
                Text(
                    stringResource(R.string.profile_name_hint, SaveData.MAX_NAME),
                    style = type.bodyLarge,
                    color = colors.textTertiary
                )
            }
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(SaveData.MAX_NAME) },
                singleLine = true,
                textStyle = type.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (ready) onCreate(name) }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            RcButton(
                text = stringResource(R.string.profile_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                large = true
            )
            RcButton(
                text = stringResource(R.string.profile_create),
                onClick = { onCreate(name) },
                modifier = Modifier.weight(1f),
                style = RcButtonStyle.Accent,
                large = true,
                enabled = ready
            )
        }
    }
}
