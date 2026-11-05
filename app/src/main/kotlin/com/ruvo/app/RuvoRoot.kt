package com.ruvo.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ruvo.data.AppState
import com.ruvo.engine.IndianFormat
import androidx.compose.foundation.layout.widthIn
import com.ruvo.data.RuvoGraph
import com.ruvo.design.AmbientBackdrop
import com.ruvo.design.Motion
import com.ruvo.design.StatusBarFade
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Space
import com.ruvo.engine.Bucket
import com.ruvo.engine.guide.Library
import com.ruvo.feature.goals.GoalsScreen
import com.ruvo.feature.goals.ReviewSheet
import com.ruvo.feature.goals.rememberTilt
import com.ruvo.feature.guide.BookReader
import com.ruvo.feature.guide.GuideActions
import com.ruvo.feature.guide.GuideScreen
import com.ruvo.feature.money.MoneyScreen
import com.ruvo.feature.money.TxnSheet
import com.ruvo.feature.onboarding.OnboardingFlow
import com.ruvo.feature.onboarding.PersonaPicker
import com.ruvo.feature.orb.ChatSheet
import com.ruvo.feature.orb.OrbMood
import com.ruvo.feature.orb.RuvoOrb
import com.ruvo.feature.today.BeforeYouBuySheet
import com.ruvo.feature.today.SortStackScreen
import com.ruvo.feature.today.TodayActions
import com.ruvo.feature.today.TodayScreen
import com.ruvo.feature.today.WhySheet
import kotlinx.coroutines.launch

enum class Tab(val label: String) { TODAY("Home"), MONEY("Money"), GOALS("Goals"), GUIDE("Guide") }

/** Which sheet or overlay is open. Only one at a time, so the screen never stacks dialogs. */
sealed interface Overlay {
    data object None : Overlay
    data object Why : Overlay
    data object Buy : Overlay
    data class Chat(val question: String? = null) : Overlay
    data object QuickAdd : Overlay
    data object Settings : Overlay
    data object Personas : Overlay
    data object Sort : Overlay
    /** A Guide book, opened full screen. Today's lesson cards open their book here too. */
    data class Book(val id: String) : Overlay
    data class Txn(val id: String, val split: Boolean = false) : Overlay
    data object Weekly : Overlay
    data object Profile : Overlay
    data object Monthly : Overlay
    data object Marketplace : Overlay
}

@Composable
fun RuvoRoot(state: AppState, request: LaunchRequest?, consumed: () -> Unit) {
    // The opening plays on every cold start, until it finishes or is tapped, and until the data is ready.
    val reduced = Ruvo.reducedMotion
    var splashDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(if (reduced) 500 else 3_600); splashDone = true }
    val showApp = state.ready && splashDone
    val unlocked by com.ruvo.data.Session.unlocked.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        if (showApp) {
            // Sign in first, then the first-run setup, then the app.
            when {
                !unlocked && !state.settings.hasAccount -> com.ruvo.feature.onboarding.CreateAccountScreen(suggestedName = state.settings.name)
                !unlocked -> com.ruvo.feature.onboarding.SignInScreen(state.settings)
                !state.settings.onboarded -> OnboardingFlow(onDone = {})
                else -> MainShell(state, request, consumed)
            }
        }
        // The splash sits on top and fades away over the app.
        AnimatedVisibility(!showApp, enter = fadeIn(), exit = fadeOut(Motion.gentle())) {
            com.ruvo.design.RuvoSplash(
                onSkip = { splashDone = true },
            )
        }
    }
}

@Composable
private fun MainShell(state: AppState, request: LaunchRequest?, consumed: () -> Unit) {
    val c = Ruvo.colors
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    val scope = rememberCoroutineScope()
    val tilt by rememberTilt()
    val reduced = Ruvo.reducedMotion
    var lastBook by remember { mutableStateOf<String?>(null) }
    var newGoal by remember { mutableStateOf(false) }

    LaunchedEffect(request) {
        when (request) {
            LaunchRequest.QuickAdd -> overlay = Overlay.QuickAdd
            LaunchRequest.Sort -> overlay = Overlay.Sort
            LaunchRequest.Chat -> overlay = Overlay.Chat()
            is LaunchRequest.Txn -> overlay = Overlay.Txn(request.id, request.split)
            null -> {}
        }
        if (request != null) consumed()
    }
    BackHandler(enabled = tab != Tab.TODAY && overlay == Overlay.None) { tab = Tab.TODAY }

    val actions = TodayActions(
        openWhy = { overlay = Overlay.Why },
        openSort = { overlay = Overlay.Sort },
        openBuy = { overlay = Overlay.Buy },
        openSettings = { overlay = Overlay.Settings },
        openMoney = { tab = Tab.MONEY },
        openGoals = { tab = Tab.GOALS },
        openPersonas = { overlay = Overlay.Personas },
        openTxn = { overlay = Overlay.Txn(it) },
        openLesson = { overlay = Overlay.Book(it) },
        openWeekly = { overlay = Overlay.Weekly },
        openMonthly = { overlay = Overlay.Monthly },
        openChat = { overlay = Overlay.Chat(it) },
        openGuide = { overlay = Overlay.Marketplace },
        openAdd = { overlay = Overlay.QuickAdd },
        openProfile = { overlay = Overlay.Profile },
    )

    val guide = GuideActions(
        openBook = { overlay = Overlay.Book(it) },
        openBuy = { overlay = Overlay.Buy },
        openChat = { overlay = Overlay.Chat(it) },
        openMarketplace = { overlay = Overlay.Marketplace },
    )

    AmbientBackdrop {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                // Tabs slide toward the side you tapped, settling softly; reduced motion simply fades.
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                if (reduced) fadeIn(Motion.soft()) togetherWith fadeOut(Motion.soft())
                else (slideInHorizontally(Motion.gentle()) { w -> dir * w / 6 } + fadeIn(Motion.gentle()) + scaleIn(Motion.gentle(), initialScale = 0.985f)) togetherWith
                    (slideOutHorizontally(Motion.gentle()) { w -> -dir * w / 8 } + fadeOut(Motion.soft()))
            },
            label = "tab",
        ) { t ->
            when (t) {
                Tab.TODAY -> TodayScreen(actions)
                Tab.MONEY -> MoneyScreen(openTxn = { overlay = Overlay.Txn(it) }, openAdd = { overlay = Overlay.QuickAdd })
                Tab.GOALS -> GoalsScreen(startNewGoal = newGoal, onNewGoalShown = { newGoal = false }, onWeekly = { overlay = Overlay.Weekly })
                Tab.GUIDE -> GuideScreen(guide)
            }
        }

        StatusBarFade()

        // Soft fade under the dock so content scrolls away under it gracefully.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(c.canvas.copy(alpha = 0f), c.canvas.copy(alpha = 0.9f)))),
        )

        FloatingDock(
            tab, { tab = it },
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = Space.gutter, vertical = Space.md),
        )

        // On Home the pet says hello once per start: a short line about the runway, tap to talk. A pat (press and
        // hold) makes it spin and say something light; each pat brings the next line.
        var greeted by rememberSaveable { mutableStateOf(false) }
        var bubble by remember { mutableStateOf<String?>(null) }
        var pats by rememberSaveable { mutableStateOf(0) }
        var bubbleKey by remember { mutableStateOf(0) }
        LaunchedEffect(tab) {
            if (tab == Tab.TODAY && !greeted) {
                kotlinx.coroutines.delay(700); bubble = petLine(state); greeted = true; bubbleKey++
            } else if (tab != Tab.TODAY) bubble = null
        }
        LaunchedEffect(bubbleKey) { if (bubble != null) { kotlinx.coroutines.delay(6_500); bubble = null } }

        val anyComplete = state.goals.any { it.isComplete }
        RuvoOrb(
            mood = OrbMood.from(state.runway, anyComplete),
            tilt = tilt,
            onTalk = { bubble = null; overlay = Overlay.Chat() },
            onQuickAdd = { bubble = null; overlay = Overlay.QuickAdd },
            onPat = { bubble = patLine(state, pats); pats++; bubbleKey++ },
            size = 70.dp,
            // Floats above the dock's right end, with room below it to be pulled down.
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = Space.gutter + 2.dp, bottom = 90.dp),
        )

        AnimatedVisibility(
            bubble != null && overlay == Overlay.None,
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = Space.gutter, bottom = 170.dp),
            enter = scaleIn(Motion.bouncy(), initialScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 1f)) + fadeIn(),
            exit = scaleOut(Motion.soft(), targetScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.9f, 1f)) + fadeOut(),
        ) {
            var last by remember { mutableStateOf("") }
            bubble?.let { last = it }
            PetBubble(last, onClick = { bubble = null; overlay = Overlay.Chat() })
        }

        AnimatedVisibility(
            overlay == Overlay.Sort,
            enter = slideInVertically(Motion.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(Motion.soft()) { it } + fadeOut(),
        ) {
            SortStackScreen(onClose = { overlay = Overlay.None }, onShare = { id -> overlay = Overlay.Txn(id, split = true) })
        }

        val book = overlay as? Overlay.Book
        AnimatedVisibility(
            book != null,
            enter = fadeIn(Motion.gentle()) + scaleIn(Motion.gentle(), initialScale = 0.92f),
            exit = fadeOut(Motion.soft()) + scaleOut(Motion.soft(), targetScale = 0.96f),
        ) {
            val id = book?.id ?: lastBook
            if (id != null) {
                lastBook = id
                BookReader(
                    id,
                    onClose = { overlay = Overlay.None },
                    onApply = { apply ->
                        when (apply) {
                            Library.Apply.BEFORE_YOU_BUY -> overlay = Overlay.Buy
                            Library.Apply.NEW_GOAL -> { overlay = Overlay.None; tab = Tab.GOALS; newGoal = true }
                            Library.Apply.GOALS -> { overlay = Overlay.None; tab = Tab.GOALS }
                            Library.Apply.SORT -> overlay = Overlay.Sort
                            Library.Apply.MONEY -> { overlay = Overlay.None; tab = Tab.MONEY }
                            Library.Apply.ASK_RUVO -> overlay = Overlay.Chat()
                        }
                    },
                    onAsk = { overlay = Overlay.Chat(it) },
                )
            }
        }
    }
    when (val o = overlay) {
        Overlay.None, Overlay.Sort, is Overlay.Book -> {}
        Overlay.Why -> WhySheet { overlay = Overlay.None }
        Overlay.Buy -> BeforeYouBuySheet(onDismiss = { overlay = Overlay.None }) { price ->
            scope.launch { RuvoGraph.repository.addManual(price, "Purchase", "Shopping", Bucket.FUEL) }
        }
        is Overlay.Chat -> ChatSheet(onDismiss = { overlay = Overlay.None }, firstQuestion = o.question)
        Overlay.QuickAdd -> QuickAddSheet { overlay = Overlay.None }
        Overlay.Settings -> SettingsSheet(state, onDismiss = { overlay = Overlay.None }, onPersonas = { overlay = Overlay.Personas })
        Overlay.Personas -> PersonaPicker(onDismiss = { overlay = Overlay.None }) { overlay = Overlay.None; tab = Tab.TODAY }
        is Overlay.Txn -> TxnSheet(o.id, o.split) { overlay = Overlay.None }
        Overlay.Profile -> ProfileSheet(
            state, onDismiss = { overlay = Overlay.None },
            onSettings = { overlay = Overlay.Settings },
            onMarketplace = { overlay = Overlay.Marketplace },
            onGuide = { tab = Tab.GUIDE },
            onPersonas = { overlay = Overlay.Personas },
            onWeekly = { overlay = Overlay.Weekly },
        )
        Overlay.Weekly -> ReviewSheet(state, onDismiss = { overlay = Overlay.None }, startWithWeek = true, onOpenGuide = { tab = Tab.GUIDE })
        Overlay.Monthly -> ReviewSheet(state, onDismiss = { overlay = Overlay.None }, onOpenGuide = { tab = Tab.GUIDE })
        Overlay.Marketplace -> com.ruvo.feature.guide.MarketplaceSheet { overlay = Overlay.None }
    }
}

/**
 * The floating dock: a dark glass pill of icon buttons, in both moods. The chosen tab opens into a white pill with
 * its name in navy, springing wider as the others make room, so the current place is clear without reading. Every
 * button keeps its name for TalkBack. It leaves room on the right for the pet, which floats just above it.
 */
@Composable
fun FloatingDock(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    // Deep navy glass by day, smoky glass at night, with a faint bright edge.
    val bar = if (c.isDark) androidx.compose.ui.graphics.Color(0xF0171A2E) else androidx.compose.ui.graphics.Color(0xF2141B4D)
    val idle = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
    Row(
        modifier.shadow(20.dp, shape, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(shape).background(bar)
            .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = if (c.isDark) 0.12f else 0.16f), shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { t ->
            val on = t == selected
            val icon = when (t) {
                Tab.TODAY -> RuvoIcons.Today
                Tab.MONEY -> RuvoIcons.Money
                Tab.GOALS -> RuvoIcons.Goals
                Tab.GUIDE -> RuvoIcons.Book
            }
            val weight by androidx.compose.animation.core.animateFloatAsState(if (on) 2.1f else 1f, Motion.bouncy(), label = "w${t.name}")
            val fill by androidx.compose.animation.animateColorAsState(if (on) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0f), Motion.soft(), label = "f${t.name}")
            Row(
                Modifier.weight(weight).height(50.dp).clip(shape).background(fill)
                    .semantics { this.selected = on }
                    .clickable(role = Role.Tab, onClickLabel = "Open ${t.label}") { onSelect(t) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, t.label, Modifier.size(21.dp), tint = if (on) c.onLight else idle)
                AnimatedVisibility(on, enter = fadeIn() + androidx.compose.animation.expandHorizontally(), exit = fadeOut() + androidx.compose.animation.shrinkHorizontally()) {
                    Text(t.label, Modifier.padding(start = 6.dp), style = Ruvo.type.label, color = c.onLight, maxLines = 1)
                }
            }
        }
    }
}

/**
 * What the pet says when patted: light and warm, never about spending less. Each pat brings the next line, and
 * the runway line stays honest (an estimate, said as one).
 */
private fun patLine(state: AppState, n: Int): String {
    val r = state.runway
    val lines = listOf(
        "Hehe, that tickles!",
        "Wheee! Okay, a little dizzy now.",
        if (r.unlimited) "Your runway has no end in sight. I checked twice." else "I counted again: about ${IndianFormat.days(r.runwayDays)} of runway.",
        "Psst, the Marketplace has a few student offers.",
        "Even ₹100 into a goal makes me wiggle.",
        "I don't judge spends. I just count days.",
        "Tap me any time you have a money question.",
    )
    return lines[Math.floorMod(n, lines.size)]
}

/** What the pet says on Home: the runway in a sentence, warm and never judging. Estimates are said as estimates. */
private fun petLine(state: AppState): String {
    val r = state.runway
    val name = state.displayName.takeIf { it != "there" }?.let { "Hey $it! " } ?: "Hey! "
    val buffer = r.bufferDays
    return name + when {
        r.unlimited -> "Your money looks set for now. Want to grow a goal?"
        buffer == null -> "About ${IndianFormat.days(r.runwayDays)} of runway. Tap me to plan."
        buffer >= 3 -> "About ${IndianFormat.days(r.runwayDays)} of runway, ${IndianFormat.days(buffer)} spare. Nice and steady."
        buffer >= 0 -> "About ${IndianFormat.days(r.runwayDays)}, just enough. Let's keep an eye on Fuel."
        else -> "About ${IndianFormat.days(r.runwayDays)}, a little short of next money. Let's plan together."
    }
}

/** A speech bubble from the pet, its tail pointing down at it. */
@Composable
private fun PetBubble(text: String, onClick: () -> Unit) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    Text(
        text,
        Modifier.widthIn(max = 240.dp).shadow(12.dp, shape, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(shape).background(if (c.isDark) c.raised else c.surface)
            .border(1.dp, if (c.isDark) c.glassEdge else androidx.compose.ui.graphics.Color.White, shape)
            .clickable(role = Role.Button, onClickLabel = "Talk to Ruvo", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        style = Ruvo.type.label, color = c.ink,
    )
}
