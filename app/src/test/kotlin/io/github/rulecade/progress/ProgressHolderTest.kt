package io.github.rulecade.progress

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgressHolderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val store get() = ProgressStore(File(folder.root, ProgressStore.FILE_NAME))

    @Test
    fun startsFromTheSavedFile() {
        val saved = SaveData().withProgress(Progress(discoveredSigns = setOf("stop")))
        store.save(saved)

        assertEquals(saved, ProgressHolder(store) { it.run() }.data)
    }

    @Test
    fun updatesShowAtOnceAndAreWrittenInOrder() {
        val queued = mutableListOf<Runnable>()
        val holder = ProgressHolder(store) { queued += it }

        holder.update { it.discover("stop") }
        holder.update { it.discover("yield") }
        assertEquals(setOf("stop", "yield"), holder.progress.discoveredSigns)
        assertEquals(SaveData(), store.load())

        queued.forEach { it.run() }
        assertEquals(holder.data, store.load())
    }

    // The road is the car's: a sign found counts for every profile, the active one stays.
    @Test
    fun discoveriesReachEveryProfile() {
        val holder = ProgressHolder(store) { it.run() }
        holder.addProfile("동생")
        val second = holder.data.activeProfile
        holder.switchProfile(SaveData.DEFAULT_PROFILE)
        holder.discover("children")
        assertEquals(SaveData.DEFAULT_PROFILE, holder.data.activeProfile)
        holder.data.profileIds.forEach {
            assertEquals(setOf("children"), holder.data.progressOf(it).discoveredSigns)
        }
        assertEquals(listOf("children"), holder.data.progressOf(second).recentSigns)
        assertEquals(holder.data, store.load())
    }
}
