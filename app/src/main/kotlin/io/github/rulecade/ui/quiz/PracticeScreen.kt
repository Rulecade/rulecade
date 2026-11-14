package io.github.rulecade.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.rulecade.R
import io.github.rulecade.progress.PlayMode
import io.github.rulecade.progress.Progress
import io.github.rulecade.quiz.Difficulty
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.Question
import io.github.rulecade.quiz.QuestionBank
import io.github.rulecade.ui.component.COMPACT_WIDTH
import io.github.rulecade.ui.component.RcButton
import io.github.rulecade.ui.component.RcButtonStyle
import io.github.rulecade.ui.component.RcTab
import io.github.rulecade.ui.home.label
import io.github.rulecade.ui.theme.Radius
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Spacing

private val ROW_MIN_HEIGHT = 112.dp

// The practice destination: a round, its result, or the question list. [onHome] is the result's
// home button; [onBack] leaves a round that didn't start from the list.
@Composable
fun PracticeScreen(
    quiz: PracticeQuiz,
    bank: QuestionBank,
    progress: Progress,
    onHome: () -> Unit,
    onBack: () -> Unit
) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxWidth < COMPACT_WIDTH
    val round = quiz.round
    val result = quiz.result
    // Rounds started from the list go back there; the others go back where they started.
    val done = {
        val toList = quiz.fromList
        quiz.close()
        if (!toList) onBack()
    }
    when {
        round != null -> RoundScreen(
            round = round,
            mode = PlayMode.Practice,
            title = stringResource(
                R.string.practice_round_title,
                setName(round.set),
                round.index + 1,
                round.total
            ),
            compact = compact,
            onChoose = quiz::choose,
            onNext = quiz::next,
            onLeave = { if (round.isFinished) quiz.leave() else done() }
        ) { onLeave, onStay ->
            LeaveDialog(
                title = stringResource(R.string.practice_leave_title),
                body = null,
                leave = stringResource(R.string.practice_leave),
                stay = stringResource(R.string.practice_keep),
                onLeave = onLeave,
                onStay = onStay
            )
        }
        result != null -> ResultLayout(
            mode = PlayMode.Practice,
            title = stringResource(R.string.practice_result_title, setName(result.set)),
            compact = compact,
            answers = result.answers,
            onBack = done,
            summary = {
                ResultScore(result.correct, result.answers.size)
                Text(
                    stringResource(R.string.practice_result_note),
                    style = RulecadeTheme.typography.bodyLarge,
                    color = RulecadeTheme.colors.textTertiary
                )
            },
            buttons = { modifier ->
                HomeAndOneMore(
                    onHome = {
                        quiz.close()
                        onHome()
                    },
                    // An emptied notebook has nothing to replay; its list says so.
                    onOneMore = { if (!quiz.again()) done() },
                    modifier = modifier
                )
            }
        )
        else -> QuestionList(quiz, bank, progress, compact, onHome)
    }
}

@Composable
private fun setName(set: PracticeSet) = when (set) {
    PracticeSet.Mixed -> stringResource(R.string.practice_set_mixed)
    PracticeSet.Charging -> stringResource(R.string.practice_set_charging)
    PracticeSet.Notebook -> stringResource(R.string.home_notebook)
    PracticeSet.Missed -> stringResource(R.string.practice_set_missed)
    is PracticeSet.Course -> stringResource(set.license.label)
    is PracticeSet.Single -> stringResource(set.license.label)
    is PracticeSet.Topic -> set.title
}

// Tabs for the notebook and each course; a question can be solved alone or all of a tab at once.
@Composable
private fun QuestionList(
    quiz: PracticeQuiz,
    bank: QuestionBank,
    progress: Progress,
    compact: Boolean,
    onBack: () -> Unit
) = Screen(compact, PlayMode.Practice) {
    BackHandler(onBack = onBack)
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    val tab = quiz.tab
    val notebook = quiz.notebook
    val questions = if (tab == null) notebook else bank.byLicense(tab)
    TopBar(stringResource(R.string.practice_list_title), onBack)
    // Tabs wrap to a second line in a split window instead of hiding past the edge.
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        RcTab(
            stringResource(
                R.string.practice_tab,
                stringResource(R.string.home_notebook),
                notebook.size
            ),
            selected = tab == null
        ) { quiz.tab = null }
        License.entries.forEach { license ->
            RcTab(
                stringResource(
                    R.string.practice_tab,
                    stringResource(license.label),
                    bank.byLicense(license).size
                ),
                selected = tab == license
            ) { quiz.tab = license }
        }
    }
    if (questions.isEmpty()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.practice_notebook_empty),
                style = type.bodyLarge,
                color = colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            items(questions, key = { it.id }) { question ->
                QuestionRow(question, missed(progress, question, tab == null)) {
                    quiz.startSingle(question)
                }
            }
        }
        RcButton(
            text = if (tab == null) {
                stringResource(R.string.practice_solve_notebook, questions.size)
            } else {
                stringResource(R.string.practice_solve_course, questions.size)
            },
            onClick = { if (tab == null) quiz.startNotebook() else quiz.startCourse(tab) },
            modifier = Modifier.fillMaxWidth(),
            style = RcButtonStyle.Accent,
            large = true
        )
    }
}

// Where a notebook question was last missed; older saves do not know.
@Composable
private fun missed(progress: Progress, question: Question, inNotebook: Boolean): String? {
    if (!inNotebook) return null
    return when (progress.stats[question.id]?.lastMissed) {
        PlayMode.Ranked -> stringResource(R.string.practice_missed_ranked)
        PlayMode.Practice -> stringResource(R.string.practice_missed_practice)
        null -> null
    }
}

@Composable
private fun QuestionRow(question: Question, missed: String?, onSolve: () -> Unit) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    val meta = listOfNotNull(
        stringResource(question.license.label),
        stringResource(question.difficulty.label),
        missed
    ).joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ROW_MIN_HEIGHT)
            .background(colors.surfaceCard, RoundedCornerShape(Radius.button))
            .padding(horizontal = Spacing.large, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.large)
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
            Text(question.prompt, style = type.titleSmall)
            Text(meta, style = type.bodyMedium, color = colors.textTertiary)
        }
        RcButton(text = stringResource(R.string.practice_solve), onClick = onSolve)
    }
}

private val Difficulty.label: Int
    get() = when (this) {
        Difficulty.Easy -> R.string.difficulty_easy
        Difficulty.Normal -> R.string.difficulty_normal
        Difficulty.Hard -> R.string.difficulty_hard
    }
