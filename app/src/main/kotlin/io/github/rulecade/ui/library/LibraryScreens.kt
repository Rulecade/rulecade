package io.github.rulecade.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.rulecade.R
import io.github.rulecade.library.Library
import io.github.rulecade.library.LibraryTab
import io.github.rulecade.library.Topic
import io.github.rulecade.quiz.License
import io.github.rulecade.ui.component.COMPACT_WIDTH
import io.github.rulecade.ui.component.RcButton
import io.github.rulecade.ui.component.RcButtonStyle
import io.github.rulecade.ui.component.RcTab
import io.github.rulecade.ui.home.label
import io.github.rulecade.ui.theme.Radius
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes
import io.github.rulecade.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val ROW_MIN_HEIGHT = 112.dp
private val NUMBER_SIZE = 48.dp
private const val LIST_WEIGHT = 1.6f
private const val ARTICLE_TEXT_WEIGHT = 1.1f
private const val CHANGES_SHOWN = 5

// Opens the library topic that explains a question; null when no topic covers it.
val LocalLibraryLink = compositionLocalOf<(String) -> (() -> Unit)?> { { null } }

val LibraryTab.label: Int
    get() = when (this) {
        LibraryTab.Highway -> License.Highway.label
        LibraryTab.Intersection -> License.Intersection.label
        LibraryTab.SchoolZone -> License.SchoolZone.label
        LibraryTab.City -> License.City.label
        LibraryTab.Penalty -> R.string.library_tab_penalty
    }

@Composable
internal fun dateText(iso: String): String {
    val pattern = stringResource(R.string.license_date_pattern)
    return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern(pattern))
}

@Composable
internal fun Page(compact: Boolean, content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier
        .fillMaxSize()
        .padding(
            if (compact) {
                PaddingValues(Spacing.extraLarge)
            } else {
                PaddingValues(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.screenVertical
                )
            }
        ),
    verticalArrangement = Arrangement.spacedBy(if (compact) Spacing.large else Spacing.extraLarge),
    content = content
)

@Composable
internal fun Header(title: String, onBack: () -> Unit, trailing: String? = null) = Row(
    Modifier.fillMaxWidth().height(Sizes.button),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
) {
    RcButton(
        text = stringResource(R.string.back),
        onClick = onBack,
        icon = R.drawable.ic_back,
        iconOnly = true
    )
    Text(title, style = RulecadeTheme.typography.titleSmall)
    Spacer(Modifier.weight(1f))
    if (trailing != null) {
        Text(
            trailing,
            style = RulecadeTheme.typography.bodyMedium,
            color = RulecadeTheme.colors.textTertiary
        )
    }
}

@Composable
private fun Badge(text: String) = Box(
    Modifier
        .background(RulecadeTheme.colors.surfaceTile, CircleShape)
        .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall / 2)
) {
    Text(text, style = RulecadeTheme.typography.label, color = RulecadeTheme.colors.textSecondary)
}

@Composable
private fun badgeText(topic: Topic): String? = topic.revised?.let {
    stringResource(R.string.library_badge, LocalDate.parse(it.date).year, it.mark)
}

// Topics of a tab, with how many of their questions are solved in ranked play; the most recent
// revisions sit beside the list (below it in a split window).
@Composable
fun LibraryScreen(
    library: Library,
    tab: LibraryTab?,
    onTab: (LibraryTab) -> Unit,
    solved: (Topic) -> Pair<Int, Int>,
    onOpen: (Topic) -> Unit,
    onBack: () -> Unit
) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxWidth < COMPACT_WIDTH
    // Tabs without topics yet stay hidden.
    val tabs = LibraryTab.entries.filter { library.byTab(it).isNotEmpty() }
    val current = tab?.takeIf { it in tabs } ?: tabs.firstOrNull()
    val topics = current?.let { library.byTab(it) }.orEmpty()
    Page(compact) {
        Header(
            stringResource(R.string.law_library),
            onBack,
            stringResource(R.string.library_checked, dateText(library.checked))
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            tabs.forEach { t -> RcTab(stringResource(t.label), t == current) { onTab(t) } }
        }
        if (compact) {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                items(topics, key = { it.id }) { TopicRow(it, solved(it)) { onOpen(it) } }
                item {
                    Changes(library, onOpen, Modifier.fillMaxWidth().padding(top = Spacing.medium))
                }
            }
        } else {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
            ) {
                LazyColumn(
                    Modifier.weight(LIST_WEIGHT),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    items(topics, key = { it.id }) { TopicRow(it, solved(it)) { onOpen(it) } }
                }
                Changes(library, onOpen, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun TopicRow(topic: Topic, solved: Pair<Int, Int>, onOpen: () -> Unit) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ROW_MIN_HEIGHT)
            .background(colors.surfaceCard, RoundedCornerShape(Radius.button))
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = Spacing.large, vertical = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Text(topic.title, style = type.titleSmall)
                badgeText(topic)?.let { Badge(it) }
            }
            Text(topic.summary, style = type.bodyMedium, color = colors.textTertiary)
        }
        Text(
            stringResource(R.string.library_quiz, solved.first, solved.second),
            style = type.label,
            color = colors.textSecondary
        )
        Icon(
            painterResource(R.drawable.ic_chevron),
            contentDescription = null,
            modifier = Modifier.size(Spacing.large),
            tint = colors.textTertiary
        )
    }
}

// The most recent revision dates the current text shows for the quoted provisions.
@Composable
private fun Changes(library: Library, onOpen: (Topic) -> Unit, modifier: Modifier) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Column(
        modifier
            .background(colors.surfaceCard, RoundedCornerShape(Radius.card))
            .padding(Spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Text(stringResource(R.string.library_changes), style = type.titleSmall)
        library.changes.take(CHANGES_SHOWN).forEach { topic ->
            val revision = topic.revised!!
            Column(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Sizes.touchMinimum)
                    .clickable(role = Role.Button) { onOpen(topic) },
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall / 2)
            ) {
                Text(
                    stringResource(
                        R.string.library_change_date,
                        dateText(revision.date),
                        revision.mark
                    ),
                    style = type.label,
                    color = colors.accent
                )
                Text(topic.title, style = type.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(revision.of, style = type.bodyMedium, color = colors.textTertiary)
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Text(
            stringResource(R.string.library_source, dateText(library.checked)),
            style = type.bodyMedium,
            color = colors.textTertiary
        )
    }
}

// One topic: summary, steps, the law text, what breaking it costs, and practice with its
// questions.
@Composable
fun TopicScreen(
    topic: Topic,
    checked: String,
    questionCount: Int,
    onPractice: () -> Unit,
    onBack: () -> Unit
) = BoxWithConstraints(Modifier.fillMaxSize().background(RulecadeTheme.colors.backgroundPrimary)) {
    val compact = maxWidth < COMPACT_WIDTH
    BackHandler(onBack = onBack)
    val practice = @Composable { modifier: Modifier ->
        RcButton(
            text = stringResource(R.string.library_practice, questionCount),
            onClick = onPractice,
            modifier = modifier,
            style = RcButtonStyle.Accent,
            large = true,
            icon = R.drawable.ic_pencil
        )
    }
    Page(compact) {
        Header(
            stringResource(R.string.library_topic_title, stringResource(topic.tab.label)),
            onBack
        )
        if (compact) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
            ) {
                Intro(topic)
                Steps(topic)
                LawAndFines(topic, checked)
            }
            practice(Modifier.fillMaxWidth())
        } else {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
            ) {
                Column(
                    Modifier.weight(
                        ARTICLE_TEXT_WEIGHT
                    ).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
                ) {
                    Intro(topic)
                    Steps(topic)
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        LawAndFines(topic, checked)
                    }
                    practice(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Intro(topic: Topic) =
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(topic.title, style = RulecadeTheme.typography.headlineSmall)
            badgeText(topic)?.let { Badge(it) }
        }
        Text(
            topic.summary,
            style = RulecadeTheme.typography.bodyLarge,
            color = RulecadeTheme.colors.textSecondary
        )
    }

@Composable
private fun Steps(topic: Topic) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(stringResource(R.string.library_steps), style = type.titleSmall)
        topic.steps.forEachIndexed { i, step ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                Box(
                    Modifier.size(NUMBER_SIZE).background(colors.accentSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", style = type.label, color = colors.accent)
                }
                Text(step, style = type.bodyLarge)
            }
        }
    }
}

@Composable
private fun LawAndFines(topic: Topic, checked: String) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    topic.law.forEach { quote ->
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surfaceTile, RoundedCornerShape(Radius.tile))
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(
                stringResource(R.string.library_law, quote.source),
                style = type.label,
                color = colors.textSecondary
            )
            Text("“${quote.text}”", style = type.bodyLarge)
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceCard, RoundedCornerShape(Radius.tile))
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Text(stringResource(R.string.library_fines), style = type.titleSmall)
        topic.fines.forEach { fine ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                Text(
                    fine.label,
                    Modifier.weight(1f),
                    style = type.bodyLarge,
                    color = colors.textSecondary
                )
                Text(fine.value, style = type.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
    Text(
        stringResource(R.string.library_source, dateText(checked)),
        style = type.bodyMedium,
        color = colors.textTertiary
    )
}

// "라이브러리에서 자세히 보기" under an explanation, when a topic covers the question.
@Composable
fun LibraryLink(questionId: String) {
    val open = LocalLibraryLink.current(questionId) ?: return
    Row(
        Modifier
            .defaultMinSize(minHeight = Sizes.touchMinimum)
            .clickable(role = Role.Button, onClick = open),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
    ) {
        Text(
            stringResource(R.string.library_link),
            style = RulecadeTheme.typography.label,
            color = RulecadeTheme.colors.accent
        )
        Icon(
            painterResource(R.drawable.ic_chevron),
            contentDescription = null,
            modifier = Modifier.size(Spacing.large),
            tint = RulecadeTheme.colors.accent
        )
    }
}
