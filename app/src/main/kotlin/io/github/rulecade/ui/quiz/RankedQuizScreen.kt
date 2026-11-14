package io.github.rulecade.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.rulecade.R
import io.github.rulecade.progress.PlayMode
import io.github.rulecade.quiz.Answer
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.QuizSession
import io.github.rulecade.ui.component.COMPACT_WIDTH
import io.github.rulecade.ui.component.RcButton
import io.github.rulecade.ui.component.RcButtonStyle
import io.github.rulecade.ui.component.RcProgressBar
import io.github.rulecade.ui.home.CourseSummary
import io.github.rulecade.ui.home.label
import io.github.rulecade.ui.license.AwardStats
import io.github.rulecade.ui.license.CardSign
import io.github.rulecade.ui.license.LicenseAwardScreen
import io.github.rulecade.ui.license.earnedText
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Sizes
import io.github.rulecade.ui.theme.Spacing

private val ACTION_MIN_WIDTH = 220.dp

@Composable
fun RankedQuizScreen(
    quiz: RankedQuiz,
    courses: List<CourseSummary>,
    owner: String,
    cardSigns: (License) -> List<CardSign>,
    onHome: () -> Unit,
    onPracticeMissed: (List<Answer>) -> Unit
) = BoxWithConstraints(Modifier.fillMaxSize()) {
    RankedQuizContent(
        quiz,
        courses,
        owner,
        cardSigns,
        onHome,
        onPracticeMissed,
        compact = maxWidth < COMPACT_WIDTH
    )
}

@Composable
private fun RankedQuizContent(
    quiz: RankedQuiz,
    courses: List<CourseSummary>,
    owner: String,
    cardSigns: (License) -> List<CardSign>,
    onHome: () -> Unit,
    onPracticeMissed: (List<Answer>) -> Unit,
    compact: Boolean
) {
    val round = quiz.round
    val result = quiz.result
    val award = quiz.award
    val earned = award?.let { quiz.earnedDate(it) }
    when {
        round != null -> RoundScreen(
            round = round,
            mode = PlayMode.Ranked,
            title = stringResource(
                R.string.quiz_round_title,
                stringResource(round.license.label),
                round.index + 1,
                round.total
            ),
            compact = compact,
            onChoose = quiz::choose,
            onNext = quiz::next,
            onLeave = quiz::leave
        ) { onLeave, onStay ->
            LeaveDialog(
                title = stringResource(R.string.quiz_leave_title),
                body = stringResource(R.string.quiz_leave_body, round.answered),
                leave = stringResource(R.string.quiz_leave),
                stay = stringResource(R.string.quiz_keep_going),
                onLeave = onLeave,
                onStay = onStay
            )
        }
        award != null && result != null && earned != null -> LicenseAwardScreen(
            license = award,
            owner = owner,
            earned = earnedText(earned),
            signs = cardSigns(award),
            stats = AwardStats(
                solved = result.solvedAfter,
                total = result.total,
                accuracy = quiz.accuracy(award),
                next = courses.filter {
                    !it.licensed && it.license != award
                }.maxByOrNull { it.share }
            ),
            onResult = quiz::dismissAward,
            onHome = {
                quiz.close()
                onHome()
            }
        )
        result != null -> RankedResultScreen(
            result = result,
            onHome = {
                quiz.close()
                onHome()
            },
            onOneMore = { quiz.start(result.license) },
            onPracticeMissed = {
                quiz.close()
                onPracticeMissed(result.answers)
            },
            onBack = quiz::close,
            compact = compact
        )
        else -> CourseSelectScreen(courses, quiz::start, onHome, compact)
    }
}

@Composable
private fun CourseSelectScreen(
    courses: List<CourseSummary>,
    onStart: (License) -> Unit,
    onBack: () -> Unit,
    compact: Boolean
) = Screen(compact, PlayMode.Ranked) {
    TopBar(stringResource(R.string.ranked_course_title), onBack)
    Text(
        stringResource(R.string.quiz_ranked_intro, QuizSession.DEFAULT_COUNT),
        style = RulecadeTheme.typography.bodyLarge,
        color = RulecadeTheme.colors.textTertiary
    )
    if (compact) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            courses.forEach { course -> CourseRow(course) { onStart(course.license) } }
        }
    } else {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            courses.chunked(2).forEach { row ->
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    row.forEach { course ->
                        CourseCard(course, {
                            onStart(course.license)
                        }, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun remainingText(course: CourseSummary): String {
    val remaining = course.total - course.solved
    return if (remaining == 0) {
        stringResource(R.string.quiz_all_solved)
    } else {
        stringResource(R.string.quiz_remaining, remaining)
    }
}

@Composable
private fun StartButton(course: CourseSummary, onStart: () -> Unit, large: Boolean) {
    val replay = course.solved == course.total
    RcButton(
        text = stringResource(if (replay) R.string.quiz_replay_course else R.string.quiz_start),
        onClick = onStart,
        modifier = Modifier.widthIn(min = ACTION_MIN_WIDTH),
        style = if (replay) RcButtonStyle.Basic else RcButtonStyle.Accent,
        large = large
    )
}

// One course per row, for a split window.
@Composable
private fun CourseRow(course: CourseSummary, onStart: () -> Unit) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Card(
        Modifier.fillMaxWidth(),
        line = if (course.licensed) colors.accent else null,
        padding = PaddingValues(Spacing.large)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                Row(
                    Modifier.height(CHIP_HEIGHT),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Text(stringResource(course.license.label), style = type.titleSmall)
                    if (course.licensed) {
                        Chip(
                            stringResource(R.string.quiz_licensed),
                            colors.accentSoft,
                            colors.accent
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(
                        stringResource(R.string.home_progress, course.solved, course.total),
                        style = type.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(remainingText(course), style = type.bodyLarge, color = colors.textTertiary)
                }
                RcProgressBar(course.share, Modifier.fillMaxWidth())
            }
            StartButton(course, onStart, large = false)
        }
    }
}

@Composable
private fun CourseCard(course: CourseSummary, onStart: () -> Unit, modifier: Modifier) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Card(modifier, line = if (course.licensed) colors.accent else null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(course.license.label), style = type.titleLarge)
            Spacer(Modifier.weight(1f))
            if (course.licensed) {
                Chip(stringResource(R.string.quiz_licensed), colors.accentSoft, colors.accent)
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                Count("${course.solved}", course.total)
                RcProgressBar(course.share, Modifier.fillMaxWidth())
                Text(remainingText(course), style = type.bodyLarge, color = colors.textTertiary)
            }
            StartButton(course, onStart, large = true)
        }
    }
}

@Composable
private fun RankedResultScreen(
    result: RoundResult,
    onHome: () -> Unit,
    onOneMore: () -> Unit,
    onPracticeMissed: () -> Unit,
    onBack: () -> Unit,
    compact: Boolean
) {
    val course = stringResource(result.license.label)
    ResultLayout(
        mode = PlayMode.Ranked,
        title = stringResource(R.string.quiz_result_title, course),
        compact = compact,
        answers = result.answers,
        onBack = onBack,
        summary = { RankedSummary(result, course) },
        buttons = { modifier -> HomeAndOneMore(onHome, onOneMore, modifier) },
        // Practice with this round's misses; it never counts toward the license.
        extra = if (result.correct < result.answers.size) {
            { modifier ->
                RcButton(
                    text = stringResource(R.string.quiz_practice_missed),
                    onClick = onPracticeMissed,
                    modifier = modifier,
                    icon = R.drawable.ic_pencil,
                    iconTint = RulecadeTheme.colors.accent,
                    large = true
                )
            }
        } else {
            null
        }
    )
}

@Composable
private fun RankedSummary(result: RoundResult, course: String) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    ResultScore(result.correct, result.answers.size)
    Card(Modifier.fillMaxWidth(), background = colors.surfaceTile) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Icon(
                painterResource(R.drawable.ic_crown),
                contentDescription = null,
                modifier = Modifier.size(Sizes.icon),
                tint = colors.gradeGold
            )
            Text(stringResource(R.string.quiz_result_license, course), style = type.titleSmall)
        }
        Count("${result.solvedBefore} → ${result.solvedAfter}", result.total)
        RcProgressBar(result.solvedAfter.toFloat() / result.total, Modifier.fillMaxWidth())
        val gained = result.solvedAfter - result.solvedBefore
        Text(
            if (gained > 0) {
                stringResource(R.string.quiz_result_new, gained)
            } else {
                stringResource(R.string.quiz_result_none)
            },
            style = type.bodyMedium,
            color = colors.textTertiary
        )
    }
}
