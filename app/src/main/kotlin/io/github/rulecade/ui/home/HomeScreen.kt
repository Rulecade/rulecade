package io.github.rulecade.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.rulecade.Destination
import io.github.rulecade.R
import io.github.rulecade.catalog.Sign
import io.github.rulecade.progress.Progress
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.QuestionBank
import io.github.rulecade.ui.component.COMPACT_WIDTH
import io.github.rulecade.ui.component.PixelWordmark
import io.github.rulecade.ui.component.RcButton
import io.github.rulecade.ui.component.RcButtonStyle
import io.github.rulecade.ui.component.RcProgressBar
import io.github.rulecade.ui.license.RoadSign
import io.github.rulecade.ui.license.RoadSignImage
import io.github.rulecade.ui.quiz.CHARGING_COUNT
import io.github.rulecade.ui.quiz.Chip
import io.github.rulecade.ui.theme.Radius
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes
import io.github.rulecade.ui.theme.Spacing

data class CourseSummary(
    val license: License,
    val total: Int,
    val solved: Int = 0,
    val licensed: Boolean = false
) {
    val share: Float get() = if (total == 0) 0f else solved.toFloat() / total
}

fun courseSummaries(bank: QuestionBank, progress: Progress) = License.entries.map { license ->
    val questions = bank.byLicense(license)
    CourseSummary(
        license,
        questions.size,
        progress.solvedCount(questions),
        license in progress.licenses
    )
}

private const val START_CARD_WEIGHT = 1.25f
private val RECENT_TILE = 112.dp
private val RECENT_TILE_COMPACT = 64.dp
private const val RECENT_SIGN_SHARE = 0.75f
private val PROFILE_AVATAR = 48.dp

val License.label: Int
    get() = when (this) {
        License.Highway -> R.string.license_highway
        License.Intersection -> R.string.license_intersection
        License.SchoolZone -> R.string.license_school_zone
        License.City -> R.string.license_city
    }

@Composable
fun HomeScreen(
    courses: List<CourseSummary>,
    notebookCount: Int,
    onPractice: () -> Unit,
    onRanked: () -> Unit,
    onNotebook: () -> Unit,
    onCourses: () -> Unit,
    onOpen: (Destination) -> Unit,
    onOpenLicense: (License) -> Unit,
    recent: List<Sign>,
    onOpenSign: (Sign) -> Unit,
    onRecentShown: () -> Unit,
    profileName: String,
    onProfiles: () -> Unit,
    charging: Boolean,
    onCharging: () -> Unit,
    modifier: Modifier = Modifier,
    wordmarkModifier: Modifier = Modifier
) {
    // Once the player has seen the recent signs, the next one found on the road starts a new
    // list. On the road Pleos covers the app while it stays resumed and focused, so seeing is
    // a touch on home: the cover blocks touches. While charging the card offers the ten-minute
    // round instead, so the signs are not shown and not seen.
    val showRecent = recent.isNotEmpty() && !charging
    val seen = if (!showRecent) {
        Modifier
    } else {
        Modifier.pointerInput(recent) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                onRecentShown()
            }
        }
    }
    BoxWithConstraints(modifier.fillMaxSize().then(seen)) {
        val compact = maxWidth < COMPACT_WIDTH
        val padding = if (compact) {
            Modifier.padding(Spacing.extraLarge)
        } else {
            Modifier.padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.screenVertical
            )
        }
        // In a split window everything fits as is; the scroll only guards against overflow.
        val scroll = if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier
        val start = StartActions(
            notebookCount,
            courses.sumOf { it.total },
            if (showRecent) recent else emptyList(),
            charging,
            onCharging,
            onOpenSign,
            onPractice,
            onRanked,
            onNotebook,
            onCourses
        )
        Column(
            padding.fillMaxSize().then(scroll),
            verticalArrangement = Arrangement.spacedBy(
                if (compact) Spacing.small else Spacing.extraLarge
            )
        ) {
            // Same height as a button so the wordmark stays put when top bar actions arrive; a
            // split window is short on height, so there it is a touch target's height.
            Row(
                Modifier.height(if (compact) Sizes.touchMinimum else Sizes.button),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelWordmark(wordmarkModifier)
                Spacer(Modifier.weight(1f))
                ProfileChip(profileName, onProfiles)
            }
            if (compact) {
                StartCard(start, Modifier.fillMaxWidth(), compact = true)
                LicenseCard(courses, onOpenLicense, Modifier.fillMaxWidth(), expanded = false)
            } else {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
                ) {
                    StartCard(start, Modifier.weight(START_CARD_WEIGHT).fillMaxHeight())
                    LicenseCard(courses, onOpenLicense, Modifier.weight(1f).fillMaxHeight())
                }
            }
            MenuButtons(onOpen, compact)
        }
    }
}

private class StartActions(
    val notebookCount: Int,
    val questionCount: Int,
    val recent: List<Sign>,
    val charging: Boolean,
    val onCharging: () -> Unit,
    val onOpenSign: (Sign) -> Unit,
    val onPractice: () -> Unit,
    val onRanked: () -> Unit,
    val onNotebook: () -> Unit,
    val onCourses: () -> Unit
)

@Composable
private fun Card(modifier: Modifier, spacing: Dp, content: @Composable ColumnScope.() -> Unit) =
    Column(
        modifier
            .background(RulecadeTheme.colors.surfaceCard, RoundedCornerShape(Radius.card))
            .padding(Spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )

// Practice (연습) and ranked play (실전) side by side, told apart by icon and caption. A split
// window drops the round length and puts the two links in one row. While the car charges, the
// card leads with the ten-minute round.
@Composable
private fun StartCard(actions: StartActions, modifier: Modifier, compact: Boolean = false) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Card(modifier, if (compact) Spacing.medium else Spacing.large) {
        when {
            actions.charging -> {
                Text(stringResource(R.string.home_charging_title), style = type.headlineSmall)
                if (!compact) {
                    Text(
                        stringResource(R.string.home_charging_body, CHARGING_COUNT),
                        style = type.bodyLarge,
                        color = colors.textTertiary
                    )
                }
                ChargingButton(actions.onCharging)
            }
            actions.recent.isNotEmpty() -> RecentSigns(actions.recent, actions.onOpenSign, compact)
            else -> {
                Text(stringResource(R.string.home_start_title), style = type.headlineSmall)
                if (!compact) {
                    Text(
                        stringResource(R.string.home_start_body),
                        style = type.bodyLarge,
                        color = colors.textTertiary
                    )
                }
            }
        }
        val notebook = @Composable { linkModifier: Modifier ->
            RowLink(
                R.drawable.ic_quiz,
                stringResource(R.string.home_notebook),
                stringResource(R.string.home_count, actions.notebookCount),
                actions.onNotebook,
                linkModifier,
                showIcon = !compact
            )
        }
        val courses = @Composable { linkModifier: Modifier ->
            RowLink(
                R.drawable.ic_book,
                stringResource(R.string.home_courses),
                stringResource(R.string.home_count, actions.questionCount),
                actions.onCourses,
                linkModifier,
                showIcon = !compact
            )
        }
        // A split window has no room for the links beside the charging round; they come back
        // when charging ends or the window widens.
        if (compact) {
            if (!actions.charging) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    notebook(Modifier.weight(1f))
                    courses(Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                notebook(Modifier.fillMaxWidth())
                courses(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            ModeButton(
                R.drawable.ic_pencil,
                colors.accent,
                stringResource(R.string.practice),
                stringResource(R.string.practice_sub),
                actions.onPractice,
                Modifier.weight(1f)
            )
            ModeButton(
                R.drawable.ic_crown,
                colors.gradeGold,
                stringResource(R.string.ranked),
                stringResource(R.string.ranked_sub),
                actions.onRanked,
                Modifier.weight(1f)
            )
        }
    }
}

// The card's main action while the car charges: a ten-minute practice round, kept like any
// practice so leaving when charging ends loses nothing.
@Composable
private fun ChargingButton(onClick: () -> Unit) =
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        RcButton(
            text = stringResource(R.string.home_charging_start),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            style = RcButtonStyle.Accent,
            large = true,
            icon = R.drawable.ic_bolt
        )
        Text(
            stringResource(R.string.practice_sub),
            modifier = Modifier.padding(top = Spacing.extraSmall),
            style = RulecadeTheme.typography.bodyMedium,
            color = RulecadeTheme.colors.textTertiary
        )
    }

// Who is playing; opens the profile picker.
@Composable
private fun ProfileChip(name: String, onClick: () -> Unit) {
    val colors = RulecadeTheme.colors
    Row(
        Modifier
            .height(Sizes.touchMinimum)
            .clip(RoundedCornerShape(Radius.button))
            .background(colors.buttonBasicEnabled)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = Spacing.extraSmall, end = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Box(
            Modifier.size(PROFILE_AVATAR).background(colors.accentSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), style = RulecadeTheme.typography.label, color = colors.accent)
        }
        Text(name, style = RulecadeTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
    }
}

// Signs found on the road since home last showed them; each opens its catalog page.
@Composable
private fun RecentSigns(signs: List<Sign>, onOpen: (Sign) -> Unit, compact: Boolean) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    val tile = if (compact) RECENT_TILE_COMPACT else RECENT_TILE
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        // A split window has the height of the title it replaces: one line, no chip.
        if (compact) {
            Text(
                stringResource(R.string.home_recent_title_compact, signs.size),
                style = type.titleSmall
            )
        } else {
            Chip(stringResource(R.string.home_recent_chip), colors.accentSoft, colors.accent)
            Text(stringResource(R.string.home_recent_title, signs.size), style = type.headlineSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            signs.forEach { sign ->
                val drawn = RoadSign.of(sign.number) ?: return@forEach
                Box(
                    Modifier
                        .size(tile)
                        .clip(RoundedCornerShape(Radius.button))
                        .background(colors.surfaceTile)
                        .clickable(role = Role.Button) { onOpen(sign) }
                        .semantics { contentDescription = sign.name },
                    contentAlignment = Alignment.Center
                ) {
                    RoadSignImage(drawn, Modifier.size(tile * RECENT_SIGN_SHARE))
                }
            }
        }
    }
}

@Composable
private fun RowLink(
    @DrawableRes icon: Int,
    label: String,
    count: String,
    onClick: () -> Unit,
    modifier: Modifier,
    showIcon: Boolean
) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Row(
        modifier
            .defaultMinSize(minHeight = Sizes.button)
            .clip(RoundedCornerShape(Radius.button))
            .background(colors.buttonBasicEnabled)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        if (showIcon) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(Sizes.icon),
                tint = colors.textPrimary
            )
        }
        Text(label, style = type.bodyLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.weight(1f))
        Text(count, style = type.bodyLarge, color = colors.textTertiary)
        Icon(
            painterResource(R.drawable.ic_chevron),
            contentDescription = null,
            modifier = Modifier.size(Spacing.large),
            tint = colors.textTertiary
        )
    }
}

// Both modes look alike (neutral with a green outline); the icon and caption tell them apart.
@Composable
private fun ModeButton(
    @DrawableRes icon: Int,
    iconTint: Color,
    label: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier
) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    RcButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        large = true,
        icon = icon,
        iconTint = iconTint,
        outline = RulecadeTheme.colors.accent
    )
    Text(
        caption,
        modifier = Modifier.padding(top = Spacing.extraSmall),
        style = RulecadeTheme.typography.bodyMedium,
        color = RulecadeTheme.colors.textTertiary
    )
}

@Composable
private fun LicenseCard(
    courses: List<CourseSummary>,
    onOpenLicense: (License) -> Unit,
    modifier: Modifier,
    expanded: Boolean = true
) {
    val type = RulecadeTheme.typography
    Card(modifier, Spacing.medium) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Icon(
                painterResource(R.drawable.ic_crown),
                contentDescription = null,
                modifier = Modifier.size(Sizes.icon),
                tint = RulecadeTheme.colors.gradeGold
            )
            Text(stringResource(R.string.home_licenses), style = type.titleSmall)
        }
        Column(
            if (expanded) Modifier.weight(1f) else Modifier,
            verticalArrangement = if (expanded) {
                Arrangement.SpaceAround
            } else {
                Arrangement.spacedBy(Spacing.small)
            }
        ) {
            courses.forEach { CourseRow(it, onOpenLicense) }
        }
    }
}

@Composable
private fun CourseRow(course: CourseSummary, onOpenLicense: (License) -> Unit) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    // An earned course opens its license card again.
    val open = if (course.licensed) {
        Modifier
            .defaultMinSize(minHeight = Sizes.touchMinimum)
            .clickable(role = Role.Button) { onOpenLicense(course.license) }
    } else {
        Modifier
    }
    Column(open, verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(course.license.label), style = type.bodyLarge)
            if (course.licensed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_licensed),
                        style = type.label,
                        color = colors.accent
                    )
                    Icon(
                        painterResource(R.drawable.ic_chevron),
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.large),
                        tint = colors.accent
                    )
                }
            } else {
                Text(
                    stringResource(R.string.home_progress, course.solved, course.total),
                    style = type.bodyLarge,
                    color = colors.textTertiary
                )
            }
        }
        RcProgressBar(course.share, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MenuButtons(onOpen: (Destination) -> Unit, compact: Boolean) {
    val items = listOf(
        Triple(
            Destination.Library,
            R.drawable.ic_book,
            if (compact) R.string.law_library_short else R.string.law_library
        ),
        Triple(
            Destination.Catalog,
            R.drawable.ic_sign,
            if (compact) R.string.sign_catalog_short else R.string.sign_catalog
        )
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (compact) Spacing.small else Spacing.medium)
    ) {
        items.forEach { (destination, icon, label) ->
            RcButton(
                text = stringResource(label),
                onClick = { onOpen(destination) },
                modifier = Modifier.weight(1f),
                large = !compact,
                icon = icon,
                stacked = compact
            )
        }
    }
}
