package io.github.rulecade.ui.quiz

import io.github.rulecade.progress.ProgressHolder
import io.github.rulecade.progress.ProgressStore
import io.github.rulecade.progress.SaveData
import io.github.rulecade.quiz.Difficulty
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.Question
import io.github.rulecade.quiz.QuestionBank
import java.io.File
import java.time.LocalDate
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RankedQuizTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun question(id: String, license: License = License.Highway) = Question(
        id = id,
        license = license,
        prompt = "Q $id",
        choices = listOf("a", "b", "c", "d"),
        answer = 0,
        explanation = "why",
        difficulty = Difficulty.Normal,
        checked = "2026-11-10",
        basis = "law"
    )

    private val today = LocalDate.of(2026, 11, 10)
    private val store get() = ProgressStore(File(folder.root, ProgressStore.FILE_NAME))

    private fun quiz(highway: Int): Pair<RankedQuiz, ProgressHolder> {
        val questions = (1..highway).map { question("h$it") } + question("c1", License.City)
        val holder = ProgressHolder(store) { it.run() }
        val quiz =
            RankedQuiz(QuestionBank(1, "2026-11-10", questions), holder, { today }, Random(4))
        return quiz to holder
    }

    // Answers the current question and moves on; returns its id.
    private fun RankedQuiz.answer(right: Boolean): String {
        val round = round!!
        val item = round.item
        choose(if (right) item.answer else (item.answer + 1) % item.choices.size)
        if (!round.isFinished) next()
        return item.question.id
    }

    @Test
    fun roundsPutUnsolvedQuestionsFirst() {
        val (quiz, _) = quiz(highway = 7)
        quiz.start(License.Highway)
        val first = List(5) { quiz.answer(right = true) }
        quiz.leave()

        quiz.start(License.Highway)
        val second = List(5) { quiz.answer(right = true) }

        assertTrue(second.containsAll((1..7).map { "h$it" } - first.toSet()))
    }

    @Test
    fun leavingEarlyRecordsNothing() {
        val (quiz, holder) = quiz(highway = 7)
        quiz.start(License.Highway)
        quiz.answer(right = true)
        quiz.answer(right = false)
        quiz.leave()

        assertNull(quiz.round)
        assertNull(quiz.result)
        assertTrue(holder.progress.stats.isEmpty())
        assertEquals(SaveData(), store.load())
    }

    @Test
    fun theLastAnswerSavesTheRound() {
        val (quiz, holder) = quiz(highway = 7)
        quiz.start(License.Highway)
        repeat(4) { quiz.answer(right = true) }
        quiz.answer(right = false)

        val result = quiz.result!!
        assertEquals(RoundResult(License.Highway, result.answers, 0, 4, 7), result)
        assertEquals(4, result.correct)
        assertEquals(1, holder.progress.notebook.size)
        assertEquals(holder.data, store.load())

        assertNotNull(quiz.round)
        quiz.leave()
        assertNull(quiz.round)
        assertNotNull(quiz.result)
    }

    @Test
    fun streakCountsRightAnswersInARow() {
        val (quiz, _) = quiz(highway = 7)
        quiz.start(License.Highway)
        val streaks = listOf(true, true, false, true).map { right ->
            val round = quiz.round!!
            val item = round.item
            quiz.choose(if (right) item.answer else (item.answer + 1) % 4)
            round.streak.also { quiz.next() }
        }

        assertEquals(listOf(1, 2, 0, 1), streaks)
    }

    @Test
    fun solvingTheWholeCourseAwardsTheLicense() {
        val (quiz, holder) = quiz(highway = 5)
        quiz.start(License.Highway)
        repeat(5) { quiz.answer(right = true) }

        assertEquals(mapOf(License.Highway to "2026-11-10"), holder.progress.licenses)
        assertEquals(5, quiz.result!!.solvedAfter)
        assertEquals(License.Highway, quiz.award)
        assertEquals(100, quiz.accuracy(License.Highway))

        quiz.leave()
        quiz.dismissAward()
        assertNull(quiz.award)
        assertNotNull(quiz.result)
    }

    @Test
    fun anEarnedLicenseIsAwardedOnlyOnce() {
        val (quiz, _) = quiz(highway = 5)
        quiz.start(License.Highway)
        repeat(5) { quiz.answer(right = true) }
        quiz.leave()

        quiz.start(License.Highway)
        repeat(5) { quiz.answer(right = true) }

        assertNull(quiz.award)
    }

    @Test
    fun repeatedTapsAreIgnored() {
        val (quiz, _) = quiz(highway = 7)
        quiz.start(License.Highway)
        val round = quiz.round!!
        quiz.next()
        assertEquals(0, round.index)

        val item = round.item
        quiz.choose(item.answer)
        quiz.choose((item.answer + 1) % item.choices.size)
        assertEquals(1, round.streak)
        assertEquals(item.answer, round.answer!!.choice)

        quiz.next()
        quiz.next()
        assertEquals(1, round.index)
        assertNull(round.answer)

        quiz.leave()
        quiz.choose(0)
        quiz.next()
        assertNull(quiz.round)
    }

    @Test
    fun aRepeatedLastTapSavesTheRoundOnce() {
        val (quiz, holder) = quiz(highway = 7)
        quiz.start(License.Highway)
        repeat(4) { quiz.answer(right = true) }
        val last = quiz.round!!.item
        quiz.choose(last.answer)
        quiz.choose(last.answer)

        assertEquals(5, quiz.result!!.answers.size)
        assertTrue(holder.progress.stats.values.all { it.rankedRight == 1 })
        assertEquals(holder.data, store.load())
    }

    @Test
    fun aShortCourseGivesAShorterRound() {
        val (quiz, _) = quiz(highway = 3)
        quiz.start(License.Highway)

        assertEquals(3, quiz.round!!.total)
        assertFalse(quiz.round!!.isFinished)
    }
}
