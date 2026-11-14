package io.github.rulecade.quiz

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizSessionTest {

    private fun question(id: String, answer: Int = 0) = Question(
        id = id,
        license = License.Highway,
        prompt = "Q $id",
        choices = listOf("a", "b", "c", "d"),
        answer = answer,
        explanation = "why",
        difficulty = Difficulty.Normal,
        checked = "2026-11-08",
        basis = "law"
    )

    private val bank = (1..8).map { question("q$it", answer = it % 4) }

    @Test
    fun takesTheRequestedNumberOfDistinctQuestions() {
        val session = QuizSession(bank, count = 5, random = Random(1))
        assertEquals(5, session.items.size)
        assertEquals(5, session.items.map { it.question.id }.toSet().size)
    }

    @Test
    fun shuffledChoicesStillPointAtTheRightAnswer() {
        val session = QuizSession(bank, count = 8, random = Random(7))
        session.items.forEach { item ->
            assertEquals(item.question.choices[item.question.answer], item.choices[item.answer])
            assertTrue(item.isCorrect(item.answer))
        }
    }

    @Test
    fun countsCorrectAnswersAndFinishes() {
        val session = QuizSession(bank, count = 3, random = Random(3))
        session.answer(session.current!!.answer)
        val wrong = (session.current!!.answer + 1) % 4
        assertFalse(session.answer(wrong).correct)
        session.answer(session.current!!.answer)

        assertTrue(session.isFinished)
        assertNull(session.current)
        assertEquals(2, session.correctCount)
        assertEquals(2f / 3, session.accuracy, 0.0001f)
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsAnswersAfterTheEnd() {
        val session = QuizSession(bank.take(1), count = 1)
        session.answer(0)
        session.answer(0)
    }
}
