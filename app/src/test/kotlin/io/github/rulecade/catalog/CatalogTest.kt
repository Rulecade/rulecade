package io.github.rulecade.catalog

import io.github.rulecade.library.Library
import io.github.rulecade.progress.Progress
import io.github.rulecade.progress.QuestionStats
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.QuestionBank
import io.github.rulecade.ui.license.RoadSign
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Guards the shipped catalog and the grade rules.
class CatalogTest {

    private val catalog = Catalog.parse(File("src/main/assets/${Catalog.ASSET_PATH}").readText())
    private val bank = QuestionBank.parse(
        File("src/main/assets/${QuestionBank.ASSET_PATH}").readText()
    )

    @Test
    fun signsAreUniqueAndDrawn() {
        val ids = catalog.signs.map { it.id }
        assertEquals("duplicate ids", ids.size, ids.toSet().size)
        ids.forEach { assertTrue(it, it.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*"))) }
        val numbers = catalog.signs.map { it.number }
        assertEquals("duplicate numbers", numbers.size, numbers.toSet().size)
        catalog.signs.forEach {
            assertNotNull("no drawing for ${it.number}", RoadSign.of(it.number))
        }
    }

    // Annex 6 numbers caution signs 1xx, regulation signs 2xx and instruction signs 3xx.
    @Test
    fun numbersMatchCategories() {
        val hundreds = mapOf(
            SignCategory.Caution to 1,
            SignCategory.Regulation to 2,
            SignCategory.Instruction to 3
        )
        catalog.signs.forEach {
            assertEquals("${it.id} number", hundreds[it.category], it.number / 100)
            assertTrue("${it.id} meaning", it.meaning.isNotBlank())
        }
        val checked = LocalDate.parse(catalog.checked)
        assertTrue("checked is in the future", !checked.isAfter(LocalDate.now()))
    }

    // Each sign links to existing questions, and a question counts toward one sign at most.
    @Test
    fun signsLinkToQuestions() {
        val ids = bank.questions.map { it.id }.toSet()
        catalog.signs.forEach { s ->
            assertTrue("${s.id} has no questions", s.questions.isNotEmpty())
            s.questions.forEach { assertTrue("${s.id}: $it", it in ids) }
        }
        val linked = catalog.signs.flatMap { it.questions }
        assertEquals("a question in two signs", linked.size, linked.toSet().size)
    }

    // A sign's page links to the library topic of its first question, so every sign needs one.
    @Test
    fun everySignHasALibraryTopic() {
        val library = Library.parse(File("src/main/assets/${Library.ASSET_PATH}").readText())
        catalog.signs.forEach { s ->
            assertNotNull("${s.id} has no library topic", library.topicFor(s.questions.first()))
        }
    }

    @Test
    fun gradesFollowRankedAnswersOnly() {
        val sign = Sign("s", 224, SignCategory.Regulation, "s", "m", listOf("a", "b"))
        fun progress(vararg stats: Pair<String, QuestionStats>) = Progress(stats = stats.toMap())
        assertEquals(Grade.Uncollected, Progress().grade(sign))
        assertEquals(
            Grade.Uncollected,
            progress("a" to QuestionStats(practiceRight = 3, rankedWrong = 1)).grade(sign)
        )
        assertEquals(Grade.Collected, progress("a" to QuestionStats(rankedRight = 1)).grade(sign))
        assertEquals(
            Grade.Mastered,
            progress(
                "a" to QuestionStats(rankedRight = 1),
                "b" to QuestionStats(rankedRight = 2, rankedWrong = 1)
            ).grade(sign)
        )
        assertEquals(1, progress("b" to QuestionStats(rankedRight = 1)).solvedCount(sign))
    }

    // Every license card shows three signs, all linked to that course's questions.
    @Test
    fun eachLicenseCardShowsItsCourseSigns() {
        val expected = mapOf(
            License.Highway to listOf(217, 224, 225),
            License.Intersection to listOf(109, 132, 329),
            License.SchoolZone to listOf(132, 133, 218),
            License.City to listOf(218, 219, 224)
        )
        License.entries.forEach { license ->
            val signs = catalog.signsFor(license, bank)
            assertEquals("$license", expected[license], signs.map { it.number })
            signs.forEach { s ->
                assertTrue(
                    "${s.id} for $license",
                    s.questions.any { bank.byId(it)?.license == license }
                )
            }
        }
    }

    // With a single linked question, solving it masters the sign at once.
    @Test
    fun aSingleQuestionSignIsMasteredByOneAnswer() {
        val sign = Sign("s", 217, SignCategory.Regulation, "s", "m", listOf("a"))
        val progress = Progress(stats = mapOf("a" to QuestionStats(rankedRight = 1)))
        assertEquals(Grade.Mastered, progress.grade(sign))
    }

    // A sign passed on the road is discovered until a ranked answer collects it.
    @Test
    fun discoveredSignsWaitForARankedAnswer() {
        val sign = Sign("s", 133, SignCategory.Caution, "s", "m", listOf("a", "b"))
        val found = Progress().discover("s")
        assertEquals(Grade.Discovered, found.grade(sign))
        val collected = found.copy(stats = mapOf("a" to QuestionStats(rankedRight = 1)))
        assertEquals(Grade.Collected, collected.grade(sign))
    }
}
