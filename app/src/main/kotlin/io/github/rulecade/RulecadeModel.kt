package io.github.rulecade

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import io.github.rulecade.catalog.Catalog
import io.github.rulecade.library.Library
import io.github.rulecade.navi.NaviWatcher
import io.github.rulecade.progress.ProgressHolder
import io.github.rulecade.progress.ProgressStore
import io.github.rulecade.quiz.QuestionBank
import io.github.rulecade.ui.quiz.PracticeQuiz
import io.github.rulecade.ui.quiz.RankedQuiz
import io.github.rulecade.vehicle.ChargeWatcher
import io.github.rulecade.vehicle.SeatWatcher
import java.io.File
import java.util.concurrent.Executors

// Survives configuration changes such as the day/night switch, so a round in play is kept.
class RulecadeModel(app: Application) : AndroidViewModel(app) {

    val bank: QuestionBank = app.assets.open(QuestionBank.ASSET_PATH).bufferedReader().use {
        QuestionBank.parse(it.readText())
    }

    val library: Library = app.assets.open(Library.ASSET_PATH).bufferedReader().use {
        Library.parse(it.readText())
    }

    val catalog: Catalog = app.assets.open(Catalog.ASSET_PATH).bufferedReader().use {
        Catalog.parse(it.readText())
    }

    private val writer = Executors.newSingleThreadExecutor()

    val progress =
        ProgressHolder(ProgressStore(File(app.filesDir, ProgressStore.FILE_NAME)), writer)

    val rankedQuiz = RankedQuiz(bank, progress)

    val practiceQuiz = PracticeQuiz(bank, progress)

    private val main = Handler(Looper.getMainLooper())

    // Signs passed on the road while the app runs; progress only changes on the main thread.
    private val navi = NaviWatcher(app) { sign ->
        main.post { progress.discover(sign) }
    }.also { it.start() }

    // Someone in the passenger seat; the app offers profiles once per run when it turns true.
    var passenger by mutableStateOf(false)
        private set
    var passengerAsked = false
    var profilesAsked = false

    private val seat = SeatWatcher(app) { seated ->
        main.post { passenger = seated }
    }.also { it.start() }

    // While the car charges, home offers a ten-minute round.
    var charging by mutableStateOf(false)
        private set

    private val charge = ChargeWatcher(app) { charging = it }.also { it.start() }

    // Queued saves still run after shutdown.
    override fun onCleared() {
        charge.stop()
        seat.stop()
        navi.stop()
        writer.shutdown()
    }
}
