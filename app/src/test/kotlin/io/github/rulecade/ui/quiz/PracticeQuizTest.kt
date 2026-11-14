package io.github.rulecade.ui.quiz

import io.github.rulecade.progress.PlayMode
import io.github.rulecade.progress.ProgressHolder
import io.github.rulecade.progress.ProgressStore
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

class PracticeQuizTest {

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

    private val questions =
        (1..6).map { question("h$it") } + (1..3).map { question("c$it", License.City) }
    private val bank = QuestionBank(1, "2026-11-10", questions)
    private val store get() = ProgressStore(File(folder.root, ProgressStore.FILE_NAME))
    private val holder = lazy { ProgressHolder(store) { it.run() } }
    private val practice by lazy { PracticeQuiz(bank, holder.value, Random(7)) }
    private val ranked by lazy {
        RankedQuiz(bank, holder.value, { LocalDate.of(2026, 11, 13) }, Random(7))
    }

    private fun QuizRound.play(right: Boolean, choose: (Int) -> Unit, next: () -> Unit): String {
        val item = item
        choose(if (right) item.answer else (item.answer + 1) % item.choices.size)
        if (!isFinished) next()
        return item.question.id
    }

    private fun PracticeQuiz.answer(right: Boolean) = round!!.play(right, {
        choose(it)
    }, { next() })

    @Test
    fun mixedRoundsTakeFiveQuestionsFromEveryCourse() {
        practice.startMixed()
        assertEquals(5, practice.round!!.total)
        assertEquals(PracticeSet.Mixed, practice.round!!.set)
        assertFalse(practice.fromList)
    }

    @Test
    fun chargingRoundsTakeFifteenMixedQuestionsAndReplayAnew() {
        // Fifteen, or every question of a smaller bank.
        val count = minOf(CHARGING_COUNT, bank.questions.size)
        practice.startCharging()
        assertEquals(count, practice.round!!.total)
        assertEquals(PracticeSet.Charging, practice.round!!.set)
        assertFalse(practice.fromList)
        repeat(count) { practice.answer(right = true) }
        assertTrue(practice.again())
        assertEquals(PracticeSet.Charging, practice.round!!.set)
        assertEquals(count, practice.round!!.total)
    }

    @Test
    fun everyAnswerIsSavedAtOnceAndNeverCountsTowardLicenses() {
        practice.startCourse(License.City)
        val first = practice.answer(right = false)

        assertEquals(PlayMode.Practice, holder.value.progress.stats[first]?.lastMissed)
        assertEquals(listOf(first), holder.value.progress.notebook)
        assertEquals(holder.value.data, store.load())

        practice.answer(right = true)
        practice.answer(right = true)
        assertEquals(0, holder.value.progress.solvedCount(bank.byLicense(License.City)))
        assertTrue(holder.value.progress.licenses.isEmpty())
        assertEquals(3, practice.result!!.answers.size)
        assertTrue(practice.fromList)
    }

    @Test
    fun leavingKeepsTheAnswersGiven() {
        practice.startMixed()
        val id = practice.answer(right = false)
        practice.leave()

        assertNull(practice.round)
        assertNull(practice.result)
        assertEquals(listOf(id), holder.value.progress.notebook)
    }

    @Test
    fun theNotebookRoundPlaysTheMissedQuestionsAndEmptiesTheNotebook() {
        practice.startCourse(License.City)
        repeat(3) { practice.answer(right = false) }
        assertEquals(3, practice.notebook.size)

        practice.startNotebook()
        assertEquals(PracticeSet.Notebook, practice.round!!.set)
        repeat(3) { practice.answer(right = true) }

        assertTrue(holder.value.progress.notebook.isEmpty())
        practice.leave()
        assertFalse(practice.again())
        assertNotNull(practice.result)
        assertNull(practice.round)
    }

    @Test
    fun missedRankedAnswersStartAPracticeRound() {
        ranked.start(License.Highway)
        val wrong = (0 until 5).map { i ->
            ranked.round!!.play(i % 2 == 0, ranked::choose, ranked::next)
        }
            .filterIndexed { i, _ -> i % 2 == 1 }
        practice.startMissed(ranked.result!!.answers)

        val round = practice.round!!
        assertEquals(PracticeSet.Missed, round.set)
        assertEquals(wrong.toSet(), round.questions.map { it.id }.toSet())
        assertFalse(practice.fromList)
    }

    @Test
    fun againReplaysTheSameSet() {
        val single = questions.first()
        practice.startSingle(single)
        practice.answer(right = true)
        practice.again()

        assertEquals(PracticeSet.Single(License.Highway), practice.round!!.set)
        assertEquals(listOf(single), practice.round!!.questions)
        assertNull(practice.result)
    }

    @Test
    fun repeatedTapsAreIgnored() {
        practice.startMixed()
        val round = practice.round!!
        practice.choose(round.item.answer)
        practice.choose(round.item.answer)
        practice.next()
        practice.next()

        assertEquals(1, round.index)
        assertEquals(1, holder.value.progress.stats.values.sumOf { it.practiceRight })
    }
}
