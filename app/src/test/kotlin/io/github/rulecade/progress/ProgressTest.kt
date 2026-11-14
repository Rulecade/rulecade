package io.github.rulecade.progress

import io.github.rulecade.quiz.Answer
import io.github.rulecade.quiz.Difficulty
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.Question
import io.github.rulecade.quiz.QuestionBank
import io.github.rulecade.quiz.QuizSession
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

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

    private fun answer(question: Question, correct: Boolean): Answer {
        val session = QuizSession(listOf(question), count = 1)
        val item = session.current!!
        return session.answer(if (correct) item.answer else (item.answer + 1) % 4)
    }

    private val h1 = question("h1")
    private val h2 = question("h2")
    private val h3 = question("h3")
    private val c1 = question("c1", License.City)
    private val bank = QuestionBank(1, "2026-11-10", listOf(h1, h2, h3, c1))
    private val highway = bank.byLicense(License.Highway)
    private val today = LocalDate.of(2026, 11, 10)

    @Test
    fun rightRankedAnswersSolveQuestions() {
        val progress = Progress().recordRanked(listOf(answer(h1, true), answer(h2, false)))

        assertEquals(1, progress.solvedCount(highway))
        assertEquals(QuestionStats(rankedRight = 1), progress.stats["h1"])
        assertEquals(
            QuestionStats(rankedWrong = 1, lastMissed = PlayMode.Ranked),
            progress.stats["h2"]
        )
    }

    @Test
    fun rankedPickPutsUnsolvedQuestionsFirst() {
        val progress = Progress().recordRanked(listOf(answer(h1, true)))

        assertEquals(setOf("h2", "h3"), progress.rankedPick(highway, 2).map { it.id }.toSet())
        assertEquals("h1", progress.rankedPick(highway, 3).last().id)
    }

    // Profiles keep their own progress; a new one starts empty, gets the next free id and plays.
    @Test
    fun profilesKeepTheirOwnProgress() {
        val mine = Progress().recordRanked(listOf(answer(h1, true)))
        val start = SaveData().withProgress(mine)
        val added = start.addProfile("  우리집막내동생  ")
        assertEquals("우리집막내동", added.names[added.activeProfile])
        assertEquals(listOf(SaveData.DEFAULT_PROFILE, added.activeProfile), added.profileIds)
        assertEquals(Progress(), added.progress)
        assertEquals(mine, added.switchTo(SaveData.DEFAULT_PROFILE).progress)
        assertEquals(added, added.switchTo("nobody"))
        assertEquals(added, added.addProfile("   "))
        val full = (2..SaveData.MAX_PROFILES).fold(start) { data, n -> data.addProfile("p$n") }
        assertFalse(full.canAddProfile)
        assertEquals(full, full.addProfile("more"))
        assertEquals(full.profileIds.size, full.profileIds.toSet().size)
    }

    // Signs found on the road gather until home shows them; the next find starts a new list.
    @Test
    fun recentSignsStartAnewAfterHomeShowsThem() {
        val found = Progress().discover("children").discover("max-speed").discover("children")
        assertEquals(listOf("children", "max-speed"), found.recentSigns)
        assertEquals(setOf("children", "max-speed"), found.discoveredSigns)
        val shown = found.showRecent()
        assertTrue(shown.recentShown)
        assertEquals(found.recentSigns, shown.recentSigns)
        val next = shown.discover("roundabout")
        assertEquals(listOf("roundabout"), next.recentSigns)
        assertFalse(next.recentShown)
        assertEquals(setOf("children", "max-speed", "roundabout"), next.discoveredSigns)
        assertEquals(Progress(), Progress().showRecent())
    }

    @Test
    fun practiceNeverCountsTowardLicenses() {
        val progress = highway.fold(Progress()) { p, q -> p.recordPractice(answer(q, true)) }

        assertEquals(0, progress.solvedCount(highway))
        assertTrue(progress.unawardedLicenses(bank).isEmpty())
        assertEquals(QuestionStats(practiceRight = 1), progress.stats["h1"])
    }

    @Test
    fun wrongAnswersEnterTheNotebookNewestFirst() {
        val progress = Progress()
            .recordRanked(listOf(answer(h1, false)))
            .recordPractice(answer(c1, false))
            .recordRanked(listOf(answer(h1, false)))

        assertEquals(listOf("h1", "c1"), progress.notebook)
    }

    @Test
    fun theLastMissKeepsWhereItHappened() {
        val progress = Progress()
            .recordRanked(listOf(answer(h1, false)))
            .recordPractice(answer(h1, false))
            .recordPractice(answer(c1, false))
            .recordRanked(listOf(answer(c1, false), answer(h2, true)))

        assertEquals(PlayMode.Practice, progress.stats["h1"]?.lastMissed)
        assertEquals(PlayMode.Ranked, progress.stats["c1"]?.lastMissed)
        assertEquals(null, progress.stats["h2"]?.lastMissed)
    }

    @Test
    fun onlyARightPracticeAnswerLeavesTheNotebook() {
        val afterRanked = Progress()
            .recordPractice(answer(h1, false))
            .recordRanked(listOf(answer(h1, true)))
        assertEquals(listOf("h1"), afterRanked.notebook)

        assertTrue(afterRanked.recordPractice(answer(h1, true)).notebook.isEmpty())
    }

    @Test
    fun licenseIsEarnedWhenEveryQuestionOfTheCourseIsSolved() {
        val almost = Progress().recordRanked(listOf(answer(h1, true), answer(h2, true)))
        assertTrue(almost.unawardedLicenses(bank).isEmpty())

        val done = almost.recordRanked(listOf(answer(h3, true)))
        assertEquals(listOf(License.Highway), done.unawardedLicenses(bank))

        val awarded = done.award(done.unawardedLicenses(bank), today)
        assertEquals(mapOf(License.Highway to "2026-11-10"), awarded.licenses)
        assertTrue(awarded.unawardedLicenses(bank).isEmpty())
    }

    @Test
    fun awardedLicenseStaysWhenQuestionsAreAdded() {
        val awarded = Progress()
            .recordRanked(highway.map { answer(it, true) })
            .let { it.award(it.unawardedLicenses(bank), today) }
        val grown = bank.copy(questions = bank.questions + question("h4"))

        assertEquals(3, awarded.solvedCount(grown.byLicense(License.Highway)))
        assertTrue(License.Highway in awarded.licenses)
        assertTrue(awarded.unawardedLicenses(grown).isEmpty())
    }

    @Test
    fun removedQuestionsAreNotCounted() {
        val progress = Progress().recordRanked(listOf(answer(question("gone"), true)))

        assertEquals(0, progress.solvedCount(highway))
        assertFalse(progress.isSolved("h1"))
    }

    @Test
    fun profilesKeepSeparateProgress() {
        val progress = Progress().discover("school-zone")
        val data = SaveData().withProgress(progress)

        assertEquals(progress, data.progress)
        assertEquals(Progress(), data.copy(activeProfile = "guest").progress)
    }
}
