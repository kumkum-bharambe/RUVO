package com.ruvo.feature.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Amount
import com.ruvo.design.Chip
import com.ruvo.design.Eyebrow
import com.ruvo.design.FillBar
import com.ruvo.design.GhostButton
import com.ruvo.design.Haptics
import com.ruvo.design.Motion
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.design.parseRupees
import com.ruvo.engine.BeforeYouBuy
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Insights
import com.ruvo.engine.SpendBasis
import com.ruvo.engine.SpendSource
import com.ruvo.engine.guide.Lessons
import kotlinx.coroutines.launch

/** "Why 23 days?": the whole calculation in plain words, plus the switch for where daily spend comes from. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhySheet(onDismiss: () -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val r = state.runway
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    RuvoSheet(if (r.unlimited) "Why no end?" else "Why ${IndianFormat.days(r.runwayDays)}?", onDismiss, eyebrow = "Your runway") {
        Line("You have", r.balance)
        Line("Promised before next money (Fixed)", -r.fixedReserved)
        Line("Protected for goals (Future)", -r.futureProtected)
        HorizontalDivider(Modifier.padding(vertical = Space.xs), color = c.hairline)
        Line("Free to spend (Fuel)", r.fuel, strong = true)
        Spacer(Modifier.height(Space.md))
        Text(
            if (r.unlimited) "Your recent spending is close to zero, so your Fuel has no end in sight."
            else "At about ${IndianFormat.rupees(r.dailySpend)} a day, Fuel lasts ${IndianFormat.days(r.runwayDays)}.",
            style = Ruvo.type.body, color = c.ink,
        )
        Text(runwaySubline(r), style = Ruvo.type.bodyStrong, color = if (r.makesIt) c.ink else c.warning)
        if (r.reservedCommitments.isNotEmpty()) {
            Spacer(Modifier.height(Space.md))
            Eyebrow("Set aside in Fixed")
            r.reservedCommitments.forEach { cm ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${cm.name} · ${cm.dueDate?.let { IndianFormat.date(it, r.today) } ?: ""}", Modifier.weight(1f), style = Ruvo.type.body, color = c.inkSoft)
                    Amount(cm.amount, style = Ruvo.type.label, color = c.inkSoft)
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        Eyebrow("Daily spend comes from")
        Spacer(Modifier.height(Space.xs))
        val used = when (r.spendSource) {
            SpendSource.HISTORY -> "your last two weeks of spending"
            SpendSource.PLAN -> "your monthly plan"
            SpendSource.EVEN_SPREAD -> "an even spread to your next money"
        }
        Text("Right now: $used.", style = Ruvo.type.caption, color = c.inkSoft)
        Spacer(Modifier.height(Space.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            listOf(SpendBasis.AUTO to "Let Ruvo choose", SpendBasis.HISTORY to "My history", SpendBasis.PLAN to "My plan").forEach { (b, label) ->
                Chip(label, state.settings.spendBasis == b, { scope.launch { RuvoGraph.repository.setSpendBasis(b) } })
            }
        }
        Spacer(Modifier.height(Space.lg))
        Eyebrow("What can change it?")
        Spacer(Modifier.height(Space.xs))
        listOf(
            "New income" to "Adds to Fuel the day it arrives",
            "A large purchase" to "Uses Fuel all at once",
            "A new commitment" to "Moves money from Fuel to Fixed",
            "Your spending pattern" to "A higher daily spend shortens it",
        ).forEach { (what, how) ->
            RuvoCard(Modifier.fillMaxWidth().padding(bottom = Space.xs), padding = androidx.compose.foundation.layout.PaddingValues(Space.md)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(what, Modifier.weight(1f), style = Ruvo.type.bodyStrong, color = c.ink)
                    Text(how, Modifier.weight(1.3f), style = Ruvo.type.caption, color = c.inkSoft)
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        PrimaryButton("Got it", onDismiss, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Line(label: String, amount: Long, strong: Boolean = false) {
    val c = Ruvo.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, Modifier.weight(1f), style = if (strong) Ruvo.type.bodyStrong else Ruvo.type.body, color = c.ink)
        Amount(amount, style = if (strong) Ruvo.type.title else Ruvo.type.amount)
    }
}

/**
 * Before you buy: type a price, see it in days. The result never says no; it shows the consequence and
 * leaves the choice with the student.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BeforeYouBuySheet(onDismiss: () -> Unit, onBought: (Long) -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val r = state.runway
    val c = Ruvo.colors
    val view = LocalView.current
    var text by remember { mutableStateOf("") }
    val price = parseRupees(text) ?: 0L
    val result = if (price > 0) BeforeYouBuy.evaluate(r, price) else null
    LaunchedEffect(result?.verdict) { if (result != null) Haptics.medium(view) }

    RuvoSheet("What does it cost?", onDismiss, eyebrow = "Before you buy") {
        RuvoField(text, { text = it }, "Price", number = true, prefix = "₹ ")
        Spacer(Modifier.height(Space.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            listOf(200, 500, 1_000, 2_500).forEach { v -> Chip(IndianFormat.rupees(v * 100L), text == v.toString(), { text = v.toString() }) }
        }
        Spacer(Modifier.height(Space.md))
        if (result != null) {
            val accent = when (result.verdict) {
                BeforeYouBuy.Verdict.COMFORTABLE -> c.fuel
                BeforeYouBuy.Verdict.TIGHT -> c.sun
                BeforeYouBuy.Verdict.SHORT -> c.warning
            }
            RuvoCard(color = accent.copy(alpha = if (c.isDark) 0.16f else 0.18f), border = null) {
                Eyebrow("Costs", color = c.ink)
                Text(
                    if (result.costDays == Math.floor(result.costDays)) IndianFormat.days(result.costDays.toInt()) else "${result.costDays} days",
                    style = Ruvo.type.display, color = c.ink,
                )
                Text("of your usual spending", style = Ruvo.type.caption, color = c.inkSoft)
                Spacer(Modifier.height(Space.md))
                val before = r.runwayDays.coerceAtLeast(1)
                val shrink by animateFloatAsState(result.runwayAfter.toFloat() / before, Motion.soft(), label = "shrink")
                FillBar(shrink, accent)
                Spacer(Modifier.height(Space.xs))
                Text("Runway ${r.runwayDays} → ${result.runwayAfter} days", style = Ruvo.type.bodyStrong, color = c.ink)
                Text(
                    when (result.verdict) {
                        BeforeYouBuy.Verdict.COMFORTABLE -> result.bufferAfter?.let { "You would still make it with ${IndianFormat.days(it)} spare." } ?: "Plenty of runway left."
                        BeforeYouBuy.Verdict.TIGHT -> result.bufferAfter?.let { "You would make it with ${IndianFormat.days(it)} spare. The rest of the month gets tight." } ?: "Your runway gets short."
                        BeforeYouBuy.Verdict.SHORT -> result.bufferAfter?.let { "You would be ${IndianFormat.days(-it)} short of your next money." } ?: "It uses more than your Fuel."
                    },
                    style = Ruvo.type.body, color = c.ink,
                )
            }
            Spacer(Modifier.height(Space.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                GhostButton("Skip it", onDismiss, Modifier.weight(1f))
                PrimaryButton("I bought it", { onBought(price); onDismiss() }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(Space.xs))
            Text("Ruvo never blocks a purchase. The choice is yours.", style = Ruvo.type.caption, color = c.inkFaint)
        } else {
            Text("You have ${IndianFormat.rupees(r.fuel.coerceAtLeast(0))} of Fuel. A day of your spending is about ${IndianFormat.rupees(r.dailySpend)}.", style = Ruvo.type.body, color = c.inkSoft)
        }
    }
}

@Composable
fun LessonSheet(id: String, onDismiss: () -> Unit, onAsk: () -> Unit) {
    val lesson = Lessons.byId(id) ?: return onDismiss()
    val c = Ruvo.colors
    RuvoSheet(lesson.title, onDismiss, eyebrow = "A small idea") {
        Text(lesson.body, style = Ruvo.type.body, color = c.ink)
        Spacer(Modifier.height(Space.sm))
        Text("Shows up when: ${lesson.trigger.lowercase()}", style = Ruvo.type.caption, color = c.inkFaint)
        Spacer(Modifier.height(Space.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            PrimaryButton("Ask Ruvo about this", onAsk, Modifier.fillMaxWidth())
            GhostButton("Close", onDismiss, Modifier.fillMaxWidth())
        }
    }
}
