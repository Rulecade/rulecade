package io.github.rulecade.navi

// Navigation events that stand for a catalog sign, by the sign's meaning in Annex 6; every other
// event finds nothing. Event types are matched by their constant names.
object RoadSigns {
    const val CHILDREN = "children"
    const val NO_STOPPING = "no-stopping"
    const val MAX_SPEED = "max-speed"
    const val ROUNDABOUT = "roundabout"

    // A school zone has the children's routes 133 warns of; a no-parking zone forbids stopping
    // and parking, which is 218.
    fun forCaution(type: String) = when (type) {
        "SCHOOL_ZONE" -> CHILDREN
        "NO_PARKING_ZONE" -> NO_STOPPING
        else -> null
    }

    // School zone speed and signal cameras stand in a school zone.
    fun forCamera(type: String) = if ("SCHOOL_ZONE" in type) CHILDREN else null

    // A road with a speed limit has its maximum speed set (224); 0 means none is known.
    fun forSpeedLimit(kmh: Int) = if (kmh > 0) MAX_SPEED else null

    // Leaving a roundabout means the car went through one (109).
    fun forTurn(type: String) = if (type == "TURN_EXIT_ROUNDABOUT") ROUNDABOUT else null
}
