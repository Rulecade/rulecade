package io.github.rulecade.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Seat
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import android.content.Context
import android.util.Log

// Whether someone sits in the front passenger seat, from the Vehicle SDK, so the app can offer
// that person their own profile.
class SeatWatcher(context: Context, private val onPassenger: (Boolean) -> Unit) {

    private val vehicle = Vehicle(context.applicationContext)
    private var seat: Seat? = null
    private val passengerSeat = SeatArea(SeatValue.ROW_1_RIGHT)

    // On the Pleos Connect emulator the SDK reports false when the Virtual Console seats someone
    // (SEAT_OCCUPANCY 2, occupied) and true when it empties the seat (1, vacant), so false is
    // taken as occupied; null is unknown.
    private val listener = object : SeatOccupancyListener {
        override fun onSeatOccupancyUpdated(occupancy: Boolean?) {
            onPassenger(occupancy == false)
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "passenger seat unavailable", e)
        }
    }

    fun start() {
        try {
            vehicle.initialize()
            seat = vehicle.getSeat().also { it.registerSeatOccupancy(passengerSeat, listener) }
        } catch (e: Exception) {
            Log.w(TAG, "vehicle unavailable", e)
        }
    }

    fun stop() {
        try {
            seat?.unregisterSeatOccupancy(passengerSeat, listener)
            vehicle.release()
        } catch (e: Exception) {
            Log.w(TAG, "vehicle release failed", e)
        }
    }

    private companion object {
        const val TAG = "SeatWatcher"
    }
}
