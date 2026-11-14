package io.github.rulecade.vehicle

import ai.pleos.playground.vehicle.Vehicle
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

// Whether the car is charging (the EV charge switch), from the Vehicle SDK. The SDK has no
// listener for the switch, so it is read every few seconds while the app runs.
class ChargeWatcher(context: Context, private val onCharging: (Boolean) -> Unit) {

    private val vehicle = Vehicle(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())

    private val poll = object : Runnable {
        override fun run() {
            read()
            handler.postDelayed(this, PERIOD_MILLIS)
        }
    }

    // The first read waits for the vehicle service to bind; reads before that never answer.
    fun start() {
        try {
            vehicle.initialize()
        } catch (e: Exception) {
            Log.w(TAG, "vehicle unavailable", e)
            return
        }
        handler.postDelayed(poll, FIRST_MILLIS)
    }

    fun stop() {
        handler.removeCallbacks(poll)
        try {
            vehicle.release()
        } catch (e: Exception) {
            Log.w(TAG, "vehicle release failed", e)
        }
    }

    private fun read() {
        try {
            vehicle.getEvBattery().getEvChargeSwitchState(
                onComplete = { on -> handler.post { onCharging(on == true) } },
                onFailed = { Log.w(TAG, "charge switch unavailable", it) }
            )
        } catch (e: Exception) {
            Log.w(TAG, "charge switch unavailable", e)
        }
    }

    private companion object {
        const val TAG = "ChargeWatcher"
        const val FIRST_MILLIS = 2_000L
        const val PERIOD_MILLIS = 10_000L
    }
}
