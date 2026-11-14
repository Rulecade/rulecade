package io.github.rulecade.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The library tabs: the four courses, then fines and points.
@Serializable
enum class LibraryTab {
    @SerialName("highway")
    Highway,

    @SerialName("intersection")
    Intersection,

    @SerialName("school_zone")
    SchoolZone,

    @SerialName("city")
    City,

    @SerialName("penalty")
    Penalty
}

// A verbatim excerpt; "(…)" marks words left out.
@Serializable
data class LawQuote(val source: String, val text: String)

// What breaking the rule costs a passenger car, under the given condition.
@Serializable
data class Fine(val label: String, val value: String)

// The latest revision mark the current text shows for the quoted provision: its date (the
// promulgation date) and whether it was amended (개정) or added (신설).
@Serializable
data class Revision(val date: String, val mark: String, val of: String)

@Serializable
data class Topic(
    val id: String,
    val tab: LibraryTab,
    val title: String,
    val summary: String,
    val steps: List<String>,
    val law: List<LawQuote>,
    val fines: List<Fine>,
    val questions: List<String>,
    val revised: Revision? = null
)

// Facts were compared with the current law text on `checked`.
@Serializable
data class Library(val version: Int, val checked: String, val topics: List<Topic>) {
    fun byTab(tab: LibraryTab) = topics.filter { it.tab == tab }

    fun topic(id: String) = topics.firstOrNull { it.id == id }

    // The topic that explains a question, if any.
    fun topicFor(questionId: String) = topics.firstOrNull { questionId in it.questions }

    // Newest first.
    val changes: List<Topic> get() = topics.filter {
        it.revised != null
    }.sortedByDescending { it.revised!!.date }

    companion object {
        const val ASSET_PATH = "content/library.json"

        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): Library = json.decodeFromString(text)
    }
}
