package io.github.rulecade.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CameraAlert
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.SystemClock
import android.util.Log

// Listens to the navigation while the app runs (also while Pleos covers it on the road) and
// reports the catalog signs the events stand for. A sign found again within a few minutes is
// the same stretch of road, so it is reported once.
class NaviWatcher(context: Context, private val onSign: (String) -> Unit) {

    private val navi = NaviHelper(context.applicationContext)
    private val lastFound = mutableMapOf<String, Long>()

    internal val listener = object : NaviHelperEventListener {
        override fun onCautionPointInfo(cautionPointList: List<CautionPoint>) {
            cautionPointList.forEach { found(RoadSigns.forCaution(it.type.name)) }
        }

        override fun onCameraAlert(cameraAlertList: List<CameraAlert>) {
            cameraAlertList.forEach { found(RoadSigns.forCamera(it.type.name)) }
        }

        override fun onCurrentRoadSpeedLimit(speedLimit: Int) {
            found(RoadSigns.forSpeedLimit(speedLimit))
        }

        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
            tbtInfoList.forEach { found(RoadSigns.forTurn(it.type.name)) }
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "navigation error $errorCode")
        }
    }

    fun start() {
        active = this
        navi.initialize(
            onConnected = { navi.addListener(listener) },
            onFailed = { Log.w(TAG, "navigation unavailable", it) }
        )
    }

    fun stop() {
        if (active === this) active = null
        navi.removeListener(listener)
        navi.release()
    }

    // A replayed drive (debug builds) counts its signs again.
    @Synchronized
    internal fun forgetRecent() = lastFound.clear()

    @Synchronized
    private fun found(sign: String?) {
        sign ?: return
        val now = SystemClock.elapsedRealtime()
        val last = lastFound[sign]
        if (last != null && now - last < REPEAT_MILLIS) return
        lastFound[sign] = now
        onSign(sign)
    }

    internal companion object {
        private const val TAG = "NaviWatcher"
        private const val REPEAT_MILLIS = 10 * 60 * 1000L

        // The watcher of the running app, for the debug build's simulated drive.
        @Volatile
        var active: NaviWatcher? = null
    }
}
