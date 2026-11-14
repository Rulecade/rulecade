package io.github.rulecade.debug

import ai.pleos.playground.navi.constants.CameraType
import ai.pleos.playground.navi.constants.CautionPointType
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.CameraAlert
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.TBTInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.rulecade.navi.NaviWatcher

// Debug builds only: the emulator has no route to drive, so `scripts/emu navi` replays a short
// drive into the running app's navigation listener, with the data types NaviHelper sends.
class MockDriveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val watcher = NaviWatcher.active ?: return
        watcher.forgetRecent()
        with(watcher.listener) {
            onCautionPointInfo(
                listOf(CautionPoint("mock-school", CautionPointType.SCHOOL_ZONE, 300))
            )
            onCameraAlert(
                listOf(
                    CameraAlert(
                        "mock-camera",
                        CameraType.SPEED_CAMERA_SCHOOL_ZONE,
                        true,
                        30,
                        200,
                        0
                    )
                )
            )
            onCautionPointInfo(
                listOf(CautionPoint("mock-no-parking", CautionPointType.NO_PARKING_ZONE, 120))
            )
            onCurrentRoadSpeedLimit(50)
            onTBTInfo(listOf(TBTInfo(TurnType.TURN_EXIT_ROUNDABOUT, 40.0, "mock roundabout")))
        }
    }
}
