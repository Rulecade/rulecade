package io.github.rulecade.quiz

import io.github.rulecade.ui.license.RoadSign
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Guards the shipped question set: the structure must be sound and every text must stay short
// enough to read at a glance (Pleos guide: one line within 80 characters, at most 3 lines).
class QuestionContentTest {

    private val bank = QuestionBank.parse(
        File("src/main/assets/${QuestionBank.ASSET_PATH}").readText()
    )

    @Test
    fun idsAreUniqueKebabCase() {
        val ids = bank.questions.map { it.id }
        assertEquals("duplicate ids", ids.size, ids.toSet().size)
        ids.forEach { assertTrue(it, it.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*"))) }
    }

    @Test
    fun choicesAndAnswersAreValid() {
        bank.questions.forEach { q ->
            assertTrue("${q.id} choice count", q.choices.size in 2..4)
            assertEquals("${q.id} duplicate choices", q.choices.size, q.choices.toSet().size)
            assertTrue("${q.id} answer index", q.answer in q.choices.indices)
        }
    }

    @Test
    fun textsFitTheScreen() {
        bank.questions.forEach { q ->
            assertTrue("${q.id} prompt", q.prompt.length <= MAX_PROMPT)
            q.choices.forEach { assertTrue("${q.id} choice: $it", it.length <= MAX_CHOICE) }
            assertTrue("${q.id} explanation", q.explanation.length <= MAX_EXPLANATION)
        }
    }

    // Wrong legal information must never ship: every question cites an article and records
    // when its facts were last compared with the current law text.
    @Test
    fun everyQuestionCitesItsSource() {
        bank.questions.forEach { q ->
            assertTrue(
                "${q.id} basis must cite an article: ${q.basis}",
                ARTICLE.containsMatchIn(q.basis)
            )
            val checked = try {
                LocalDate.parse(q.checked)
            } catch (e: DateTimeParseException) {
                throw AssertionError("${q.id} checked is not a date: ${q.checked}", e)
            }
            assertTrue("${q.id} checked is in the future", !checked.isAfter(LocalDate.now()))
        }
    }

    // Choices are shuffled every round, so none may depend on its position.
    @Test
    fun choicesDoNotDependOnPosition() {
        bank.questions.flatMap { q -> q.choices.map { q.id to it } }.forEach { (id, choice) ->
            assertTrue("$id: $choice", POSITIONAL.none { it in choice })
        }
    }

    @Test
    fun everyLicenseHasQuestions() {
        License.entries.forEach { license ->
            assertTrue("$license", bank.byLicense(license).size >= MIN_PER_LICENSE)
        }
    }

    // A picture must be drawable: a real lane count, every vehicle on the road, and exactly one
    // vehicle the question is about.
    @Test
    fun scenesAreDrawable() {
        val scenes = bank.questions.mapNotNull { q -> q.scene?.let { q.id to it } }
        assertTrue("no picture questions", scenes.isNotEmpty())
        scenes.forEach { (id, scene) ->
            assertTrue("$id lanes", scene.lanes in MIN_LANES..MAX_LANES)
            assertEquals("$id subject", 1, scene.vehicles.count { it.subject })
            scene.vehicles.forEach { v ->
                assertTrue("$id lane ${v.lane}", v.lane in 1..scene.lanes)
                assertTrue("$id at ${v.at}", v.at in 0f..1f)
            }
            // Vehicles in one lane must not overlap even on the shortest road.
            scene.vehicles.groupBy { it.lane }.values.forEach { lane ->
                lane.map { it.row(Scene.MIN_ROWS) until it.row(Scene.MIN_ROWS) + it.length }
                    .sortedBy { it.first }
                    .zipWithNext()
                    .forEach { (a, b) -> assertTrue("$id overlap $a $b", a.last < b.first) }
            }
        }
    }

    // An intersection picture: the player's car comes from the bottom and is the only one with a
    // drawn way through, no two cars share a spot, and the signal and sign can be drawn.
    @Test
    fun crossingsAreDrawable() {
        val crossings = bank.questions.mapNotNull { q -> q.crossing?.let { q to it } }
        assertTrue("no intersection pictures", crossings.isNotEmpty())
        crossings.forEach { (q, crossing) ->
            val id = q.id
            assertTrue("$id has two pictures", q.scene == null)
            val subjects = crossing.cars.filter { it.subject }
            assertEquals("$id subject", 1, subjects.size)
            assertEquals("$id subject road", Arm.South, subjects.single().from)
            assertTrue("$id turn", crossing.cars.all { it.subject || it.turn == null })
            val spots = crossing.cars.map { it.from to it.at }
            assertEquals("$id cars overlap", spots.size, spots.toSet().size)
            crossing.signal?.let {
                assertTrue("$id lamps", it.lamps in Signal.LAMP_COUNTS)
                assertTrue("$id arrow", it.lit != Lamp.Arrow || it.lamps == 4)
            }
            crossing.sign?.let { assertTrue("$id sign $it", RoadSign.of(it) != null) }
        }
    }

    @Test
    fun pictureAndTextQuestionsMixInACourse() {
        val highway = bank.byLicense(License.Highway)
        assertTrue(highway.any { it.scene != null })
        assertTrue(highway.any { it.scene == null })
        val intersection = bank.byLicense(License.Intersection)
        assertTrue(intersection.any { it.crossing != null })
        assertTrue(intersection.any { it.crossing == null })
    }

    private companion object {
        const val MAX_PROMPT = 40
        const val MAX_CHOICE = 24
        const val MAX_EXPLANATION = 80
        const val MIN_PER_LICENSE = 3
        const val MIN_LANES = 2
        const val MAX_LANES = 4
        val ARTICLE = Regex(
            "^(도로교통법( 시행령| 시행규칙)?|특정범죄 가중처벌 등에 관한 법률) (제\\d+조|별표 \\d+)"
        )
        val POSITIONAL = listOf("위의", "위 보기", "모두 맞", "모두 정답", "모두 틀", "정답 없음", "보기 중")
    }
}
