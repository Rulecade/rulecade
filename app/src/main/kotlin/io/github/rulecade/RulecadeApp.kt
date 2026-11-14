package io.github.rulecade

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import io.github.rulecade.catalog.SignCategory
import io.github.rulecade.catalog.grade
import io.github.rulecade.catalog.solvedCount
import io.github.rulecade.library.LibraryTab
import io.github.rulecade.quiz.License
import io.github.rulecade.ui.catalog.CatalogScreen
import io.github.rulecade.ui.catalog.SignScreen
import io.github.rulecade.ui.home.HomeScreen
import io.github.rulecade.ui.home.courseSummaries
import io.github.rulecade.ui.library.LibraryScreen
import io.github.rulecade.ui.library.LocalLibraryLink
import io.github.rulecade.ui.library.TopicScreen
import io.github.rulecade.ui.license.CardSign
import io.github.rulecade.ui.license.LicenseCardScreen
import io.github.rulecade.ui.license.RoadSign
import io.github.rulecade.ui.license.earnedText
import io.github.rulecade.ui.opening.OpeningOverlay
import io.github.rulecade.ui.opening.OpeningState
import io.github.rulecade.ui.profile.ProfileDialog
import io.github.rulecade.ui.profile.ProfileSummary
import io.github.rulecade.ui.quiz.PracticeScreen
import io.github.rulecade.ui.quiz.RankedQuizScreen
import io.github.rulecade.ui.theme.RulecadeTheme

enum class Destination { Home, Practice, Quiz, Library, Catalog }

@Composable
fun RulecadeApp(model: RulecadeModel, playOpening: Boolean) {
    val opening = remember { OpeningState(playOpening) }
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    // A practice started from a library topic or a sign goes back there when it is left: what
    // was showing then, or null when it started anywhere else.
    var practiceTopic by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceSign by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceBelow by rememberSaveable { mutableStateOf<Destination?>(null) }
    val goHome = {
        destination = Destination.Home
        practiceBelow = null
    }
    BackHandler(enabled = destination != Destination.Home, onBack = goHome)
    LaunchedEffect(opening) { opening.play() }
    val courses = courseSummaries(model.bank, model.progress.progress)
    val data = model.progress.data
    val me = stringResource(R.string.profile_me)
    val nameOf = { id: String -> data.names[id] ?: me }
    val owner = nameOf(data.activeProfile)
    // Who plays: asked once a run when there are several profiles or someone sits beside the
    // driver, on home only, and any time from the home chip.
    var askProfile by rememberSaveable { mutableStateOf(false) }
    val cardSigns: (License) -> List<CardSign> = { license ->
        model.catalog.signsFor(license, model.bank).mapNotNull { s ->
            RoadSign.of(s.number)?.let { CardSign(it, model.progress.progress.grade(s)) }
        }
    }
    val practice = model.practiceQuiz
    var openLicense by rememberSaveable { mutableStateOf<License?>(null) }
    val shown = openLicense
    val earned = shown?.let { model.progress.progress.licenses[it] }
    // A library topic opens over whatever is showing; back returns there.
    var openTopic by rememberSaveable { mutableStateOf<String?>(null) }
    val topic = openTopic?.let { model.library.topic(it) }
    var libraryTab by rememberSaveable { mutableStateOf<LibraryTab?>(null) }
    // A sign opens over the catalog; a library topic opened from it comes back to it.
    var openSign by rememberSaveable { mutableStateOf<String?>(null) }
    val sign = openSign?.let { model.catalog.sign(it) }
    var catalogTab by rememberSaveable { mutableStateOf<SignCategory?>(null) }
    // Both the topic and the sign close so the round shows; leaving it opens them again.
    val practiceHere: (String, List<String>) -> Unit = { title, ids ->
        practice.startTopic(title, ids)
        practiceTopic = openTopic
        practiceSign = openSign
        // A topic opened from a practice round replaces that round, so it can't go back to it.
        practiceBelow = destination.takeUnless { it == Destination.Practice } ?: Destination.Home
        openTopic = null
        openSign = null
        destination = Destination.Practice
    }
    val leavePractice = {
        val below = practiceBelow
        if (below == null) {
            goHome()
        } else {
            openTopic = practiceTopic
            openSign = practiceSign
            destination = below
            practiceBelow = null
        }
    }
    val libraryLink: (String) -> (() -> Unit)? = { id ->
        model.library.topicFor(id)?.let { t -> { openTopic = t.id } }
    }

    CompositionLocalProvider(LocalLibraryLink provides libraryLink) {
        Box(Modifier.fillMaxSize().background(RulecadeTheme.colors.backgroundPrimary)) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = opening.homeAlpha }) {
                // The card view replaces the screen, so nothing behind it can be tapped or read.
                if (shown != null && earned != null) {
                    LicenseCardScreen(
                        license = shown,
                        owner = owner,
                        earned = earnedText(earned),
                        signs = cardSigns(shown),
                        solvedTotal = model.bank.byLicense(shown).size,
                        onClose = { openLicense = null }
                    )
                } else if (topic != null) {
                    TopicScreen(
                        topic = topic,
                        checked = model.library.checked,
                        questionCount = topic.questions.count { model.bank.byId(it) != null },
                        onPractice = { practiceHere(topic.title, topic.questions) },
                        onBack = { openTopic = null }
                    )
                } else if (sign != null) {
                    val progress = model.progress.progress
                    SignScreen(
                        sign = sign,
                        grade = progress.grade(sign),
                        solved = progress.solvedCount(sign),
                        checked = model.catalog.checked,
                        onPractice = { practiceHere(sign.name, sign.questions) },
                        onBack = { openSign = null }
                    )
                } else {
                    when (destination) {
                        Destination.Home -> HomeScreen(
                            courses = courses,
                            notebookCount = practice.notebook.size,
                            onPractice = {
                                practice.startMixed()
                                destination = Destination.Practice
                            },
                            onRanked = { destination = Destination.Quiz },
                            onNotebook = {
                                practice.tab = null
                                destination = Destination.Practice
                            },
                            onCourses = {
                                practice.tab = License.entries.first()
                                destination = Destination.Practice
                            },
                            onOpen = { destination = it },
                            onOpenLicense = { openLicense = it },
                            recent = model.progress.progress.recentSigns.mapNotNull(
                                model.catalog::sign
                            ),
                            onOpenSign = { openSign = it.id },
                            onRecentShown = { model.progress.update { it.showRecent() } },
                            profileName = owner,
                            onProfiles = { askProfile = true },
                            charging = model.charging,
                            onCharging = {
                                practice.startCharging()
                                destination = Destination.Practice
                            },
                            wordmarkModifier = Modifier.onGloballyPositioned {
                                opening.target = it.positionInRoot()
                            }
                        )
                        Destination.Practice -> PracticeScreen(
                            practice,
                            model.bank,
                            model.progress.progress,
                            onHome = goHome,
                            onBack = leavePractice
                        )
                        Destination.Quiz -> RankedQuizScreen(
                            model.rankedQuiz,
                            courses,
                            owner,
                            cardSigns,
                            goHome
                        ) { answers ->
                            practice.startMissed(answers)
                            destination = Destination.Practice
                        }
                        Destination.Library -> LibraryScreen(
                            library = model.library,
                            tab = libraryTab,
                            onTab = { libraryTab = it },
                            solved = { t ->
                                val questions = t.questions.mapNotNull(model.bank::byId)
                                model.progress.progress.solvedCount(questions) to questions.size
                            },
                            onOpen = { openTopic = it.id },
                            onBack = goHome
                        )
                        Destination.Catalog -> CatalogScreen(
                            catalog = model.catalog,
                            progress = model.progress.progress,
                            category = catalogTab,
                            onCategory = { catalogTab = it },
                            onOpen = { openSign = it.id },
                            onBack = goHome
                        )
                    }
                }
            }
            val onHome = destination == Destination.Home &&
                shown == null &&
                topic == null &&
                sign == null
            LaunchedEffect(opening.isRunning, onHome, model.passenger) {
                if (opening.isRunning || !onHome) return@LaunchedEffect
                if (!model.profilesAsked) {
                    model.profilesAsked = true
                    if (data.profileIds.size > 1) askProfile = true
                }
                if (model.passenger && !model.passengerAsked) {
                    model.passengerAsked = true
                    askProfile = true
                }
            }
            if (askProfile && onHome) {
                ProfileDialog(
                    profiles = data.profileIds.map {
                        ProfileSummary(it, nameOf(it), data.progressOf(it).licenses.size)
                    },
                    active = data.activeProfile,
                    passenger = model.passenger,
                    canAdd = data.canAddProfile,
                    onPick = {
                        model.progress.switchProfile(it)
                        askProfile = false
                    },
                    onAdd = {
                        model.progress.addProfile(it)
                        askProfile = false
                    },
                    onClose = { askProfile = false }
                )
            }
            if (opening.isRunning) OpeningOverlay(opening)
        }
    }
}
