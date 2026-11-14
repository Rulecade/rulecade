package io.github.rulecade.ui.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.rulecade.progress.ProgressHolder
import io.github.rulecade.quiz.Answer
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.QuestionBank
import io.github.rulecade.quiz.QuizSession
import java.time.LocalDate
import kotlin.random.Random

// One ranked round, kept in memory until its last answer.
class RankedRound internal constructor(val license: License, session: QuizSession) :
    QuizRound(session)

data class RoundResult(
    val license: License,
    val answers: List<Answer>,
    val solvedBefore: Int,
    val solvedAfter: Int,
    val total: Int
) {
    val correct: Int get() = answers.count { it.correct }
}

// The ranked mode (실전). A round is saved only when its last question is answered, so a
// round left early never changes progress.
class RankedQuiz(
    private val bank: QuestionBank,
    private val progress: ProgressHolder,
    private val today: () -> LocalDate = LocalDate::now,
    private val random: Random = Random
) {
    var round by mutableStateOf<RankedRound?>(null)
        private set

    var result by mutableStateOf<RoundResult?>(null)
        private set

    // A license earned by the last round, until its award screen is dismissed.
    var award by mutableStateOf<License?>(null)
        private set

    fun earnedDate(license: License): String? = progress.progress.licenses[license]

    fun accuracy(license: License) = progress.progress.rankedAccuracy(bank.byLicense(license))

    fun dismissAward() {
        award = null
    }

    fun start(license: License) {
        award = null
        val questions = bank.byLicense(license)
        val picked = progress.progress.rankedPick(questions, QuizSession.DEFAULT_COUNT, random)
        round = RankedRound(license, QuizSession(picked, picked.size, random))
        result = null
    }

    // Taps can arrive after the screen has moved on (a quick double tap); those are ignored.
    fun choose(choice: Int) {
        val round = round?.takeIf { it.answer == null } ?: return
        round.choose(choice)
        if (round.isFinished) record(round)
    }

    fun next() {
        round?.takeIf { it.answer != null && !it.isFinished }?.next()
    }

    // Drops an unfinished round; after the last answer this opens the result.
    fun leave() {
        round = null
    }

    fun close() {
        round = null
        result = null
        award = null
    }

    private fun record(round: RankedRound) {
        val questions = bank.byLicense(round.license)
        val before = progress.progress.solvedCount(questions)
        val held = progress.progress.licenses.keys
        progress.update { old ->
            val new = old.recordRanked(round.results)
            new.award(new.unawardedLicenses(bank), today())
        }
        val after = progress.progress.solvedCount(questions)
        result = RoundResult(round.license, round.results, before, after, questions.size)
        award = (progress.progress.licenses.keys - held).firstOrNull()
    }
}
