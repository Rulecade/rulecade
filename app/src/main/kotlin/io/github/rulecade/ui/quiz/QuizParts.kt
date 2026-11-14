package io.github.rulecade.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.rulecade.R
import io.github.rulecade.progress.PlayMode
import io.github.rulecade.quiz.Answer
import io.github.rulecade.quiz.QuizItem
import io.github.rulecade.ui.component.RcButton
import io.github.rulecade.ui.component.RcButtonStyle
import io.github.rulecade.ui.component.RcProgressBar
import io.github.rulecade.ui.library.LibraryLink
import io.github.rulecade.ui.theme.Radius
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes
import io.github.rulecade.ui.theme.Spacing
import kotlin.math.roundToInt

// Pieces shared by the ranked (실전) and practice (연습) screens.

internal val CHIP_HEIGHT = 56.dp
private val BAND_HEIGHT = 72.dp
private val CARD_LINE = 4.dp
private val ROW_LINE = 3.dp
private val TOP_BAR_PROGRESS_WIDTH = 360.dp
private val TOP_BAR_PROGRESS_COMPACT_WIDTH = 120.dp
private val CHOICE_MIN_HEIGHT = 104.dp
private val SCENE_HEIGHT = 440.dp
private val SCENE_COMPACT_HEIGHT = 240.dp
private val ANSWER_ROW_MIN_HEIGHT = 80.dp
private val BADGE_SIZE = 56.dp
private val MARK_SIZE = 44.dp
private val POPUP_MIN_WIDTH = 720.dp
private val POPUP_MAX_WIDTH = 1040.dp
private val POPUP_PADDING = 56.dp
private const val DIM_ALPHA = 0.45f
private const val PROMPT_WEIGHT = 1f
private const val CHOICES_WEIGHT = 1.15f
private const val SUMMARY_WEIGHT = 1f
private const val DETAILS_WEIGHT = 1.25f

// Every ranked and practice screen starts with this band, so the mode is never in doubt.
@Composable
internal fun ModeBand(mode: PlayMode, compact: Boolean) {
    val colors = RulecadeTheme.colors
    val ranked = mode == PlayMode.Ranked
    Row(
        Modifier
            .fillMaxWidth()
            .height(BAND_HEIGHT)
            .background(colors.surfaceTile)
            .padding(horizontal = if (compact) Spacing.extraLarge else Spacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Icon(
            painterResource(if (ranked) R.drawable.ic_crown else R.drawable.ic_pencil),
            contentDescription = null,
            modifier = Modifier.size(Spacing.large),
            tint = if (ranked) colors.gradeGold else colors.accent
        )
        Text(
            stringResource(if (ranked) R.string.band_ranked else R.string.band_practice),
            style = RulecadeTheme.typography.label
        )
    }
}

@Composable
internal fun Screen(compact: Boolean, mode: PlayMode, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize()) {
        ModeBand(mode, compact)
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    if (compact) {
                        PaddingValues(Spacing.extraLarge)
                    } else {
                        PaddingValues(
                            start = Spacing.screenHorizontal,
                            end = Spacing.screenHorizontal,
                            top = Spacing.extraLarge,
                            bottom = Spacing.screenVertical
                        )
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(
                if (compact) Spacing.large else Spacing.extraLarge
            ),
            content = content
        )
    }

@Composable
internal fun TopBar(
    title: String,
    onBack: (() -> Unit)?,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().height(Sizes.button),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        if (onBack != null) {
            RcButton(
                text = stringResource(R.string.back),
                onClick = onBack,
                icon = R.drawable.ic_back,
                iconOnly = true
            )
        }
        Text(title, style = RulecadeTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
internal fun Chip(text: String, background: Color, content: Color, modifier: Modifier = Modifier) =
    Row(
        modifier
            .height(CHIP_HEIGHT)
            .background(background, CircleShape)
            .padding(horizontal = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = RulecadeTheme.typography.label, color = content)
    }

@Composable
internal fun Card(
    modifier: Modifier = Modifier,
    background: Color = RulecadeTheme.colors.surfaceCard,
    line: Color? = null,
    padding: PaddingValues = PaddingValues(Spacing.extraLarge),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(Radius.card)
    Column(
        modifier
            .background(background, shape)
            .then(if (line != null) Modifier.border(CARD_LINE, line, shape) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        content = content
    )
}

@Composable
internal fun Count(value: String, total: Int) = Text(
    buildAnnotatedString {
        append("$value ")
        withStyle(SpanStyle(color = RulecadeTheme.colors.textTertiary)) { append("/ $total") }
    },
    style = RulecadeTheme.typography.headlineSmall
)

// One question at a time. Practice shows the basis and fine right after an answer; ranked play
// keeps those for the result.
@Composable
internal fun RoundScreen(
    round: QuizRound,
    mode: PlayMode,
    title: String,
    compact: Boolean,
    onChoose: (Int) -> Unit,
    onNext: () -> Unit,
    onLeave: () -> Unit,
    confirm: @Composable (onLeave: () -> Unit, onStay: () -> Unit) -> Unit
) {
    val type = RulecadeTheme.typography
    var confirmLeave by remember(round) { mutableStateOf(false) }
    // Nothing is lost before the first answer, and a finished round is already saved.
    val tryLeave = {
        if (round.answered > 0 && !round.isFinished) confirmLeave = true else onLeave()
    }
    BackHandler(onBack = tryLeave)
    val item = round.item
    val answer = round.answer
    val details = mode == PlayMode.Practice
    Box(Modifier.fillMaxSize()) {
        Screen(compact, mode) {
            TopBar(title, tryLeave) {
                RcProgressBar(
                    round.answered.toFloat() / round.total,
                    Modifier.width(
                        if (compact) TOP_BAR_PROGRESS_COMPACT_WIDTH else TOP_BAR_PROGRESS_WIDTH
                    )
                )
            }
            // After an answer the feedback scrolls into view, but never past the picked or the
            // right choice; the next button stays below it.
            val scroll = rememberScrollState()
            val marks = remember(item) { ScrollMarks() }
            LaunchedEffect(answer) {
                if (answer == null) {
                    scroll.scrollTo(0)
                } else {
                    withFrameNanos { }
                    scroll.animateScrollTo(
                        marks.target(answer.choice, item.answer, scroll.maxValue)
                    )
                }
            }
            if (compact) {
                // The prompt sits above the choices.
                Column(
                    Modifier.weight(1f).onSizeChanged { marks.viewport = it.height }
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Card(Modifier.fillMaxWidth()) {
                        item.question.scene?.let {
                            RoadScene(it, Modifier.fillMaxWidth().height(SCENE_COMPACT_HEIGHT))
                        }
                        item.question.crossing?.let {
                            CrossingScene(it, Modifier.fillMaxWidth().height(SCENE_COMPACT_HEIGHT))
                        }
                        Text(item.question.prompt, style = type.headlineSmall)
                    }
                    Choices(item, answer, onChoose, marks)
                    if (answer != null) Feedback(round, answer, details, marks)
                }
                if (answer != null) NextButton(round, onNext, onLeave)
            } else {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
                ) {
                    Card(Modifier.weight(PROMPT_WEIGHT).fillMaxHeight()) {
                        item.question.scene?.let {
                            RoadScene(it, Modifier.fillMaxWidth().height(SCENE_HEIGHT))
                        }
                        item.question.crossing?.let {
                            CrossingScene(it, Modifier.fillMaxWidth().height(SCENE_HEIGHT))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(item.question.prompt, style = type.headlineSmall)
                        Spacer(Modifier.weight(1f))
                    }
                    Column(
                        Modifier.weight(CHOICES_WEIGHT).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.small)
                    ) {
                        Column(
                            Modifier.weight(1f).onSizeChanged { marks.viewport = it.height }
                                .verticalScroll(scroll),
                            verticalArrangement = Arrangement.spacedBy(Spacing.small)
                        ) {
                            Choices(item, answer, onChoose, marks)
                            if (answer != null) Feedback(round, answer, details, marks)
                        }
                        if (answer != null) NextButton(round, onNext, onLeave)
                    }
                }
            }
        }
        if (confirmLeave) confirm(onLeave) { confirmLeave = false }
    }
}

// Where the choices and the feedback sit in the scrolled column, in pixels from its top.
private class ScrollMarks {
    val choiceTops = mutableMapOf<Int, Int>()
    var feedbackBottom = 0
    var viewport = 0

    // Far enough to show the whole feedback, but the picked and the right choice stay in view.
    fun target(picked: Int, right: Int, max: Int): Int {
        val keep = minOf(choiceTops[picked] ?: 0, choiceTops[right] ?: 0)
        return (feedbackBottom - viewport).coerceIn(0, minOf(keep, max))
    }
}

@Composable
private fun Choices(item: QuizItem, answer: Answer?, onChoose: (Int) -> Unit, marks: ScrollMarks) {
    item.choices.forEachIndexed { index, text ->
        val state = when {
            answer == null -> ChoiceState.Open
            index == item.answer -> ChoiceState.Right
            index == answer.choice -> ChoiceState.Wrong
            else -> ChoiceState.Dim
        }
        Choice(
            index + 1,
            text,
            state,
            Modifier.onPlaced { marks.choiceTops[index] = it.positionInParent().y.roundToInt() }
        ) { onChoose(index) }
    }
}

@Composable
private fun Feedback(round: QuizRound, answer: Answer, details: Boolean, marks: ScrollMarks) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    val right = answer.correct
    val question = round.item.question
    Card(
        Modifier.fillMaxWidth().onPlaced {
            marks.feedbackBottom = (it.positionInParent().y + it.size.height).roundToInt()
        },
        background = if (right) colors.accentSoft else colors.wrongSoft
    ) {
        Text(
            when {
                !right -> stringResource(R.string.quiz_wrong)
                round.streak > 1 -> stringResource(R.string.quiz_correct_streak, round.streak)
                else -> stringResource(R.string.quiz_correct)
            },
            style = type.titleSmall,
            color = if (right) colors.accent else colors.wrong
        )
        Text(question.explanation, style = type.bodyLarge)
        if (details) {
            Source(question.basis, question.penalty)
            LibraryLink(question.id)
        }
    }
}

@Composable
private fun Source(basis: String, penalty: String?) = Text(
    listOfNotNull(basis, penalty).joinToString(" · "),
    style = RulecadeTheme.typography.bodyMedium,
    color = RulecadeTheme.colors.textTertiary
)

@Composable
private fun NextButton(round: QuizRound, onNext: () -> Unit, onLeave: () -> Unit) = RcButton(
    text = stringResource(if (round.isFinished) R.string.quiz_see_result else R.string.quiz_next),
    onClick = if (round.isFinished) onLeave else onNext,
    modifier = Modifier.fillMaxWidth(),
    style = RcButtonStyle.Filled,
    large = true
)

private enum class ChoiceState { Open, Right, Wrong, Dim }

@Composable
private fun Choice(
    number: Int,
    text: String,
    state: ChoiceState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = RulecadeTheme.colors
    val shape = RoundedCornerShape(Radius.button)
    val (background, line, badge, onBadge) = when (state) {
        ChoiceState.Right -> listOf(
            colors.accentSoft,
            colors.accent,
            colors.accent,
            colors.onAccent
        )
        ChoiceState.Wrong -> listOf(colors.wrongSoft, colors.wrong, colors.wrong, Color.White)
        else -> listOf(colors.buttonBasicEnabled, null, colors.surfaceAccent, colors.textPrimary)
    }
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = CHOICE_MIN_HEIGHT)
            .alpha(if (state == ChoiceState.Dim) DIM_ALPHA else 1f)
            .background(background!!, shape)
            .then(if (line != null) Modifier.border(CARD_LINE, line, shape) else Modifier)
            .clickable(enabled = state == ChoiceState.Open, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.large, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Box(
            Modifier.size(BADGE_SIZE).background(badge!!, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (state) {
                    ChoiceState.Right -> "✓"
                    ChoiceState.Wrong -> "✗"
                    else -> "$number"
                },
                style = RulecadeTheme.typography.label,
                color = onBadge!!
            )
        }
        Text(text, style = RulecadeTheme.typography.titleSmall)
    }
}

@Composable
internal fun LeaveDialog(
    title: String,
    body: String?,
    leave: String,
    stay: String,
    onLeave: () -> Unit,
    onStay: () -> Unit
) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    BackHandler(onBack = onStay)
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.dim)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(min = POPUP_MIN_WIDTH, max = POPUP_MAX_WIDTH)
                .background(colors.backgroundPopup, RoundedCornerShape(Radius.popup))
                .padding(POPUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
        ) {
            Text(title, style = type.headlineSmall)
            if (body != null) {
                Text(body, style = type.bodyLarge, color = colors.textSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                RcButton(
                    text = leave,
                    onClick = onLeave,
                    modifier = Modifier.weight(1f),
                    large = true
                )
                RcButton(
                    text = stay,
                    onClick = onStay,
                    modifier = Modifier.weight(1f),
                    style = RcButtonStyle.Accent,
                    large = true
                )
            }
        }
    }
}

// A finished round: the summary with its buttons, and every answer to open for details.
@Composable
internal fun ResultLayout(
    mode: PlayMode,
    title: String,
    compact: Boolean,
    answers: List<Answer>,
    onBack: () -> Unit,
    summary: @Composable ColumnScope.() -> Unit,
    buttons: @Composable (Modifier) -> Unit,
    extra: (@Composable (Modifier) -> Unit)? = null
) = Screen(compact, mode) {
    BackHandler(onBack = onBack)
    TopBar(title, onBack = null)
    if (compact) {
        // The buttons stay in view below the scrolling summary and answers.
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Card(Modifier.fillMaxWidth(), content = summary)
            answers.forEach { AnswerDetail(it) }
        }
        extra?.invoke(Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
            buttons(Modifier.weight(1f))
        }
    } else {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Spacing.extraLarge)) {
            Card(Modifier.weight(SUMMARY_WEIGHT).fillMaxHeight()) {
                summary()
                Spacer(Modifier.weight(1f))
                extra?.invoke(Modifier.fillMaxWidth())
                buttons(Modifier.fillMaxWidth())
            }
            Column(
                Modifier
                    .weight(DETAILS_WEIGHT)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                answers.forEach { AnswerDetail(it) }
            }
        }
    }
}

@Composable
internal fun ResultScore(correct: Int, total: Int) = Text(
    stringResource(R.string.quiz_result_score, correct, total),
    style = RulecadeTheme.typography.headlineLarge
)

@Composable
internal fun HomeAndOneMore(onHome: () -> Unit, onOneMore: () -> Unit, modifier: Modifier) {
    RcButton(
        text = stringResource(R.string.quiz_home),
        onClick = onHome,
        modifier = modifier,
        large = true
    )
    RcButton(
        text = stringResource(R.string.quiz_one_more),
        onClick = onOneMore,
        modifier = modifier,
        style = RcButtonStyle.Accent,
        large = true
    )
}

// Wrong answers open by default; any row opens to show the explanation and its source.
@Composable
private fun AnswerDetail(answer: Answer) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    val question = answer.item.question
    var open by remember(answer) { mutableStateOf(!answer.correct) }
    val shape = RoundedCornerShape(Radius.button)
    Column(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ANSWER_ROW_MIN_HEIGHT)
            .background(colors.surfaceCard, shape)
            .then(if (!answer.correct) Modifier.border(ROW_LINE, colors.wrong, shape) else Modifier)
            .clickable(role = Role.Button) { open = !open }
            .padding(horizontal = Spacing.large, vertical = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Box(
                Modifier
                    .size(MARK_SIZE)
                    .background(
                        if (answer.correct) colors.accentSoft else colors.wrongSoft,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (answer.correct) "✓" else "✗",
                    style = type.label,
                    color = if (answer.correct) colors.accent else colors.wrong
                )
            }
            Text(question.prompt, style = type.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
        if (open) {
            val rightText = question.choices[question.answer]
            Text(
                buildAnnotatedString {
                    if (!answer.correct) {
                        append(
                            stringResource(
                                R.string.quiz_my_answer,
                                answer.item.choices[answer.choice]
                            )
                        )
                        append(" · ")
                    }
                    withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Bold)) {
                        append(stringResource(R.string.quiz_right_answer, rightText))
                    }
                },
                style = type.bodyLarge
            )
            Text(question.explanation, style = type.bodyMedium, color = colors.textSecondary)
            Source(question.basis, question.penalty)
            LibraryLink(question.id)
        }
    }
}
