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
import com.ruvo.feature.goals.MonthlyReviewSheet
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
import com.ruvo.feature.today.WeeklyReviewSheet
import com.ruvo.feature.today.WhySheet
import kotlinx.coroutines.launch

enum class Tab(val label: String) { TODAY("Today"), MONEY("Money"), GOALS("Goals"), GUIDE("Guide") }

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
}

@Composable
fun RuvoRoot(state: AppState, request: LaunchRequest?, consumed: () -> Unit) {
    // A calm splash on every cold start: at least a beat long, and until the data is ready.
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1100); splashDone = true }
    val showApp = state.ready && splashDone
    Box(Modifier.fillMaxSize()) {
        if (showApp) {
            if (!state.settings.onboarded) OnboardingFlow(onDone = {}) else MainShell(state, request, consumed)
        }
        // The splash sits on top and fades away over the app.
        AnimatedVisibility(!showApp, enter = fadeIn(), exit = fadeOut(Motion.gentle())) {
            com.ruvo.design.RuvoSplash()
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
    var toMarket by remember { mutableStateOf(false) }

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
        openGuide = { tab = Tab.GUIDE },
        openAdd = { overlay = Overlay.QuickAdd },
        openProfile = { overlay = Overlay.Profile },
    )

    val guide = GuideActions(
        openBook = { overlay = Overlay.Book(it) },
        openBuy = { overlay = Overlay.Buy },
        openChat = { overlay = Overlay.Chat(it) },
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
                Tab.GUIDE -> GuideScreen(guide, showMarketplace = toMarket, onMarketplaceShown = { toMarket = false })
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

        val anyComplete = state.goals.any { it.isComplete }
        RuvoOrb(
            mood = OrbMood.from(state.runway, anyComplete),
            tilt = tilt,
            onTalk = { overlay = Overlay.Chat() },
            onQuickAdd = { overlay = Overlay.QuickAdd },
            size = 64.dp,
            // Floats above the dock's right end, with room below it to be pulled down.
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = Space.gutter + 4.dp, bottom = 100.dp),
        )

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
            onMarketplace = { tab = Tab.GUIDE; toMarket = true },
            onPersonas = { overlay = Overlay.Personas },
            onWeekly = { overlay = Overlay.Weekly },
        )
        Overlay.Weekly -> WeeklyReviewSheet(onDismiss = { overlay = Overlay.None }, onOpenGuide = { tab = Tab.GUIDE })
        Overlay.Monthly -> MonthlyReviewSheet(state, onDismiss = { overlay = Overlay.None }, onWeekly = { overlay = Overlay.Weekly })
    }
}

/**
 * The floating dock: four destinations on a glass pill that floats above the content. The selected item sits
 * on an amber pill that slides between items on a spring and glows softly; the icon lifts as it is chosen.
 * It leaves room on the right for the Orb, which floats just above it.
 */
@Composable
fun FloatingDock(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    BoxWithConstraints(
        modifier.shadow(20.dp, shape, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(shape).background(if (c.isDark) c.surface else c.surface.copy(alpha = 0.94f))
            .border(1.dp, c.glassEdge, shape).padding(6.dp),
    ) {
        val itemWidth = maxWidth / Tab.entries.size
        val x by animateDpAsState(itemWidth * selected.ordinal, Motion.snappy(), label = "dock")
        Box(
            Modifier.offset(x = x).width(itemWidth).height(58.dp)
                .shadow(10.dp, shape, ambientColor = c.fuelDeep, spotColor = c.fuelDeep)
                .clip(shape).background(c.amberBrush),
        )
        Row {
            Tab.entries.forEach { t ->
                val on = t == selected
                val icon = when (t) {
                    Tab.TODAY -> RuvoIcons.Today
                    Tab.MONEY -> RuvoIcons.Money
                    Tab.GOALS -> RuvoIcons.Goals
                    Tab.GUIDE -> RuvoIcons.Book
                }
                val lift by animateDpAsState(if (on) (-1).dp else 0.dp, Motion.bouncy(), label = "lift${t.name}")
                Column(
                    Modifier.width(itemWidth).height(58.dp).clip(shape)
                        .semantics { this.selected = on }
                        .clickable(role = Role.Tab, onClickLabel = "Open ${t.label}") { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, null, Modifier.size(22.dp).offset(y = lift), tint = if (on) c.onAccent else c.inkSoft)
                    Spacer(Modifier.height(2.dp))
                    Text(t.label, style = Ruvo.type.caption, color = if (on) c.onAccent else c.inkSoft, maxLines = 1)
                }
            }
        }
    }
}
