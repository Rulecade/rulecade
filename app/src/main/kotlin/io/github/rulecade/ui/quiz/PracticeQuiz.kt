package io.github.rulecade.ui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.rulecade.progress.ProgressHolder
import io.github.rulecade.quiz.Answer
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.Question
import io.github.rulecade.quiz.QuestionBank
import io.github.rulecade.quiz.QuizSession
import kotlin.random.Random

// About ten minutes of questions, at about three minutes a round of five.
const val CHARGING_COUNT = 15

// What a practice round is made of; it also names the round.
sealed interface PracticeSet {
    data object Mixed : PracticeSet

    // A longer mixed round, offered while the car is charging.
    data object Charging : PracticeSet

    data object Notebook : PracticeSet

    // The wrong answers of the ranked round just played.
    data object Missed : PracticeSet

    data class Course(val license: License) : PracticeSet

    data class Single(val license: License) : PracticeSet

    // The questions of a library topic.
    data class Topic(val title: String) : PracticeSet
}

class PracticeRound internal constructor(
    val set: PracticeSet,
    val questions: List<Question>,
    session: QuizSession
) : QuizRound(session)

data class PracticeResult(
    val set: PracticeSet,
    val questions: List<Question>,
    val answers: List<Answer>
) {
    val correct: Int get() = answers.count { it.correct }
}

// The unranked mode (연습). Every answer is saved at once and only moves the notebook; it never
// counts toward licenses.
class PracticeQuiz(
    private val bank: QuestionBank,
    private val progress: ProgressHolder,
    private val random: Random = Random
) {
    var round by mutableStateOf<PracticeRound?>(null)
        private set

    var result by mutableStateOf<PracticeResult?>(null)
        private set

    // The question list tab: null is the notebook, otherwise a course.
    var tab by mutableStateOf<License?>(null)

    // Rounds started from the question list return there; the others go home.
    var fromList by mutableStateOf(false)
        private set

    val notebook: List<Question> get() = progress.progress.notebook.mapNotNull(bank::byId)

    fun startMixed() = start(
        PracticeSet.Mixed,
        bank.questions.shuffled(random).take(QuizSession.DEFAULT_COUNT),
        false
    )

    fun startCharging() = start(
        PracticeSet.Charging,
        bank.questions.shuffled(random).take(CHARGING_COUNT),
        false
    )

    fun startNotebook() = start(PracticeSet.Notebook, notebook, true)

    fun startCourse(license: License) =
        start(PracticeSet.Course(license), bank.byLicense(license), true)

    fun startSingle(question: Question) =
        start(PracticeSet.Single(question.license), listOf(question), true)

    fun startTopic(title: String, ids: List<String>) =
        start(PracticeSet.Topic(title), ids.mapNotNull(bank::byId), false)

    fun startMissed(answers: List<Answer>) =
        start(PracticeSet.Missed, answers.filter { !it.correct }.map { it.item.question }, false)

    // Plays the finished set again; mixed and notebook rounds pick their questions anew. False
    // when there is nothing left to play (an emptied notebook).
    fun again(): Boolean {
        val last = result ?: return false
        return when (last.set) {
            PracticeSet.Mixed -> startMixed()
            PracticeSet.Charging -> startCharging()
            PracticeSet.Notebook -> startNotebook()
            else -> start(last.set, last.questions, fromList)
        }
    }

    // Taps can arrive after the screen has moved on (a quick double tap); those are ignored.
    fun choose(choice: Int) {
        val round = round?.takeIf { it.answer == null } ?: return
        val answer = round.choose(choice)
        progress.update { it.recordPractice(answer) }
        if (round.isFinished) result = PracticeResult(round.set, round.questions, round.results)
    }

    fun next() {
        round?.takeIf { it.answer != null && !it.isFinished }?.next()
    }

    // Answers given so far stay saved; after the last answer this opens the result.
    fun leave() {
        round = null
    }

    fun close() {
        round = null
        result = null
    }

    private fun start(set: PracticeSet, questions: List<Question>, fromList: Boolean): Boolean {
        if (questions.isEmpty()) return false
        this.fromList = fromList
        round = PracticeRound(set, questions, QuizSession(questions, questions.size, random))
        result = null
        return true
    }
}
