package io.github.rulecade.progress

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// Keeps SaveData in one small JSON file. Call save off the main thread.
class ProgressStore(private val file: File) {

    @Synchronized
    fun load(): SaveData {
        if (!file.exists()) return SaveData()
        return try {
            json.decodeFromString<SaveData>(file.readText())
        } catch (e: SerializationException) {
            setAside()
        }
    }

    // Written to a temporary file first, so a crash mid-write never leaves a half file.
    @Synchronized
    fun save(data: SaveData) {
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        FileOutputStream(temp).use { out ->
            out.write(json.encodeToString(SaveData.serializer(), data).toByteArray())
            out.fd.sync()
        }
        Files.move(
            temp.toPath(),
            file.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
    }

    // A broken file must not crash the app; it is kept beside the new one for inspection.
    private fun setAside(): SaveData {
        Files.move(
            file.toPath(),
            File(file.path + ".bad").toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
        return SaveData()
    }

    companion object {
        const val FILE_NAME = "progress.json"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
