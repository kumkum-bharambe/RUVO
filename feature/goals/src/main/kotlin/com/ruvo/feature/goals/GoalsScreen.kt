package com.ruvo.feature.goals

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ruvo.design.FillBar
import com.ruvo.design.Radius
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.AppState
import com.ruvo.data.RuvoGraph
import com.ruvo.design.AccentButton
import com.ruvo.design.Amount
import com.ruvo.design.Chip
import com.ruvo.design.Eyebrow
import com.ruvo.design.GhostButton
import com.ruvo.design.Haptics
import com.ruvo.design.Motion
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoSheet
import com.ruvo.design.SectionTitle
import com.ruvo.design.Space
import com.ruvo.design.reveal
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import com.ruvo.design.parseRupees
import com.ruvo.engine.Goal
import com.ruvo.engine.IndianFormat
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Phone tilt from the accelerometer, -1 (left) to 1 (right). Zero when reduced motion is on. */
@Composable
fun rememberTilt(): State<Float> {
    val context = LocalContext.current
    val reduced = Ruvo.reducedMotion
    val tilt = remember { mutableFloatStateOf(0f) }
    DisposableEffect(reduced) {
        if (reduced) return@DisposableEffect onDispose { }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var smoothed = tilt.floatValue
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // Low-pass filter so the liquid settles instead of jittering.
                val raw = (-e.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                smoothed = smoothed * 0.85f + raw * 0.15f
                // Ignore sensor noise (about 1°) so a phone at rest lets the liquid settle completely.
                if (abs(smoothed - tilt.floatValue) > 0.02f) tilt.floatValue = smoothed
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return tilt
}

/**
 * Goals: liquid jars that tilt with the phone, insights on when each lands, perks and the monthly review.
 * [startNewGoal] opens the new-goal sheet straight away, e.g. from a Guide book's last page.
 */
@Composable
fun GoalsScreen(startNewGoal: Boolean = false, onNewGoalShown: () -> Unit = {}, onWeekly: () -> Unit = {}) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    var openGoal by remember { mutableStateOf<String?>(null) }
    var newGoal by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(startNewGoal) { if (startNewGoal) { newGoal = true; onNewGoalShown() } }
    var review by remember { mutableStateOf(false) }
    val tilt by rememberTilt()
    val c = Ruvo.colors

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.dockClearance)) {
        item {
            Column(Modifier.statusBarsPadding().padding(horizontal = Space.gutter, vertical = Space.md)) {
                Eyebrow("Goals")
                Text("Money with a future", Modifier.semantics { heading() }, style = Ruvo.type.headline, color = c.ink)
                Text(
                    "${IndianFormat.rupees(state.runway.futureProtected)} protected. Your runway never counts it.",
                    style = Ruvo.type.body, color = c.inkSoft,
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                // One big card per goal: the picture of what it's for leads, the numbers follow.
                state.goals.forEachIndexed { i, g ->
                    GoalCard(g, state.goalShapes[g.id] ?: 0, tilt, state.today, state.runway.fuel, Modifier.fillMaxWidth().reveal(i)) { openGoal = g.id }
                }
                NewGoalCard(Modifier.fillMaxWidth()) { newGoal = true }
            }
        }
        if (state.goals.isNotEmpty()) item { GoalInsights(state, Modifier.padding(horizontal = Space.gutter).padding(top = Space.md).reveal(3)) }
        item {
            RuvoCard(Modifier.fillMaxWidth().padding(horizontal = Space.gutter).padding(top = Space.xl), brush = c.heroBrush, border = null, elevated = true, onClick = { review = true }, onClickLabel = "Open monthly review") {
                Eyebrow("Monthly review", color = c.onHero.copy(alpha = 0.7f))
                Text("Your month in 30 seconds", style = Ruvo.type.title, color = c.onHero)
                Text("What came in, what went out, what you protected.", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.7f))
            }
        }
    }
    openGoal?.let { id -> GoalSheet(id, state) { openGoal = null } }
    if (newGoal) NewGoalSheet { newGoal = false }
    if (review) MonthlyReviewSheet(state, onDismiss = { review = false }, onWeekly = onWeekly)
}

/** Where a goal stands: on track if what is protected keeps pace with a straight line to the target date. */
internal fun onTrack(g: Goal, today: LocalDate): Boolean? {
    val end = g.targetDate ?: return null
    if (g.isComplete) return true
    val months = java.time.temporal.ChronoUnit.MONTHS.between(today.withDayOfMonth(1), end.withDayOfMonth(1)).coerceAtLeast(0)
    val reachable = g.protectedAmount + g.monthlyContribution * months
    return reachable >= g.target
}

/** Goal insights: when each goal lands at its current pace, and what would bring the date in reach. */
@Composable
private fun GoalInsights(state: AppState, modifier: Modifier) {
    val c = Ruvo.colors
    RuvoCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(RuvoIcons.Target, null, Modifier.size(18.dp), tint = c.futureInk)
            Spacer(Modifier.width(Space.xs))
            Eyebrow("Goal insights", color = c.futureInk)
        }
        state.goals.forEach { g ->
            Spacer(Modifier.height(Space.sm))
            val left = (g.target - g.protectedAmount).coerceAtLeast(0)
            val line = when {
                g.isComplete -> "Done. Every rupee is there."
                g.monthlyContribution > 0 -> {
                    val months = ((left + g.monthlyContribution - 1) / g.monthlyContribution).toInt()
                    val lands = state.today.plusMonths(months.toLong())
                    val ok = onTrack(g, state.today)
                    val base = "At ${IndianFormat.rupees(g.monthlyContribution)} a month it lands around ${lands.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${lands.year}."
                    if (ok == false && g.targetDate != null) {
                        val m = java.time.temporal.ChronoUnit.MONTHS.between(state.today.withDayOfMonth(1), g.targetDate!!.withDayOfMonth(1)).coerceAtLeast(1)
                        "$base To make ${IndianFormat.date(g.targetDate!!)}, try about ${IndianFormat.rupees(left / m)} a month."
                    } else base
                }
                else -> "No monthly amount yet. Even a small regular pour makes the date real."
            }
            Text(g.name, style = Ruvo.type.bodyStrong, color = c.ink)
            Text(line, style = Ruvo.type.caption, color = c.inkSoft)
        }
    }
}

@Composable
private fun GoalCard(g: Goal, shape: Int, tilt: Float, today: LocalDate, fuel: Long, modifier: Modifier, onClick: () -> Unit) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val pct = (g.progress * 100).toInt()
    val left = (g.target - g.protectedAmount).coerceAtLeast(0)
    RuvoCard(modifier, onClick = onClick, onClickLabel = "Open ${g.name}", padding = PaddingValues(Space.sm), elevated = !c.isDark) {
        Box {
            GoalIllustration(
                sceneFor(g.name, shape), tilt,
                Modifier.fillMaxWidth().height(170.dp).clearAndSetSemantics { contentDescription = "${g.name}, $pct percent saved" },
                complete = g.isComplete,
            )
            // A glass badge on the picture: how far along, at a glance.
            Row(
                Modifier.align(Alignment.TopEnd).padding(Space.xs).clip(RoundedCornerShape(Radius.chip))
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f))
                    .border(1.dp, androidx.compose.ui.graphics.Color.White, RoundedCornerShape(Radius.chip))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (g.isComplete) "Done" else "$pct%", style = Ruvo.type.bodyStrong, color = androidx.compose.ui.graphics.Color(0xFF141B4D))
            }
        }
        Column(Modifier.padding(horizontal = Space.xs, vertical = Space.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(g.name, style = Ruvo.type.title, color = c.ink, maxLines = 1)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Amount(g.protectedAmount, style = Ruvo.type.bodyStrong, countUp = true)
                        Text(" of ${IndianFormat.rupees(g.target)}", style = Ruvo.type.caption, color = c.inkSoft)
                    }
                }
                val ok = onTrack(g, today)
                if (ok != null) com.ruvo.design.Tag(
                    if (g.isComplete) "Complete" else if (ok) "On track" else "Catching up",
                    color = (if (g.isComplete) c.sun else if (ok) c.future else c.fuel).copy(alpha = 0.22f),
                    content = if (ok) c.futureInk else c.fuelInk,
                )
            }
            Spacer(Modifier.height(Space.sm))
            MilestoneBar(g.progress.toFloat(), if (g.isComplete) c.sun else c.future)
            Spacer(Modifier.height(Space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (g.isComplete) "Every rupee is there." else "${IndianFormat.rupees(left)} to go" + (g.targetDate?.let { " · by ${IndianFormat.date(it, today)}" } ?: ""),
                    Modifier.weight(1f), style = Ruvo.type.caption, color = c.inkSoft,
                )
                // One tap to move a little Fuel into the goal; only offered when there's Fuel to move.
                if (!g.isComplete && fuel >= 100_00) {
                    Text(
                        "+ ₹100",
                        Modifier.clip(RoundedCornerShape(Radius.chip)).background(c.future.copy(alpha = 0.18f))
                            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Add 100 rupees to ${g.name} from Fuel") {
                                Haptics.confirm(view)
                                scope.launch { RuvoGraph.repository.pour(g.id, 100_00) }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        style = Ruvo.type.label, color = c.futureInk,
                    )
                }
            }
        }
    }
}

/** Progress with marks at a quarter, a half and three quarters; each lights up once it's passed. */
@Composable
private fun MilestoneBar(progress: Float, accent: androidx.compose.ui.graphics.Color) {
    val c = Ruvo.colors
    Box(Modifier.fillMaxWidth().height(14.dp), contentAlignment = Alignment.CenterStart) {
        FillBar(progress, accent, height = 8.dp)
        Row(Modifier.fillMaxWidth()) {
            listOf(0.25f, 0.5f, 0.75f).forEach { m ->
                Spacer(Modifier.weight(if (m == 0.25f) 0.25f else 0.25f))
                Box(
                    Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (progress >= m) accent else c.surface)
                        .border(2.dp, if (progress >= m) androidx.compose.ui.graphics.Color.White else c.hairline, androidx.compose.foundation.shape.CircleShape),
                )
            }
            Spacer(Modifier.weight(0.25f))
        }
    }
}

@Composable
private fun NewGoalCard(modifier: Modifier, onClick: () -> Unit) {
    val c = Ruvo.colors
    RuvoCard(modifier, onClick = onClick, onClickLabel = "Add a goal") {
        Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.amberBrush), contentAlignment = Alignment.Center) {
            Icon(RuvoIcons.Plus, null, Modifier.size(24.dp), tint = c.onAccent)
        }
        Spacer(Modifier.height(Space.xs))
        Text("New goal", style = Ruvo.type.bodyStrong, color = c.ink)
        Text("A trip, a laptop, a cushion", style = Ruvo.type.caption, color = c.inkFaint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalSheet(id: String, state: AppState, onDismiss: () -> Unit) {
    val g = state.goals.firstOrNull { it.id == id } ?: return onDismiss()
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val tilt by rememberTilt()
    var custom by remember { mutableStateOf("") }
    var releasing by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val fuel = state.runway.fuel.coerceAtLeast(0)
    val daily = state.runway.dailySpend

    RuvoSheet(g.name, onDismiss, eyebrow = if (g.isComplete) "Goal complete" else "Goal") {
        GoalIllustration(sceneFor(g.name, state.goalShapes[g.id] ?: 0), tilt, Modifier.fillMaxWidth().height(180.dp), complete = g.isComplete)
        Spacer(Modifier.height(Space.sm))
        FillBar(g.progress.toFloat(), if (g.isComplete) c.sun else c.future, height = 8.dp)
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.Bottom) {
            Amount(g.protectedAmount, style = Ruvo.type.display)
            Text("  of ${IndianFormat.rupees(g.target)}", Modifier.padding(bottom = 6.dp), style = Ruvo.type.body, color = c.inkSoft)
        }
        Text("Protected. Your runway does not count it.", style = Ruvo.type.caption, color = c.inkFaint)
        Spacer(Modifier.height(Space.lg))
        if (!releasing) {
            Eyebrow("Pour from Fuel")
            Spacer(Modifier.height(Space.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                listOf(100, 500, 1_000).forEach { v ->
                    val paise = v * 100L
                    val days = if (daily > 0) (paise / daily).toInt() else 0
                    Chip(if (days > 0) "${IndianFormat.rupees(paise)} · ${days}d" else IndianFormat.rupees(paise), false, {
                        Haptics.confirm(view)
                        scope.launch { RuvoGraph.repository.pour(g.id, paise) }
                    }, accent = c.future)
                }
            }
            Spacer(Modifier.height(Space.sm))
            RuvoField(custom, { custom = it }, "Another amount", number = true, prefix = "₹ ")
            Spacer(Modifier.height(Space.sm))
            val amount = parseRupees(custom) ?: 0
            PrimaryButton(
                if (amount > 0) "Pour ${IndianFormat.rupees(amount)}" else "Pour", {
                    Haptics.confirm(view)
                    scope.launch { RuvoGraph.repository.pour(g.id, amount) }
                    custom = ""
                }, Modifier.fillMaxWidth(), enabled = amount in 1..fuel,
            )
            Text("You have ${IndianFormat.rupees(fuel)} of Fuel.", Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkFaint)
            Spacer(Modifier.height(Space.md))
            GhostButton("I need some of this money", { releasing = true }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Space.sm))
            // Removing a goal is always visible, with one confirmation that says where the money goes.
            if (!removing) {
                androidx.compose.material3.TextButton({ removing = true }, Modifier.fillMaxWidth()) {
                    Text("Remove this goal", style = Ruvo.type.label, color = c.warning)
                }
            } else {
                RuvoCard(Modifier.fillMaxWidth(), color = c.warning.copy(alpha = 0.1f), border = c.warning.copy(alpha = 0.5f)) {
                    Text("Remove ${g.name}?", style = Ruvo.type.title, color = c.ink)
                    Text(
                        if (g.protectedAmount > 0) "Its ${IndianFormat.rupees(g.protectedAmount)} goes back to Fuel, so your runway grows. You can make a new goal any time."
                        else "Nothing is protected in it yet. You can make a new goal any time.",
                        style = Ruvo.type.body, color = c.inkSoft,
                    )
                    Spacer(Modifier.height(Space.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        GhostButton("Keep it", { removing = false }, Modifier.weight(1f))
                        AccentButton("Remove", { scope.launch { RuvoGraph.repository.deleteGoal(g.id) }; onDismiss() }, Modifier.weight(1f), accent = c.warning)
                    }
                }
            }
        } else {
            val take = (parseRupees(custom) ?: 0).coerceAtMost(g.protectedAmount)
            Text("It is your money and your call. Moving it back to Fuel adds to your runway and moves the goal back.", style = Ruvo.type.body, color = c.ink)
            Spacer(Modifier.height(Space.sm))
            RuvoField(custom, { custom = it }, "How much?", number = true, prefix = "₹ ")
            if (take > 0 && daily > 0) {
                Text("Runway +${IndianFormat.days((take / daily).toInt())} · ${g.name} back to ${IndianFormat.rupees(g.protectedAmount - take)}", Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkSoft)
            }
            Spacer(Modifier.height(Space.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                GhostButton("Keep it protected", { releasing = false; custom = "" }, Modifier.weight(1f))
                PrimaryButton("Move to Fuel", { scope.launch { RuvoGraph.repository.release(g.id, take) }; releasing = false; custom = "" }, Modifier.weight(1f), enabled = take > 0)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewGoalSheet(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }
    var months by remember { mutableStateOf(6) }
    var picked by remember { mutableStateOf<GoalScene?>(null) }
    val scope = rememberCoroutineScope()
    val tilt by rememberTilt()
    val c = Ruvo.colors
    RuvoSheet("Do you want to save for a goal?", onDismiss, eyebrow = "New goal") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            listOf("Trip with friends", "Laptop", "Emergency cushion", "Course fees", "Phone").forEach { s -> Chip(s, name == s, { name = s }) }
        }
        Spacer(Modifier.height(Space.sm))
        RuvoField(name, { name = it }, "Goal name")
        Spacer(Modifier.height(Space.sm))
        RuvoField(target, { target = it }, "Target", number = true, prefix = "₹ ")
        Spacer(Modifier.height(Space.sm))
        RuvoField(monthly, { monthly = it }, "Each month (optional)", number = true, prefix = "₹ ")
        Spacer(Modifier.height(Space.md))
        Eyebrow("By when")
        Spacer(Modifier.height(Space.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            listOf(3, 6, 12).forEach { m -> Chip("$m months", months == m, { months = m }) }
        }
        Spacer(Modifier.height(Space.md))
        Eyebrow("Pick a picture")
        Text("Ruvo picks one from the name. Tap another if you like.", style = Ruvo.type.caption, color = c.inkFaint)
        Spacer(Modifier.height(Space.xs))
        val shown = picked ?: sceneFor(name, 0)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            items(GoalScene.entries.size) { i ->
                val scene = GoalScene.entries[i]
                val on = scene == shown
                Column(
                    Modifier.width(96.dp).clip(RoundedCornerShape(Radius.inner))
                        .border(2.dp, if (on) c.fuel else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(Radius.inner))
                        .clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClickLabel = scene.label) { picked = scene }
                        .padding(4.dp),
                ) {
                    GoalIllustration(scene, tilt, Modifier.fillMaxWidth().height(64.dp))
                    Text(scene.label, Modifier.padding(top = 4.dp, start = 4.dp), style = Ruvo.type.caption, color = if (on) c.ink else c.inkSoft)
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        val t = parseRupees(target) ?: 0
        PrimaryButton("Create goal", {
            scope.launch {
                RuvoGraph.repository.addGoal(name.ifBlank { "My goal" }, t, LocalDate.now().plusMonths(months.toLong()), parseRupees(monthly) ?: (t / months), picked?.let { PICKED_SCENE + it.ordinal } ?: 0)
            }
            onDismiss()
        }, Modifier.fillMaxWidth(), enabled = t > 0)
    }
}

/** Your month in 30 seconds: what came in, went out, was saved and is still free, then what to do next. */
@Composable
fun MonthlyReviewSheet(state: AppState, onDismiss: () -> Unit, onWeekly: () -> Unit = {}) {
    val c = Ruvo.colors
    val month = state.today.withDayOfMonth(1)
    val tx = state.activity.map { it.txn }.filter { !it.date.isBefore(month) && it.status != com.ruvo.engine.TxnStatus.IGNORED }
    val came = tx.filter { it.direction == com.ruvo.engine.Direction.CREDIT && it.kind == com.ruvo.engine.TxnKind.INCOME }.sumOf { it.amount }
    val went = tx.filter { it.countsAsSpending }.sumOf { it.yourShare }
    val top = tx.filter { it.countsAsSpending }.groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.yourShare } }.maxByOrNull { it.value }
    var giveJob by remember { mutableStateOf(false) }
    RuvoSheet("Your month in 30 seconds", onDismiss, eyebrow = "${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} so far") {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            RuvoCard(Modifier.weight(1f).reveal(0), brush = c.bucketBrush(com.ruvo.engine.Bucket.FUTURE), border = null) {
                Eyebrow("Received", color = androidx.compose.ui.graphics.Color(0xFF0B1A33).copy(alpha = 0.75f))
                Amount(came, style = Ruvo.type.title, color = androidx.compose.ui.graphics.Color(0xFF0B1A33), countUp = true)
            }
            RuvoCard(Modifier.weight(1f).reveal(1), brush = c.heroBrush, border = null) { Eyebrow("Spent", color = c.onHero.copy(alpha = 0.7f)); Amount(went, style = Ruvo.type.title, color = c.onHero) }
        }
        Spacer(Modifier.height(Space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            RuvoCard(Modifier.weight(1f).reveal(2), brush = c.bucketBrush(com.ruvo.engine.Bucket.FUTURE), border = null) { Eyebrow("Saved", color = c.onAccent.copy(alpha = 0.7f)); Amount(state.runway.futureProtected, style = Ruvo.type.title, color = c.onAccent) }
            RuvoCard(Modifier.weight(1f).reveal(3), brush = c.amberBrush, border = null) { Eyebrow("Still free", color = c.onAccent.copy(alpha = 0.7f)); Amount(state.runway.fuel.coerceAtLeast(0), style = Ruvo.type.title, color = c.onAccent) }
        }
        Spacer(Modifier.height(Space.lg))
        Text("What can you do next?", style = Ruvo.type.title, color = c.ink)
        val firstGoal = state.goals.firstOrNull { !it.isComplete }
        Spacer(Modifier.height(Space.xs))
        NextStep("Save better", if (firstGoal != null) "You could move a little earlier toward ${firstGoal.name}, the day money arrives." else "Give your next money a job with a small goal.", "Give it a job") { giveJob = true }
        top?.let { NextStep("Spending pattern", "${it.key} was your biggest category, at ${IndianFormat.rupees(it.value)}.", "See what changed") { onDismiss(); onWeekly() } }
        NextStep("Plan ahead", "Bills and fees are predictable. Give the next one a date and set money aside early.", "Got it", onDismiss)
        if (giveJob) {
            if (firstGoal != null) GoalSheet(firstGoal.id, state) { giveJob = false } else NewGoalSheet { giveJob = false }
        }
        Spacer(Modifier.height(Space.lg))
        PrimaryButton("Close", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun NextStep(label: String, text: String, action: String, onClick: () -> Unit) {
    val c = Ruvo.colors
    RuvoCard(Modifier.fillMaxWidth().padding(bottom = Space.xs)) {
        Eyebrow(label, color = c.fuelInk)
        Spacer(Modifier.height(Space.xxs))
        Text(text, style = Ruvo.type.body, color = c.ink)
        Spacer(Modifier.height(Space.sm))
        com.ruvo.design.Chip(action, true, onClick)
    }
}