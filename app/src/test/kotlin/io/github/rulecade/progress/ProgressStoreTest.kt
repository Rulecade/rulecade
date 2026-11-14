package io.github.rulecade.progress

import io.github.rulecade.quiz.License
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgressStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File get() = File(folder.root, ProgressStore.FILE_NAME)

    private val data = SaveData().withProgress(
        Progress(
            stats = mapOf("h1" to QuestionStats(rankedRight = 2, practiceWrong = 1)),
            notebook = listOf("c1", "h2"),
            discoveredSigns = setOf("school-zone"),
            licenses = mapOf(License.Highway to "2026-11-10")
        )
    )

    @Test
    fun missingFileStartsEmpty() {
        assertEquals(SaveData(), ProgressStore(file).load())
    }

    @Test
    fun savedDataSurvivesARestart() {
        ProgressStore(file).save(data)

        assertEquals(data, ProgressStore(file).load())
    }

    @Test
    fun fileUsesStableNames() {
        ProgressStore(file).save(data)
        val text = file.readText()

        assertTrue(text, text.contains("\"version\":1"))
        assertTrue(text, text.contains("\"licenses\":{\"highway\":\"2026-11-10\"}"))
    }

    @Test
    fun aSaveWithoutTheLastMissStillLoads() {
        file.writeText(
            "{\"version\":1,\"activeProfile\":\"default\",\"profiles\":{\"default\":" +
                "{\"stats\":{\"h1\":{\"rankedWrong\":1}},\"notebook\":[\"h1\"]}}}"
        )
        val progress = ProgressStore(file).load().progress

        assertEquals(QuestionStats(rankedWrong = 1), progress.stats["h1"])
        assertEquals(listOf("h1"), progress.notebook)
    }

    @Test
    fun theLastMissIsSavedByName() {
        val missed = SaveData().withProgress(
            Progress(
                stats = mapOf(
                    "h1" to QuestionStats(practiceWrong = 1, lastMissed = PlayMode.Practice)
                )
            )
        )
        ProgressStore(file).save(missed)

        assertTrue(file.readText().contains("\"lastMissed\":\"practice\""))
        assertEquals(missed, ProgressStore(file).load())
    }

    @Test
    fun saveReplacesTheFileAndLeavesNoTemporaryFile() {
        val store = ProgressStore(file)
        store.save(SaveData())
        store.save(data)

        assertEquals(data, store.load())
        assertEquals(listOf(ProgressStore.FILE_NAME), folder.root.list()!!.toList())
    }

    @Test
    fun brokenFileIsSetAsideAndStartsEmpty() {
        val broken = listOf(
            "{not json",
            "",
            "{\"version\":\"one\"}",
            "{\"profiles\":{\"a\":{\"licenses\":{\"moon\":\"x\"}}}}"
        )
        broken.forEach { text ->
            file.writeText(text)
            val store = ProgressStore(file)

            assertEquals(text, SaveData(), store.load())
            assertFalse(file.exists())
            assertEquals(text, File(file.path + ".bad").readText())

            store.save(data)
            assertEquals(data, store.load())
        }
    }
}
