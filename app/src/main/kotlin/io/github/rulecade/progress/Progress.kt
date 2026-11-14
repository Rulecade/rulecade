package io.github.rulecade.progress

import io.github.rulecade.quiz.Answer
import io.github.rulecade.quiz.License
import io.github.rulecade.quiz.Question
import io.github.rulecade.quiz.QuestionBank
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PlayMode {
    @SerialName("ranked")
    Ranked,

    @SerialName("practice")
    Practice
}

@Serializable
data class QuestionStats(
    val rankedRight: Int = 0,
    val rankedWrong: Int = 0,
    val practiceRight: Int = 0,
    val practiceWrong: Int = 0,
    // Where the question was last answered wrong; unknown in saves from before it was kept.
    val lastMissed: PlayMode? = null
)

// One player's progress. Only ranked answers count toward licenses; sign grades are derived
// from `stats` by the catalog, so the raw counts are kept rather than grades.
@Serializable
data class Progress(
    val stats: Map<String, QuestionStats> = emptyMap(),
    val notebook: List<String> = emptyList(),
    val discoveredSigns: Set<String> = emptySet(),
    val licenses: Map<License, String> = emptyMap(),
    // Signs found on the road since home last showed them, in the order found.
    val recentSigns: List<String> = emptyList(),
    val recentShown: Boolean = false
) {
    fun isSolved(id: String) = (stats[id]?.rankedRight ?: 0) > 0

    fun solvedCount(questions: List<Question>) = questions.count { isSolved(it.id) }

    // Share of ranked answers that were right, in percent; null before any ranked answer.
    fun rankedAccuracy(questions: List<Question>): Int? {
        val counts = questions.mapNotNull { stats[it.id] }
        val right = counts.sumOf { it.rankedRight }
        val all = right + counts.sumOf { it.rankedWrong }
        return if (all == 0) null else (right * 100f / all).roundToInt()
    }

    // Unsolved questions come first so the cumulative license stays reachable.
    fun rankedPick(questions: List<Question>, count: Int, random: Random = Random): List<Question> {
        val (unsolved, solved) = questions.shuffled(random).partition { !isSolved(it.id) }
        return (unsolved + solved).take(count)
    }

    // A whole ranked round is recorded at once; a round left early is never recorded.
    fun recordRanked(answers: List<Answer>) =
        answers.fold(this) { progress, answer -> progress.record(answer, ranked = true) }

    fun recordPractice(answer: Answer) = record(answer, ranked = false)

    // A sign passed on the road. After home has shown the recent ones, the next find starts anew.
    fun discover(sign: String): Progress {
        val recent = if (recentShown) emptyList() else recentSigns
        return copy(
            discoveredSigns = discoveredSigns + sign,
            recentSigns = if (sign in recent) recent else recent + sign,
            recentShown = false
        )
    }

    fun showRecent() = if (recentShown || recentSigns.isEmpty()) this else copy(recentShown = true)

    fun unawardedLicenses(bank: QuestionBank) = License.entries.filter { license ->
        val questions = bank.byLicense(license)
        license !in licenses && questions.isNotEmpty() && questions.all { isSolved(it.id) }
    }

    // Awards stay when questions are added to a course later.
    fun award(earned: Collection<License>, date: LocalDate) =
        copy(licenses = licenses + earned.associateWith { date.toString() })

    private fun record(answer: Answer, ranked: Boolean): Progress {
        val id = answer.item.question.id
        val old = stats[id] ?: QuestionStats()
        val new = when {
            ranked && answer.correct -> old.copy(rankedRight = old.rankedRight + 1)
            ranked -> old.copy(rankedWrong = old.rankedWrong + 1, lastMissed = PlayMode.Ranked)
            answer.correct -> old.copy(practiceRight = old.practiceRight + 1)
            else -> old.copy(practiceWrong = old.practiceWrong + 1, lastMissed = PlayMode.Practice)
        }
        val notebook = when {
            !answer.correct -> listOf(id) + (notebook - id)
            !ranked -> notebook - id
            else -> notebook
        }
        return copy(stats = stats + (id to new), notebook = notebook)
    }
}

// Everything saved on the device. Each profile keeps its own progress; the first one has no
// stored name (the app calls it "나"), the ones added later have the name the player typed.
@Serializable
data class SaveData(
    val version: Int = VERSION,
    val activeProfile: String = DEFAULT_PROFILE,
    val profiles: Map<String, Progress> = emptyMap(),
    val names: Map<String, String> = emptyMap()
) {
    val progress: Progress get() = profiles[activeProfile] ?: Progress()

    // The first profile, then the added ones in the order they were made.
    val profileIds: List<String> get() = (listOf(DEFAULT_PROFILE) + names.keys).distinct()

    val canAddProfile: Boolean get() = profileIds.size < MAX_PROFILES

    fun withProgress(progress: Progress) = copy(profiles = profiles + (activeProfile to progress))

    fun progressOf(id: String) = profiles[id] ?: Progress()

    fun switchTo(id: String) = if (id in profileIds) copy(activeProfile = id) else this

    // A new profile starts empty and becomes the active one.
    fun addProfile(name: String): SaveData {
        val clean = name.trim().take(MAX_NAME)
        if (clean.isEmpty() || !canAddProfile) return this
        val id = generateSequence(profileIds.size + 1) { it + 1 }
            .map { "profile-$it" }
            .first { it !in profileIds }
        return copy(activeProfile = id, names = names + (id to clean))
    }

    companion object {
        const val VERSION = 1
        const val DEFAULT_PROFILE = "default"
        const val MAX_PROFILES = 4
        const val MAX_NAME = 6
    }
}
