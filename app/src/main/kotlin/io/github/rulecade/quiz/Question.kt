package io.github.rulecade.quiz

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Road situations a license is earned for. Every question belongs to exactly one.
@Serializable
enum class License {
    @SerialName("highway")
    Highway,

    @SerialName("intersection")
    Intersection,

    @SerialName("school_zone")
    SchoolZone,

    @SerialName("city")
    City
}

// Ranked rounds draw every difficulty (unsolved first); the tag drives practice and display.
@Serializable
enum class Difficulty {
    @SerialName("easy")
    Easy,

    @SerialName("normal")
    Normal,

    @SerialName("hard")
    Hard
}

// `basis` cites the article, and `checked` is the date the facts were compared with the
// current law text.
@Serializable
data class Question(
    val id: String,
    val license: License,
    val prompt: String,
    val choices: List<String>,
    val answer: Int,
    val explanation: String,
    val difficulty: Difficulty,
    val checked: String,
    val basis: String,
    val penalty: String? = null,
    val sign: String? = null,
    // A drawn road for picture questions; the prompt still reads on its own without it.
    val scene: Scene? = null,
    // The same for an intersection; a question has at most one of the two.
    val crossing: Crossing? = null
)

@Serializable
enum class VehicleKind {
    @SerialName("car")
    Car,

    @SerialName("truck")
    Truck
}

// One vehicle on the road: its lane counted from the left (1 is next to the median) and how far
// down the picture it sits (0 at the top, 1 at the bottom). Traffic moves up.
@Serializable
data class SceneVehicle(
    val kind: VehicleKind,
    val lane: Int,
    val at: Float,
    val subject: Boolean = false
) {
    // Length in road cells, and the first cell row on a road of [rows] rows.
    val length: Int get() = if (kind == VehicleKind.Truck) TRUCK_LENGTH else CAR_LENGTH

    fun row(rows: Int) = ((rows - length) * at).toInt().coerceIn(0, maxOf(0, rows - length))

    companion object {
        const val CAR_LENGTH = 6
        const val TRUCK_LENGTH = 10
    }
}

// A stretch of one-way highway seen from above.
@Serializable
data class Scene(val lanes: Int, val vehicles: List<SceneVehicle>) {
    companion object {
        // The road is drawn at least this many cells long.
        const val MIN_ROWS = 20
    }
}

// The road a vehicle comes from, seen from above with the player's car coming from the bottom.
@Serializable
enum class Arm {
    @SerialName("south")
    South,

    @SerialName("north")
    North,

    @SerialName("east")
    East,

    @SerialName("west")
    West
}

// Waiting to enter the intersection (behind the stop line where there is one), or inside it.
@Serializable
enum class Spot {
    @SerialName("waiting")
    Waiting,

    @SerialName("inside")
    Inside
}

@Serializable
enum class Turn {
    @SerialName("left")
    Left,

    @SerialName("straight")
    Straight,

    @SerialName("right")
    Right
}

// A Korean horizontal signal head: red, yellow, (green left arrow,) green from the left.
@Serializable
enum class Lamp {
    @SerialName("red")
    Red,

    @SerialName("yellow")
    Yellow,

    @SerialName("arrow")
    Arrow,

    @SerialName("green")
    Green
}

@Serializable
data class Signal(val lamps: Int, val lit: Lamp) {
    companion object {
        val LAMP_COUNTS = 3..4
    }
}

// A car at an intersection; the subject's turn is drawn as its path.
@Serializable
data class CrossingCar(
    val from: Arm,
    val at: Spot,
    val subject: Boolean = false,
    val turn: Turn? = null
)

// A four-way intersection of two-way roads. With a signal it also has stop lines and crosswalks.
@Serializable
data class Crossing(val signal: Signal? = null, val sign: Int? = null, val cars: List<CrossingCar>)

@Serializable
data class QuestionBank(val version: Int, val updated: String, val questions: List<Question>) {
    fun byLicense(license: License) = questions.filter { it.license == license }

    fun byId(id: String) = questions.firstOrNull { it.id == id }

    companion object {
        const val ASSET_PATH = "content/questions.json"

        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): QuestionBank = json.decodeFromString(text)
    }
}
