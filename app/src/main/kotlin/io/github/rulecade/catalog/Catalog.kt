package io.github.rulecade.catalog

import io.github.rulecade.progress.Progress
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.QuestionBank
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Sign categories of Annex 6 of the Road Traffic Act Enforcement Rule.
@Serializable
enum class SignCategory {
    @SerialName("caution")
    Caution,

    @SerialName("regulation")
    Regulation,

    @SerialName("instruction")
    Instruction
}

// A safety sign with its annex number, its meaning quoted from the annex, and the questions
// that ask the rule it shows.
@Serializable
data class Sign(
    val id: String,
    val number: Int,
    val category: SignCategory,
    val name: String,
    val meaning: String,
    val questions: List<String>
)

enum class Grade { Uncollected, Discovered, Collected, Mastered }

// Only ranked answers count: one linked question solved collects a sign, all of them master it.
// Before that, a sign passed on the road is discovered.
fun Progress.grade(sign: Sign): Grade {
    val solved = solvedCount(sign)
    return when {
        solved == 0 && sign.id in discoveredSigns -> Grade.Discovered
        solved == 0 -> Grade.Uncollected
        solved == sign.questions.size -> Grade.Mastered
        else -> Grade.Collected
    }
}

fun Progress.solvedCount(sign: Sign) = sign.questions.count { isSolved(it) }

// Facts were compared with the current annex on `checked`.
@Serializable
data class Catalog(val version: Int, val checked: String, val signs: List<Sign>) {
    fun byCategory(category: SignCategory) = signs.filter { it.category == category }

    fun sign(id: String) = signs.firstOrNull { it.id == id }

    // The signs a license card shows: those linked to that course's questions, in catalog order.
    fun signsFor(license: License, bank: QuestionBank) =
        signs.filter { sign -> sign.questions.any { bank.byId(it)?.license == license } }

    companion object {
        const val ASSET_PATH = "content/signs.json"

        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): Catalog = json.decodeFromString(text)
    }
}
