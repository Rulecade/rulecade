package io.github.rulecade.library

import io.github.rulecade.quiz.QuestionBank
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Guards the shipped library: sound structure, short texts, real sources, and links to real
// questions.
class LibraryContentTest {

    private val library = Library.parse(File("src/main/assets/${Library.ASSET_PATH}").readText())
    private val bank = QuestionBank.parse(
        File("src/main/assets/${QuestionBank.ASSET_PATH}").readText()
    )

    @Test
    fun idsAreUniqueKebabCase() {
        val ids = library.topics.map { it.id }
        assertEquals("duplicate ids", ids.size, ids.toSet().size)
        ids.forEach { assertTrue(it, it.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*"))) }
    }

    @Test
    fun textsFitTheScreen() {
        library.topics.forEach { t ->
            assertTrue("${t.id} title", t.title.length <= MAX_TITLE)
            assertTrue("${t.id} summary", t.summary.length <= MAX_SUMMARY)
            assertTrue("${t.id} steps", t.steps.size in MIN_STEPS..MAX_STEPS)
            t.steps.forEach { assertTrue("${t.id} step: $it", it.length <= MAX_STEP) }
            assertTrue("${t.id} fines", t.fines.isNotEmpty())
            t.fines.forEach {
                assertTrue("${t.id} fine: ${it.label}", it.label.length <= MAX_LABEL)
            }
        }
    }

    // Every quote names the article it comes from; revisions name a real date in the past.
    @Test
    fun everyTopicCitesItsSource() {
        val checked = LocalDate.parse(library.checked)
        assertTrue("checked is in the future", !checked.isAfter(LocalDate.now()))
        library.topics.forEach { t ->
            assertTrue("${t.id} law", t.law.isNotEmpty())
            t.law.forEach { q ->
                assertTrue("${t.id} source: ${q.source}", ARTICLE.containsMatchIn(q.source))
                assertTrue("${t.id} quote", q.text.isNotBlank())
            }
            t.revised?.let { r ->
                assertTrue("${t.id} revised", !LocalDate.parse(r.date).isAfter(checked))
                assertTrue("${t.id} mark: ${r.mark}", r.mark in MARKS)
                assertTrue("${t.id} revised of: ${r.of}", ARTICLE.containsMatchIn(r.of))
            }
        }
    }

    // Each topic links to existing questions, and a question belongs to one topic at most so the
    // quiz link is never ambiguous.
    @Test
    fun topicsLinkToQuestions() {
        val ids = bank.questions.map { it.id }.toSet()
        library.topics.forEach { t ->
            assertTrue("${t.id} has no questions", t.questions.isNotEmpty())
            t.questions.forEach { assertTrue("${t.id}: $it", it in ids) }
        }
        val linked = library.topics.flatMap { it.questions }
        assertEquals("a question in two topics", linked.size, linked.toSet().size)
    }

    private companion object {
        const val MAX_TITLE = 16
        const val MAX_SUMMARY = 64
        const val MIN_STEPS = 2
        const val MAX_STEPS = 5
        const val MAX_STEP = 28
        const val MAX_LABEL = 24
        val MARKS = setOf("개정", "신설")
        val ARTICLE = Regex(
            "^(도로교통법( 시행령| 시행규칙)?|특정범죄 가중처벌 등에 관한 법률) (제\\d+조(의\\d+)?|별표 \\d+)"
        )
    }
}
