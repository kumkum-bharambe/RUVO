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
import com.ruvo.data.AppState
import com.ruvo.design.RuvoIcons
import com.ruvo.design.IconCircle
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
            SpendSource.ESTIMATE -> "your own estimate of a usual day"
            SpendSource.BLEND -> "your estimate, averaged with your last two weeks"
        }
        Text("Right now: $used.", style = Ruvo.type.caption, color = c.inkSoft)
        Spacer(Modifier.height(Space.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            listOf(SpendBasis.AUTO to "Let Ruvo choose", SpendBasis.HISTORY to "My history", SpendBasis.PLAN to "My plan").forEach { (b, label) ->
                Chip(label, state.settings.spendBasis == b, { com.ruvo.data.RuvoGraph.appScope.launch { RuvoGraph.repository.setSpendBasis(b) } })
            }
        }
        Spacer(Modifier.height(Space.lg))
        UsualDayEditor(state.settings.dailyEstimate)
        Spacer(Modifier.height(Space.md))
        ExpectedMoneyCard(state)
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
            // Before you buy is blue wherever it appears. The bar's colour carries the verdict: yellow when you
            // still make it, butter when it gets tight, a light rose when it leaves you short.
            val accent = when (result.verdict) {
                BeforeYouBuy.Verdict.COMFORTABLE -> c.fuel
                BeforeYouBuy.Verdict.TIGHT -> c.sun
                BeforeYouBuy.Verdict.SHORT -> androidx.compose.ui.graphics.Color(0xFFFF9DB2)
            }
            val on = c.onHero
            RuvoCard(brush = c.heroBrush, border = null, elevated = true) {
                Eyebrow("Costs", color = on.copy(alpha = 0.75f))
                Text(
                    if (result.costDays == Math.floor(result.costDays)) IndianFormat.days(result.costDays.toInt()) else "${result.costDays} days",
                    style = Ruvo.type.display, color = on,
                )
                Text("of your usual spending", style = Ruvo.type.caption, color = on.copy(alpha = 0.75f))
                Spacer(Modifier.height(Space.md))
                val before = r.runwayDays.coerceAtLeast(1)
                val shrink by animateFloatAsState(result.runwayAfter.toFloat() / before, Motion.soft(), label = "shrink")
                FillBar(shrink, accent, track = on.copy(alpha = 0.18f))
                Spacer(Modifier.height(Space.xs))
                Text("Runway ${r.runwayDays} → ${result.runwayAfter} days", style = Ruvo.type.bodyStrong, color = on)
                Text(
                    when (result.verdict) {
                        BeforeYouBuy.Verdict.COMFORTABLE -> result.bufferAfter?.let { "You would still make it with ${IndianFormat.days(it)} spare." } ?: "Plenty of runway left."
                        BeforeYouBuy.Verdict.TIGHT -> result.bufferAfter?.let { "You would make it with ${IndianFormat.days(it)} spare. The rest of the month gets tight." } ?: "Your runway gets short."
                        BeforeYouBuy.Verdict.SHORT -> result.bufferAfter?.let { "You would be ${IndianFormat.days(-it)} short of your next money." } ?: "It uses more than your Fuel."
                    },
                    style = Ruvo.type.body, color = on.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.height(Space.md))
            // One clear way out: the answer is the point, and nothing is recorded unless they add it themselves.
            PrimaryButton("Got it", onDismiss, Modifier.fillMaxWidth())
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

/**
 * "Your usual day": the student's own estimate. Used on its own until there is a week of spending, then averaged with
 * the last two weeks, so one heavy week cannot make the runway look scary. Empty and Save clears it.
 */
@Composable
fun UsualDayEditor(current: Long) {
    val c = Ruvo.colors
    var text by remember(current) { mutableStateOf(if (current > 0) (current / 100).toString() else "") }
    Eyebrow("Your usual day")
    Spacer(Modifier.height(Space.xs))
    Text(
        if (current > 0) "You said about ${IndianFormat.rupees(current)} a day. One heavy week only moves the runway half as much."
        else "Tell Ruvo what a usual day costs, so one heavy week cannot swing your runway.",
        style = Ruvo.type.caption, color = c.inkSoft,
    )
    Spacer(Modifier.height(Space.xs))
    RuvoField(text, { text = it }, "A usual day", number = true, prefix = "₹ ")
    Spacer(Modifier.height(Space.xs))
    val typed = parseRupees(text) ?: 0
    PrimaryButton(
        when {
            typed > 0 -> "Save · ${IndianFormat.rupees(typed)} a day"
            current > 0 -> "Clear my estimate"
            else -> "Type an amount to save"
        },
        {
            RuvoGraph.appScope.launch {
                RuvoGraph.repository.setDailyEstimate(typed)
                com.ruvo.data.Notices.post(
                    com.ruvo.data.Notices.Notice.Text(if (typed > 0) "Usual day saved: ${IndianFormat.rupees(typed)}" else "Estimate cleared · runway uses your spending"),
                )
            }
        },
        Modifier.fillMaxWidth(),
        enabled = typed > 0 || current > 0,
    )
}

/**
 * Expected money: amounts with a rough date, Sure or Maybe. The earliest Sure one becomes the next money; Maybe ones are
 * shown but never counted on. Shown under Your usual day in Why N days and in Settings → Runway. The add form opens inside the card,
 * so it never stacks a sheet on top of Settings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpectedMoneyCard(state: AppState, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val list = state.settings.expectedList()
    val next = com.ruvo.engine.RunwayEngine.earliestSure(list, state.today)
    var adding by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var sure by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(state.today.plusDays(7)) }
    var choosing by remember { mutableStateOf(false) }
    val paise = parseRupees(amount) ?: 0
    RuvoCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Expected money")
                Text(
                    if (list.isEmpty()) "Money on the way without a fixed date? Shift pay, a transfer from home."
                    else next?.let { "Next money counted from ${IndianFormat.date(it, state.today)}" } ?: "Nothing Sure yet, so your next money date is unchanged.",
                    style = Ruvo.type.caption, color = c.inkSoft,
                )
            }
            Spacer(Modifier.width(Space.xs))
            IconCircle(if (adding) RuvoIcons.Close else RuvoIcons.Plus, if (adding) "Close" else "Add expected money", { adding = !adding })
        }
        if (adding) {
            Spacer(Modifier.height(Space.sm))
            Text("A rough date is fine. Sure counts as your next money; Maybe is shown but never counted on.", style = Ruvo.type.caption, color = c.inkSoft)
            Spacer(Modifier.height(Space.sm))
            RuvoField(amount, { amount = it }, "How much?", number = true, prefix = "₹ ")
            Spacer(Modifier.height(Space.xs))
            RuvoField(note, { note = it.take(30) }, "From? e.g. Café shifts, Mum")
            Spacer(Modifier.height(Space.sm))
            Eyebrow("Around when?")
            Spacer(Modifier.height(Space.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                val presets = listOf(3L to "In 3 days", 7L to "In a week", 14L to "In 2 weeks", 30L to "In a month")
                presets.forEach { (d, label) -> Chip(label, date == state.today.plusDays(d), { date = state.today.plusDays(d) }) }
                val custom = presets.none { date == state.today.plusDays(it.first) }
                Chip(if (custom) "On ${IndianFormat.date(date)}" else "Choose a date", custom, { choosing = true })
            }
            Spacer(Modifier.height(Space.sm))
            Eyebrow("How sure?")
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Chip("Sure", sure, { sure = true })
                Chip("Maybe", !sure, { sure = false })
            }
            Spacer(Modifier.height(Space.md))
            PrimaryButton(
                if (paise > 0) "Save · ${IndianFormat.rupees(paise)} ${if (sure) "sure" else "maybe"}" else "Type the amount to save",
                {
                    val a = paise; val d = date; val s = sure; val n = note.trim()
                    RuvoGraph.appScope.launch { RuvoGraph.repository.addExpected(a, d, s, n) }
                    com.ruvo.data.Notices.post(
                        com.ruvo.data.Notices.Notice.Text(
                            if (s) "Saved · ${IndianFormat.rupees(a)} expected around ${IndianFormat.date(d)}" else "Saved as Maybe · not counted until it is Sure",
                        ),
                    )
                    adding = false; amount = ""; note = ""; sure = false; date = state.today.plusDays(7)
                },
                Modifier.fillMaxWidth(), enabled = paise > 0,
            )
        }
        list.forEach { e ->
            val past = e.date.isBefore(state.today)
            Spacer(Modifier.height(Space.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.note.ifBlank { "Money in" }, style = Ruvo.type.bodyStrong, color = if (e.sure) c.ink else c.inkSoft)
                    Text(
                        "${if (past) "Was due" else "Around"} ${IndianFormat.date(e.date, state.today)} · ${if (e.sure) "Sure" else "Maybe, not counted"}",
                        style = Ruvo.type.caption, color = c.inkSoft,
                    )
                }
                Amount(e.amount, style = Ruvo.type.label, color = if (e.sure) c.ink else c.inkSoft)
            }
            Spacer(Modifier.height(Space.xxs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                GhostButton("Remove", {
                    RuvoGraph.appScope.launch { RuvoGraph.repository.removeExpected(e.id) }
                }, Modifier.weight(1f))
                PrimaryButton("It arrived", {
                    RuvoGraph.appScope.launch { RuvoGraph.repository.expectedArrived(e) }
                    com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Saved(e.amount, true, state.runway.runwayDays))
                }, Modifier.weight(1f))
            }
        }
    }
    if (choosing) com.ruvo.design.FutureDatePicker(date, { date = it }, { choosing = false })
}
