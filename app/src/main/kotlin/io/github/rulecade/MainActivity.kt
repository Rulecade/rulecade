package io.github.rulecade

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.github.rulecade.ui.theme.RulecadeTheme

// Rulecade is not declared Distraction Optimized, so Pleos itself covers it while the car is in
// gear and brings it back where it was once parked.
class MainActivity : ComponentActivity() {

    private val model: RulecadeModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The opening plays once per process: not on resume, rotation, or split-screen changes.
        val playOpening = savedInstanceState == null && !openingShown && animationsEnabled()
        openingShown = true
        setContent {
            RulecadeTheme {
                RulecadeApp(model = model, playOpening = playOpening)
            }
        }
    }

    private fun animationsEnabled() =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

    private companion object {
        var openingShown = false
    }
}
