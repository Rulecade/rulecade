package io.github.rulecade.navi

import io.github.rulecade.catalog.Catalog
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// Guards which navigation events find which catalog signs.
class RoadSignsTest {

    @Test
    fun eventsFindTheirSigns() {
        assertEquals(RoadSigns.CHILDREN, RoadSigns.forCaution("SCHOOL_ZONE"))
        assertEquals(RoadSigns.NO_STOPPING, RoadSigns.forCaution("NO_PARKING_ZONE"))
        assertEquals(RoadSigns.CHILDREN, RoadSigns.forCamera("SPEED_CAMERA_SCHOOL_ZONE"))
        assertEquals(RoadSigns.MAX_SPEED, RoadSigns.forSpeedLimit(50))
        assertEquals(RoadSigns.ROUNDABOUT, RoadSigns.forTurn("TURN_EXIT_ROUNDABOUT"))
    }

    @Test
    fun otherEventsFindNothing() {
        assertNull(RoadSigns.forCaution("SPEED_BUMP"))
        assertNull(RoadSigns.forCaution("TRAFFIC_LIGHT_VIOLATION_ENFORCEMENT"))
        assertNull(RoadSigns.forCamera("SPEED_CAMERA_FIX"))
        assertNull(RoadSigns.forSpeedLimit(0))
        assertNull(RoadSigns.forTurn("TURN_ENTER_IC_R"))
    }

    @Test
    fun foundSignsAreInTheCatalog() {
        val catalog = Catalog.parse(File("src/main/assets/${Catalog.ASSET_PATH}").readText())
        listOf(RoadSigns.CHILDREN, RoadSigns.NO_STOPPING, RoadSigns.MAX_SPEED, RoadSigns.ROUNDABOUT)
            .forEach { assertNotNull(it, catalog.sign(it)) }
    }
}
