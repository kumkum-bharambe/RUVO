package com.ruvo.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
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
import com.ruvo.design.Motion
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Space
import com.ruvo.engine.Bucket
import com.ruvo.feature.goals.GoalsScreen
import com.ruvo.feature.goals.rememberTilt
import com.ruvo.feature.money.MoneyScreen
import com.ruvo.feature.money.TxnSheet
import com.ruvo.feature.onboarding.OnboardingFlow
import com.ruvo.feature.onboarding.PersonaPicker
import com.ruvo.feature.orb.ChatSheet
import com.ruvo.feature.orb.OrbMood
import com.ruvo.feature.orb.RuvoOrb
import com.ruvo.feature.today.BeforeYouBuySheet
import com.ruvo.feature.today.LessonSheet
import com.ruvo.feature.today.SortStackScreen
import com.ruvo.feature.today.TodayActions
import com.ruvo.feature.today.TodayScreen
import com.ruvo.feature.today.WhySheet
import kotlinx.coroutines.launch

enum class Tab(val label: String) { TODAY("Today"), MONEY("Money"), GOALS("Goals") }

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
    data class Lesson(val id: String) : Overlay
    data class Txn(val id: String, val split: Boolean = false) : Overlay
}

@Composable
fun RuvoRoot(state: AppState, request: LaunchRequest?, consumed: () -> Unit) {
    if (!state.ready) {
        Box(Modifier.fillMaxSize().background(Ruvo.colors.canvas))
        return
    }
    if (!state.settings.onboarded) {
        OnboardingFlow(onDone = {})
        return
    }
    MainShell(state, request, consumed)
}

@Composable
private fun MainShell(state: AppState, request: LaunchRequest?, consumed: () -> Unit) {
    val c = Ruvo.colors
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    val scope = rememberCoroutineScope()
    val tilt by rememberTilt()

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
        openLesson = { overlay = Overlay.Lesson(it) },
    )

    Box(Modifier.fillMaxSize().background(c.canvas)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(Motion.soft()) togetherWith fadeOut(Motion.soft()) },
            label = "tab",
        ) { t ->
            when (t) {
                Tab.TODAY -> TodayScreen(actions)
                Tab.MONEY -> MoneyScreen(openTxn = { overlay = Overlay.Txn(it) })
                Tab.GOALS -> GoalsScreen()
            }
        }

        // Soft fade under the dock so content scrolls away under it gracefully.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(c.canvas.copy(alpha = 0f), c.canvas))),
        )

        FloatingDock(tab, { tab = it }, Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = Space.gutter, bottom = Space.md))

        val anyComplete = state.goals.any { it.isComplete }
        RuvoOrb(
            mood = OrbMood.from(state.runway, anyComplete),
            tilt = tilt,
            onTalk = { overlay = Overlay.Chat() },
            onQuickAdd = { overlay = Overlay.QuickAdd },
            // Floats above the dock's right end, with room below it to be pulled down.
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = Space.gutter, bottom = 92.dp),
        )

        AnimatedVisibility(
            overlay == Overlay.Sort,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            SortStackScreen(onClose = { overlay = Overlay.None }, onShare = { id -> overlay = Overlay.Txn(id, split = true) })
        }
    }

    when (val o = overlay) {
        Overlay.None, Overlay.Sort -> {}
        Overlay.Why -> WhySheet { overlay = Overlay.None }
        Overlay.Buy -> BeforeYouBuySheet(onDismiss = { overlay = Overlay.None }) { price ->
            scope.launch { RuvoGraph.repository.addManual(price, "Purchase", "Shopping", Bucket.FUEL) }
        }
        is Overlay.Chat -> ChatSheet(onDismiss = { overlay = Overlay.None }, firstQuestion = o.question)
        Overlay.QuickAdd -> QuickAddSheet { overlay = Overlay.None }
        Overlay.Settings -> SettingsSheet(state, onDismiss = { overlay = Overlay.None }, onPersonas = { overlay = Overlay.Personas })
        Overlay.Personas -> PersonaPicker(onDismiss = { overlay = Overlay.None }) { overlay = Overlay.None; tab = Tab.TODAY }
        is Overlay.Lesson -> LessonSheet(o.id, { overlay = Overlay.None }) { overlay = Overlay.Chat("Explain this in simple words") }
        is Overlay.Txn -> TxnSheet(o.id, o.split) { overlay = Overlay.None }
    }
}

/**
 * The floating dock: three destinations on a pill that floats above the content. The selected item sits on
 * an ink blob that slides between items on a spring.
 */
@Composable
fun FloatingDock(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val itemWidth = 76.dp
    val x by animateDpAsState(itemWidth * selected.ordinal, Motion.bouncy(), label = "dock")
    val shape = RoundedCornerShape(Radius.chip)
    Box(
        modifier.shadow(if (c.isDark) 0.dp else 18.dp, shape, ambientColor = c.ink.copy(alpha = 0.2f), spotColor = c.ink.copy(alpha = 0.25f))
            .clip(shape).background(c.raised).border(1.dp, c.hairline, shape).padding(6.dp),
    ) {
        Box(Modifier.offset(x = x).width(itemWidth).height(56.dp).clip(shape).background(c.ink))
        Row {
            Tab.entries.forEach { t ->
                val on = t == selected
                val icon = when (t) {
                    Tab.TODAY -> RuvoIcons.Today
                    Tab.MONEY -> RuvoIcons.Money
                    Tab.GOALS -> RuvoIcons.Goals
                }
                Column(
                    Modifier.width(itemWidth).height(56.dp).clip(shape)
                        .semantics { this.selected = on }
                        .clickable(role = Role.Tab, onClickLabel = "Open ${t.label}") { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, null, Modifier.size(22.dp), tint = if (on) c.canvas else c.inkSoft)
                    Spacer(Modifier.height(2.dp))
                    Text(t.label, style = Ruvo.type.caption, color = if (on) c.canvas else c.inkSoft)
                }
            }
        }
    }
}
