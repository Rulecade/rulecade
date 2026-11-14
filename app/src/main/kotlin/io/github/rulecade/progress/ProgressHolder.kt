package io.github.rulecade.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executor

// Screens read progress from here; every change is written to disk in order on `writer`.
class ProgressHolder(private val store: ProgressStore, private val writer: Executor) {

    var data by mutableStateOf(store.load())
        private set

    val progress: Progress get() = data.progress

    // An unchanged progress is not written again.
    fun update(change: (Progress) -> Progress) {
        val changed = change(data.progress)
        if (changed == data.progress) return
        save(data.withProgress(changed))
    }

    // The road is the car's: a sign passed counts for every profile.
    fun discover(sign: String) = save(
        data.copy(profiles = data.profileIds.associateWith { data.progressOf(it).discover(sign) })
    )

    fun switchProfile(id: String) = save(data.switchTo(id))

    fun addProfile(name: String) = save(data.addProfile(name))

    private fun save(next: SaveData) {
        if (next == data) return
        data = next
        writer.execute { store.save(next) }
    }
}
