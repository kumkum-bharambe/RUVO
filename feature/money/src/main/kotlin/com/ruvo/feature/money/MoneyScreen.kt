package com.ruvo.feature.money

import android.content.Intent
import androidx.compose.foundation.background
import com.ruvo.design.Motion
import com.ruvo.design.tourTarget
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import com.ruvo.design.Radius
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.AppState
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Amount
import com.ruvo.design.IconCircle
import com.ruvo.design.LegendDot
import com.ruvo.design.Pill
import com.ruvo.design.PillBars
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Segment
import com.ruvo.design.SegmentedControl
import com.ruvo.design.SplitBar
import com.ruvo.design.reveal
import com.ruvo.design.Chip
import com.ruvo.design.Eyebrow
import com.ruvo.design.FillBar
import com.ruvo.design.GhostButton
import com.ruvo.design.ListRow
import com.ruvo.design.MerchantLogo
import com.ruvo.design.PersonAvatar
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoSheet
import com.ruvo.design.SectionTitle
import com.ruvo.design.Space
import com.ruvo.design.bucketMeaning
import com.ruvo.design.bucketName
import com.ruvo.design.parseRupees
import com.ruvo.engine.Bucket
import com.ruvo.engine.Direction
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Insights
import com.ruvo.engine.TxnStatus
import com.ruvo.engine.paidThisMonth
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

private enum class MoneyTab(val label: String) { OVERVIEW("Overview"), ACTIVITY("Transactions"), HISTORY("History"), OWED("Owed") }

/** How Activity is grouped: by week, by month, or everything. [offset] steps back through earlier periods. */
private enum class Span(val label: String) { WEEK("Weekly"), MONTH("Monthly"), ALL("All") }

/** The first and last day of the period [back] steps before the current one. Weeks run Monday to Sunday. */
private fun spanRange(span: Span, back: Int, today: java.time.LocalDate): Pair<java.time.LocalDate, java.time.LocalDate>? = when (span) {
    Span.WEEK -> {
        val start = today.with(java.time.DayOfWeek.MONDAY).minusWeeks(back.toLong())
        start to start.plusDays(6)
    }
    Span.MONTH -> {
        val ym = java.time.YearMonth.from(today).minusMonths(back.toLong())
        ym.atDay(1) to ym.atEndOfMonth()
    }
    Span.ALL -> null
}

private fun spanLabel(span: Span, back: Int, range: Pair<java.time.LocalDate, java.time.LocalDate>?, today: java.time.LocalDate): String = when {
    range == null -> "Everything"
    span == Span.WEEK && back == 0 -> "This week"
    span == Span.WEEK && back == 1 -> "Last week"
    span == Span.WEEK -> "${IndianFormat.date(range.first)} – ${IndianFormat.date(range.second)}"
    back == 0 -> "This month"
    else -> range.first.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + range.first.year
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyScreen(openTxn: (String) -> Unit, openAdd: () -> Unit = {}) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MoneyTab.OVERVIEW) }
    var span by rememberSaveable { mutableStateOf(Span.WEEK) }
    var back by rememberSaveable { mutableStateOf(0) }
    var editMonth by remember { mutableStateOf<java.time.YearMonth?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var balanceSheet by remember { mutableStateOf(false) }
    val c = Ruvo.colors

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.dockClearance)) {
        item {
            Row(Modifier.statusBarsPadding().padding(horizontal = Space.gutter, vertical = Space.md), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Money")
                    Text("Where it is, where it went", Modifier.semantics { heading() }, style = Ruvo.type.headline, color = c.ink)
                }
                IconCircle(RuvoIcons.Plus, "Add a spend", openAdd, tint = c.onBrand, background = c.brand)
            }
        }
        item { BalanceCard(state, Modifier.padding(horizontal = Space.gutter).tourTarget("balance").reveal(0)) { balanceSheet = true } }
        item {
            SegmentedControl(
                MoneyTab.entries.map { it.label },
                tab.ordinal, { tab = MoneyTab.entries[it] },
                Modifier.padding(horizontal = Space.gutter, vertical = Space.md).tourTarget("moneyTabs").reveal(1),
            )
        }
        when (tab) {
            MoneyTab.OVERVIEW -> {
                item { WhereItWent(state, Modifier.reveal(2)) { cat -> category = cat; span = Span.MONTH; back = 0; tab = MoneyTab.ACTIVITY } }
                item { Buckets(state) }
            }
            MoneyTab.ACTIVITY -> {
                val range = spanRange(span, back, state.today)
                item {
                    Column(Modifier.padding(horizontal = Space.gutter)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                            // The period menu: the chosen one is an ink pill, navy by day and white at night.
                            Span.entries.forEach { s -> Chip(s.label, span == s, { span = s; back = 0 }) }
                        }
                        category?.let { cat ->
                            Spacer(Modifier.height(Space.xs))
                            Chip("$cat  ×", true, { category = null })
                        }
                        Spacer(Modifier.height(Space.sm))
                        FlowCard(state, span, back, range, onBack = { back++ }, onForward = { if (back > 0) back-- }, onUpdateBalance = { balanceSheet = true }, onAdd = openAdd)
                    }
                }
                val inPeriod = state.activity.filter { entry ->
                    (range == null || (!entry.txn.date.isBefore(range.first) && !entry.txn.date.isAfter(range.second))) &&
                        (category == null || entry.txn.category == category)
                }
                val groups = inPeriod.groupBy { it.txn.date }.toSortedMap(compareByDescending { it })
                if (groups.isEmpty()) item { Empty(if (state.activity.isEmpty()) "Nothing yet. Tap + Add to record a payment, or turn on SMS tracking in Settings." else "Nothing recorded in ${spanLabel(span, back, range, state.today).lowercase()}.") }
                groups.forEach { (date, items) ->
                    item(key = "d$date") {
                        val out = items.filter { it.txn.direction == Direction.DEBIT && it.txn.status != TxnStatus.IGNORED }.sumOf { it.txn.yourShare }
                        Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter).padding(top = Space.md, bottom = Space.xxs), verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow(IndianFormat.relativeDay(date, state.today), Modifier.weight(1f))
                            if (out > 0) Text("-${IndianFormat.rupees(out)}", style = Ruvo.type.label, color = c.inkFaint)
                        }
                    }
                    items.forEach { entry -> item(key = entry.txn.id) { TxnRow(entry, state, openTxn) } }
                }
            }
            MoneyTab.HISTORY -> item {
                History(state, onEdit = { editMonth = it }, onView = { ym ->
                    // Jump to Activity, monthly, stepped back to that month.
                    span = Span.MONTH
                    back = java.time.temporal.ChronoUnit.MONTHS.between(ym, java.time.YearMonth.from(state.today)).toInt().coerceAtLeast(0)
                    category = null
                    tab = MoneyTab.ACTIVITY
                })
            }
            MoneyTab.OWED -> item { Owed(state) }
        }
    }
    if (balanceSheet) BalanceSheet(state) { balanceSheet = false }
    editMonth?.let { m -> MonthEditSheet(state, m) { editMonth = null } }
}

/**
 * GPay-style summary of the period on screen: money in, money out and what is left, with arrows to step through
 * earlier weeks or months, and the two things people do most: update the balance, add a payment.
 */
@Composable
private fun FlowCard(
    state: AppState, span: Span, back: Int, range: Pair<java.time.LocalDate, java.time.LocalDate>?,
    onBack: () -> Unit, onForward: () -> Unit, onUpdateBalance: () -> Unit, onAdd: () -> Unit,
) {
    val c = Ruvo.colors
    val f = Insights.flow(state.input.transactions, range?.first ?: java.time.LocalDate.MIN, range?.second ?: java.time.LocalDate.MAX)
    RuvoCard(Modifier.fillMaxWidth(), padding = PaddingValues(Space.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (range != null) IconCircle(RuvoIcons.Back, "Earlier", onBack)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(spanLabel(span, back, range, state.today), style = Ruvo.type.title, color = c.ink)
                if (range != null) Text("${IndianFormat.date(range.first)} – ${IndianFormat.date(range.second)}", style = Ruvo.type.caption, color = c.inkFaint)
            }
            if (range != null) IconCircle(RuvoIcons.Forward, "Later", onForward, tint = if (back > 0) c.ink else c.inkFaint.copy(alpha = 0.4f))
        }
        Spacer(Modifier.height(Space.md))
        // Credited in brand blue (money in is always blue), Debited beside it, and what is left underneath.
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            RuvoCard(Modifier.weight(1f), color = c.brandTint, border = null, padding = PaddingValues(Space.md)) {
                Eyebrow("Credited", color = c.brandInk)
                Amount(f.moneyIn, style = Ruvo.type.title, color = c.ink, signed = f.moneyIn > 0, countUp = true)
                Text("money in", style = Ruvo.type.caption, color = c.inkSoft)
            }
            RuvoCard(Modifier.weight(1f), padding = PaddingValues(Space.md)) {
                Eyebrow("Debited")
                Amount(-f.moneyOut, style = Ruvo.type.title, countUp = true)
                Text("money out", style = Ruvo.type.caption, color = c.inkFaint)
            }
        }
        Spacer(Modifier.height(Space.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (f.net >= 0) "Left" else "Over by", Modifier.weight(1f), style = Ruvo.type.bodyStrong, color = c.ink)
            Amount(kotlin.math.abs(f.net), style = Ruvo.type.title, color = if (f.net < 0) c.warning else c.ink)
            Spacer(Modifier.width(Space.xs))
            Text("· ${f.count} entries", style = Ruvo.type.caption, color = c.inkFaint)
        }
        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            GhostButton("Balance", onUpdateBalance, Modifier.weight(1f))
            PrimaryButton("Add", onAdd, Modifier.weight(1f), icon = RuvoIcons.Plus)
        }
    }
}

/** Correct a month in History: what really came in and went out. Display only; the runway never changes. */
@Composable
private fun MonthEditSheet(state: AppState, month: java.time.YearMonth, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val recorded = remember(state.input.transactions, month) {
        Insights.months(state.input.transactions, state.today, 12).firstOrNull { it.month == month }
    }
    val edit = state.settings.monthEditMap()[month]
    var received by remember { mutableStateOf(((edit?.first ?: recorded?.received ?: 0) / 100).toString()) }
    var spent by remember { mutableStateOf(((edit?.second ?: recorded?.spent ?: 0) / 100).toString()) }
    val name = month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + month.year
    RuvoSheet("Correct $name", onDismiss, eyebrow = "History") {
        Text("Match this to your bank or UPI app. It changes History only; your runway always uses your real balance.", style = Ruvo.type.body, color = Ruvo.colors.inkSoft)
        Spacer(Modifier.height(Space.md))
        RuvoField(received, { received = it }, "Money in that month", number = true, prefix = "₹ ")
        Spacer(Modifier.height(Space.sm))
        RuvoField(spent, { spent = it }, "Money out that month", number = true, prefix = "₹ ")
        recorded?.let {
            Text("Ruvo recorded ${IndianFormat.rupees(it.received)} in and ${IndianFormat.rupees(it.spent)} out.", Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = Ruvo.colors.inkFaint)
        }
        Spacer(Modifier.height(Space.lg))
        PrimaryButton("Save", {
            com.ruvo.data.RuvoGraph.appScope.launch { RuvoGraph.repository.setMonthTotals(month, parseRupees(received) ?: 0, parseRupees(spent) ?: 0) }
            onDismiss()
        }, Modifier.fillMaxWidth())
        if (edit != null) {
            Spacer(Modifier.height(Space.sm))
            GhostButton("Go back to what Ruvo recorded", { RuvoGraph.appScope.launch { RuvoGraph.repository.clearMonthTotals(month) }; onDismiss() }, Modifier.fillMaxWidth())
        }
    }
}

/**
 * The balance on the same pane of white glass as the runway on Home, with how it splits into goals, planned and
 * yours to spend. Hidden when Settings says so.
 */
@Composable
private fun BalanceCard(state: AppState, modifier: Modifier, onUpdate: () -> Unit) {
    val c = Ruvo.colors
    val r = state.runway
    val hidden = state.settings.hideBalance
    com.ruvo.design.GlassHero(modifier, onClick = onUpdate, onClickLabel = "Update balance") {
        Column(Modifier.fillMaxWidth().padding(Space.lg)) {
            Eyebrow("You have", color = c.inkSoft)
            if (hidden) Text("₹ ••••", style = Ruvo.type.display, color = c.ink)
            else Amount(r.balance, style = Ruvo.type.display, color = c.ink, countUp = true)
            Spacer(Modifier.height(Space.sm))
            val total = (r.fixedReserved + r.futureProtected + r.fuel.coerceAtLeast(0)).toFloat().coerceAtLeast(1f)
            SplitBar(listOf(Segment(r.futureProtected / total, c.future), Segment(r.fixedReserved / total, c.fixed), Segment(r.fuel.coerceAtLeast(0) / total, c.fuel)))
            Spacer(Modifier.height(Space.sm))
            Row {
                listOf(Triple(c.future, "In goals", r.futureProtected), Triple(c.fixed, "Planned", r.fixedReserved), Triple(c.fuel, "Yours to use", r.fuel.coerceAtLeast(0))).forEach { (col, label, v) ->
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Box(Modifier.padding(end = 6.dp).size(8.dp).clip(CircleShape).background(col))
                            Text(label, style = Ruvo.type.caption, color = c.inkSoft, maxLines = 1)
                        }
                        if (hidden) Text("••••", style = Ruvo.type.bodyStrong, color = c.ink) else Amount(v, style = Ruvo.type.bodyStrong, color = c.ink)
                    }
                }
            }
            Spacer(Modifier.height(Space.xs))
            Text("Across accounts and cash · tap to update", style = Ruvo.type.caption, color = c.inkFaint)
        }
    }
}

/** Spending by category over the last 30 days. */
@Composable
private fun WhereItWent(state: AppState, modifier: Modifier, onCategory: (String) -> Unit = {}) {
    val c = Ruvo.colors
    val cats = remember(state.input.transactions, state.today) { Insights.byCategory(state.input.transactions, state.today, 30) }
    if (cats.isEmpty()) return
    val top = cats.first().second.toFloat().coerceAtLeast(1f)
    // One calm ramp of brand blue, biggest first: the names say which is which, so the bars need no rainbow.
    val palette = listOf(1f, 0.78f, 0.6f, 0.46f, 0.36f, 0.28f).map { c.brand.copy(alpha = it) }
    RuvoCard(Modifier.padding(horizontal = Space.gutter).fillMaxWidth().then(modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Where it went")
                Amount(cats.sumOf { it.second }, style = Ruvo.type.title)
            }
            Text("last 30 days", style = Ruvo.type.caption, color = c.inkFaint)
        }
        cats.take(6).forEachIndexed { i, (cat, v) ->
            Spacer(Modifier.height(Space.sm))
            // A plain summary to read, not a list to tap.
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(cat, Modifier.width(104.dp), style = Ruvo.type.label, color = c.ink, maxLines = 1)
                FillBar(v / top, palette[i % palette.size], Modifier.weight(1f))
                Spacer(Modifier.width(Space.sm))
                Amount(v, style = Ruvo.type.label)
            }
        }
    }
}

/** Received and spent for the last three months, as paired pills, plus each month's numbers. */
@Composable
private fun History(state: AppState, onEdit: (java.time.YearMonth) -> Unit, onView: (java.time.YearMonth) -> Unit = {}) {
    val c = Ruvo.colors
    val edits = state.settings.monthEditMap()
    val months = remember(state.input.transactions, state.today, edits) { Insights.months(state.input.transactions, state.today, 6, edits) }
    Column(Modifier.padding(horizontal = Space.gutter)) {
        // Ruvo 7: tap a month; the others fade, and its numbers appear below.
        var picked by rememberSaveable { mutableStateOf<String?>(null) }
        val sel = months.firstOrNull { it.month.toString() == picked } ?: months.last()
        RuvoCard(Modifier.fillMaxWidth().reveal(2)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Received and spent", Modifier.weight(1f), style = Ruvo.type.title, color = c.ink)
                Text("Tap a month", style = Ruvo.type.caption, color = c.inkFaint)
            }
            Spacer(Modifier.height(Space.md))
            val max = months.maxOfOrNull { maxOf(it.received, it.spent) }?.toFloat()?.coerceAtLeast(1f) ?: 1f
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                months.forEach { m ->
                    val on = m.month == sel.month
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(Radius.inner))
                            .clickable(role = androidx.compose.ui.semantics.Role.Tab, onClickLabel = "Show ${m.month.month.name.lowercase()}") { picked = m.month.toString() }
                            .semantics(mergeDescendants = true) { contentDescription = "${m.month.month.name.lowercase()}, received ${IndianFormat.spoken(m.received)}, spent ${IndianFormat.spoken(m.spent)}" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                            // Only the chosen month is in colour; the others are two quiet greys, never a muddy tint.
                            listOf(
                                Triple(m.received, c.brand, c.ink.copy(alpha = if (c.isDark) 0.3f else 0.22f)),
                                Triple(m.spent, c.fuel, c.ink.copy(alpha = if (c.isDark) 0.16f else 0.11f)),
                            ).forEach { (v, col, grey) ->
                                val h by androidx.compose.animation.core.animateFloatAsState((v / max).coerceIn(0.04f, 1f), Motion.gentle(), label = "h")
                                val fill by androidx.compose.animation.animateColorAsState(if (on) col else grey, Motion.soft(), label = "c")
                                Box(Modifier.width(14.dp).fillMaxHeight(h).clip(RoundedCornerShape(Radius.chip)).background(fill))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            m.month.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH),
                            style = if (on) Ruvo.type.bodyStrong else Ruvo.type.label, color = if (on) c.ink else c.inkSoft,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                LegendDot(c.brand, "Received")
                LegendDot(c.fuel, "Spent")
            }
            Spacer(Modifier.height(Space.md))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
            Spacer(Modifier.height(Space.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sel.month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + sel.month.year,
                    Modifier.weight(1f), style = Ruvo.type.title, color = c.ink,
                )
                // The left-over figure is in the row below, so the header only flags an overspend.
                if (sel.leftOver < 0) Text("Over by " + IndianFormat.rupees(-sel.leftOver), style = Ruvo.type.caption, color = c.warning)
            }
            Spacer(Modifier.height(Space.xs))
            Row {
                listOf("Received" to sel.received, "Spent" to sel.spent, "Left" to sel.leftOver).forEach { (k, v) ->
                    Column(Modifier.weight(1f)) {
                        Eyebrow(k)
                        Amount(v, style = Ruvo.type.bodyStrong, color = if (k == "Left" && v < 0) c.warning else c.ink)
                    }
                }
            }
            Spacer(Modifier.height(Space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                PrimaryButton("View transactions", { onView(sel.month) }, Modifier.weight(1f))
                GhostButton("Edit", { onEdit(sel.month) }, Modifier.weight(0.6f))
            }
        }
        months.reversed().forEachIndexed { i, m ->
            RuvoCard(Modifier.fillMaxWidth().padding(top = Space.xs).reveal(3 + i)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + m.month.year, Modifier.weight(1f), style = Ruvo.type.title, color = c.ink)
                    if (m.edited) { com.ruvo.design.Tag("Edited"); Spacer(Modifier.width(Space.xs)) }
                    Text(
                        "Edit", Modifier.clip(RoundedCornerShape(Radius.chip)).border(1.dp, c.ink, RoundedCornerShape(Radius.chip))
                            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Correct this month") { onEdit(m.month) }
                            .padding(horizontal = Space.sm, vertical = 6.dp),
                        style = Ruvo.type.label, color = c.ink,
                    )
                }
                Spacer(Modifier.height(Space.xs))
                Row {
                    listOf("Received" to m.received, "Spent" to m.spent, "Left over" to m.leftOver).forEach { (k, v) ->
                        Column(Modifier.weight(1f)) {
                            Eyebrow(k)
                            Amount(v, style = Ruvo.type.label, color = if (k == "Left over" && v < 0) c.warning else c.ink)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun Empty(text: String) {
    Text(text, Modifier.padding(Space.gutter), style = Ruvo.type.body, color = Ruvo.colors.inkSoft)
}

@Composable
private fun TxnRow(entry: com.ruvo.data.ActivityItem, state: AppState, open: (String) -> Unit) {
    val c = Ruvo.colors
    val t = entry.txn
    val credit = t.direction == Direction.CREDIT
    val ignored = t.status == TxnStatus.IGNORED
    val tags = buildList {
        add(t.category)
        if (!credit) add(bucketName(t.bucket))
        if (t.othersShare > 0) add("shared")
        if (t.status == TxnStatus.TO_SORT) add("to sort")
        if (ignored) add("ignored")
    }.joinToString(" · ")
    ListRow(
        title = t.merchant, subtitle = tags, modifier = Modifier.padding(horizontal = Space.gutter),
        leading = { MerchantLogo(t.merchant, t.category) },
        onClick = { open(t.id) },
    ) {
        Amount(
            if (credit) t.amount else -t.yourShare,
            color = when { ignored -> c.inkFaint; credit -> c.brandInk; else -> c.ink }, signed = credit,
        )
    }
}

@Composable
private fun Buckets(state: AppState) {
    val c = Ruvo.colors
    val r = state.runway
    val total = (r.fixedReserved + r.futureProtected + r.fuel.coerceAtLeast(0)).coerceAtLeast(1)
    Column(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        listOf(Bucket.FIXED to r.fixedReserved, Bucket.FUEL to r.fuel.coerceAtLeast(0), Bucket.FUTURE to r.futureProtected).forEach { (b, amount) ->
            RuvoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(bucketName(b), color = c.bucketInk(b))
                        Text(bucketMeaning(b), style = Ruvo.type.caption, color = c.inkSoft)
                    }
                    Amount(amount, style = Ruvo.type.title)
                }
                Spacer(Modifier.height(Space.sm))
                FillBar(amount.toFloat() / total, c.bucket(b))
                if (b == Bucket.FIXED) {
                    Spacer(Modifier.height(Space.sm))
                    val scope = rememberCoroutineScope()
                    // null = closed, "" = adding a new one, otherwise the id being edited.
                    var editing by remember { mutableStateOf<String?>(null) }
                    if (state.commitments.isEmpty()) {
                        Text("Nothing planned yet. Add rent, a phone plan or fees so Ruvo keeps them aside.", style = Ruvo.type.caption, color = c.inkSoft)
                    }
                    state.commitments.forEach { cm ->
                        val due = r.reservedCommitments.firstOrNull { it.id == cm.id }
                        val paidNow = cm.paidThisMonth(state.today)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(Radius.inner))
                                .clickable(role = Role.Button, onClickLabel = "Edit ${cm.name}") { editing = cm.id }
                                .padding(vertical = Space.xxs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cm.name, style = Ruvo.type.bodyStrong, color = c.ink)
                                Text(
                                    (if (paidNow) "Paid this month" else due?.dueDate?.let { "Due ${IndianFormat.date(it, state.today)}" } ?: "Not paid this month") +
                                        (cm.dueDayOfMonth?.takeIf { cm.recurring }?.let { " · monthly on day $it" } ?: ""),
                                    style = Ruvo.type.caption, color = c.inkFaint,
                                )
                            }
                            Amount(cm.amount, style = Ruvo.type.label)
                            Spacer(Modifier.width(Space.xs))
                            // Paid this month with a tick, or Unpaid, which you tap once it is paid. A new month resets it.
                            PaidStatus(paid = paidNow, name = cm.name) { com.ruvo.data.RuvoGraph.appScope.launch { RuvoGraph.repository.payCommitment(cm) } }
                            Spacer(Modifier.width(Space.xxs))
                            Icon(RuvoIcons.Forward, null, Modifier.size(16.dp), tint = c.inkFaint)
                        }
                    }
                    Spacer(Modifier.height(Space.xs))
                    GhostButton("Add a fixed cost", { editing = "" }, Modifier.fillMaxWidth(), icon = RuvoIcons.Plus)
                    editing?.let { id ->
                        CommitmentSheet(state.commitments.firstOrNull { it.id == id }, onDismiss = { editing = null })
                    }
                }
            }
        }
    }
}

/** "Paid" with a tick when this cycle's payment is done; "Unpaid" otherwise, which marks it paid when tapped. */
@Composable
private fun PaidStatus(paid: Boolean, name: String, onMarkPaid: () -> Unit) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    if (paid) {
        Row(
            Modifier.heightIn(min = 36.dp).clip(shape).background(c.brandTint).padding(horizontal = 10.dp, vertical = 6.dp)
                .semantics(mergeDescendants = true) { contentDescription = "$name is paid for this cycle" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(RuvoIcons.Check, null, Modifier.size(14.dp), tint = c.brandInk)
            Spacer(Modifier.width(4.dp))
            Text("Paid", style = Ruvo.type.label, color = c.brandInk)
        }
    } else {
        Text(
            "Unpaid",
            Modifier.heightIn(min = 36.dp).clip(shape).border(1.dp, c.warning, shape)
                .clickable(role = Role.Button, onClickLabel = "Mark $name as paid", onClick = onMarkPaid)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            style = Ruvo.type.label, color = c.warning,
        )
    }
}

/**
 * Adds or edits one Fixed cost: its name, amount and the day of the month it falls due. Saving is only possible
 * with a name, an amount above zero and a day from 1 to 31. Removing asks once before it happens.
 */
@Composable
private fun CommitmentSheet(cm: com.ruvo.engine.Commitment?, onDismiss: () -> Unit) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val repo = RuvoGraph.repository
    var name by remember { mutableStateOf(cm?.name ?: "") }
    var amount by remember { mutableStateOf(cm?.amount?.let { (it / 100).toString() } ?: "") }
    var day by remember { mutableStateOf((cm?.dueDayOfMonth ?: 1).toString()) }
    var removing by remember { mutableStateOf(false) }
    val paise = parseRupees(amount) ?: 0
    val dueDay = day.toIntOrNull()
    val monthly = cm == null || cm.recurring
    val dayOk = !monthly || dueDay in 1..31
    RuvoSheet(if (cm == null) "Add a fixed cost" else "Edit ${cm.name}", onDismiss, eyebrow = "Fixed") {
        Text("Ruvo keeps this aside before it counts your runway.", style = Ruvo.type.caption, color = c.inkSoft)
        Spacer(Modifier.height(Space.sm))
        RuvoField(name, { name = it }, "Name, like Rent or Phone plan")
        Spacer(Modifier.height(Space.sm))
        RuvoField(amount, { amount = it }, "Amount", number = true, prefix = "₹ ")
        if (monthly) {
            Spacer(Modifier.height(Space.sm))
            RuvoField(day, { v -> day = v.filter { it.isDigit() }.take(2) }, "Day of the month it's due", number = true)
            if (!dayOk) Text("Use a day from 1 to 31.", Modifier.padding(top = Space.xxs), style = Ruvo.type.caption, color = c.warning)
        }
        Spacer(Modifier.height(Space.md))
        PrimaryButton(
            if (cm == null) "Add" else "Save changes", {
                com.ruvo.data.RuvoGraph.appScope.launch {
                    if (cm == null) repo.addCommitment(name.trim(), paise, dueDay ?: 1)
                    else repo.updateCommitment(cm, name, paise, dueDay)
                }
                onDismiss()
            }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && paise > 0 && dayOk,
        )
        if (cm != null) {
            Spacer(Modifier.height(Space.sm))
            if (!removing) {
                androidx.compose.material3.TextButton({ removing = true }, Modifier.fillMaxWidth()) {
                    Text("Remove this fixed cost", style = Ruvo.type.label, color = c.warning)
                }
            } else {
                RuvoCard(Modifier.fillMaxWidth(), color = c.warning.copy(alpha = 0.1f), border = c.warning.copy(alpha = 0.5f)) {
                    Text("Remove ${cm.name}?", style = Ruvo.type.title, color = c.ink)
                    Text("Ruvo stops keeping ${IndianFormat.rupees(cm.amount)} aside for it, so your Fuel and runway grow.", style = Ruvo.type.body, color = c.inkSoft)
                    Spacer(Modifier.height(Space.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        GhostButton("Keep it", { removing = false }, Modifier.weight(1f))
                        com.ruvo.design.AccentButton("Remove", { RuvoGraph.appScope.launch { repo.removeCommitment(cm.id) }; onDismiss() }, Modifier.weight(1f), accent = c.warning)
                    }
                }
            }
        }
    }
}

@Composable
private fun Owed(state: AppState) {
    val c = Ruvo.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var side by rememberSaveable { mutableStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    if (adding) OweSheet { adding = false }
    Column(Modifier.padding(horizontal = Space.gutter)) {
        // Both directions side by side: what friends owe you, and what you owe them.
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            RuvoCard(Modifier.weight(1f), brush = c.bucketBrush(Bucket.FUTURE), border = null, onClick = { side = 0 }, onClickLabel = "Show what friends owe you") {
                Eyebrow("Owed to you", color = c.onAccent.copy(alpha = 0.75f))
                Amount(state.owedToYou, style = Ruvo.type.title, color = c.onAccent)
            }
            RuvoCard(Modifier.weight(1f), brush = c.bucketBrush(Bucket.FIXED), border = null, onClick = { side = 1 }, onClickLabel = "Show what you owe") {
                Eyebrow("You owe", color = c.onAccent.copy(alpha = 0.75f))
                Amount(state.youOwe, style = Ruvo.type.title, color = c.onAccent)
            }
        }
        Spacer(Modifier.height(Space.sm))
        SegmentedControl(listOf("Owed to you", "You owe"), side, { side = it })
        Spacer(Modifier.height(Space.xs))
        if (side == 1) {
            YouOwe(state) { adding = true }
            return@Column
        }
        if (state.owed.isEmpty()) {
            Empty("Nobody owes you anything. When you add a spend, switch on Shared with friends to split it.")
            return@Column
        }
        Text("Ruvo never moves money. Friends pay you with a UPI link.", style = Ruvo.type.caption, color = c.inkSoft)
        SectionTitle("Waiting")
        state.owed.forEach { o ->
            ListRow(
                title = o.person, subtitle = "${o.merchant} · ${IndianFormat.relativeDay(o.date, state.today)}",
                leading = { PersonAvatar(o.person, c.fixed) },
            ) { Amount(o.amount) }
            Row(Modifier.fillMaxWidth().padding(bottom = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                GhostButton("Send UPI link", {
                    val link = RuvoGraph.repository.upiLink(o.amount, "${o.merchant} split")
                    val text = if (link != null) "Hey ${o.person}, your share for ${o.merchant} is ${IndianFormat.rupees(o.amount)}. Pay here: $link"
                    else "Hey ${o.person}, your share for ${o.merchant} is ${IndianFormat.rupees(o.amount)}."
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Send to ${o.person}"))
                }, Modifier.weight(1f))
                PrimaryButton("They paid", {
                    com.ruvo.data.RuvoGraph.appScope.launch { RuvoGraph.repository.markSharePaid(o.shareId) }
                    com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Text("${o.person} paid you back ${IndianFormat.rupees(o.amount)}"))
                }, Modifier.weight(1f))
            }
        }
        if (state.settings.upiVpa.isBlank()) {
            Text("Add your UPI ID in Settings so the message carries a tap-to-pay link.", style = Ruvo.type.caption, color = c.inkFaint)
        }
    }
}

/**
 * What you owe friends who paid for you. Each one sits in Fixed until you pay it back, so the runway already
 * counts it. "I paid them back" settles it as a Fixed payment; "Remove" undoes a mistake.
 */
@Composable
private fun YouOwe(state: AppState, onAdd: () -> Unit) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    PrimaryButton("A friend paid for me", onAdd, Modifier.fillMaxWidth(), icon = RuvoIcons.Plus)
    Spacer(Modifier.height(Space.xs))
    if (state.debts.isEmpty()) {
        Empty("You don't owe anyone. When a friend pays for you, add it here and Ruvo sets the money aside until you pay them back.")
        return
    }
    Text("Set aside in Fixed until you pay it back, so your runway already counts it.", style = Ruvo.type.caption, color = c.inkSoft)
    SectionTitle("To pay back")
    state.debts.forEach { d ->
        ListRow(
            title = d.person,
            subtitle = listOf(d.note, IndianFormat.relativeDay(d.date, state.today)).filter { it.isNotBlank() }.joinToString(" · "),
            leading = { PersonAvatar(d.person, c.fixed) },
        ) { Amount(d.amount) }
        Row(Modifier.fillMaxWidth().padding(bottom = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            GhostButton("Remove", { com.ruvo.data.RuvoGraph.appScope.launch { RuvoGraph.repository.deleteDebt(d.id) } }, Modifier.weight(1f))
            PrimaryButton("I paid them back", {
                com.ruvo.data.RuvoGraph.appScope.launch { RuvoGraph.repository.payBack(d.id) }
                com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Text("Paid back ${d.person} ${IndianFormat.rupees(d.amount)} · all settled"))
            }, Modifier.weight(1.4f))
        }
    }
}

/** "A friend paid for me": who, how much, what for, then Save. A short "Saved" line confirms it. */
@Composable
fun OweSheet(onDismiss: () -> Unit) {
    val c = Ruvo.colors
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val paise = parseRupees(amount) ?: 0
    RuvoSheet("A friend paid for me", onDismiss, eyebrow = "You owe") {
        Text("Ruvo sets this aside in Fixed until you pay them back, so you never spend it by accident.", style = Ruvo.type.body, color = c.inkSoft)
        Spacer(Modifier.height(Space.md))
        RuvoField(person, { person = it.take(30) }, "Who paid? e.g. Priya")
        Spacer(Modifier.height(Space.sm))
        RuvoField(amount, { amount = it }, "How much you owe", number = true, prefix = "₹ ")
        Spacer(Modifier.height(Space.sm))
        RuvoField(note, { note = it.take(30) }, "What for? (optional) e.g. Dinner")
        Spacer(Modifier.height(Space.lg))
        PrimaryButton(
            if (paise > 0 && person.isNotBlank()) "Save · you owe ${person.trim()} ${IndianFormat.rupees(paise)}" else "Save",
            {
                val who = person.trim(); val what = note.trim(); val owe = paise
                RuvoGraph.appScope.launch { RuvoGraph.repository.addDebt(who, owe, what) }
                com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Text("Saved · you owe $who ${IndianFormat.rupees(owe)}"))
                onDismiss()
            },
            Modifier.fillMaxWidth(), enabled = paise > 0 && person.isNotBlank(),
        )
    }
}

@Composable
private fun BalanceSheet(state: AppState, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf((state.runway.balance / 100).toString()) }
    val scope = rememberCoroutineScope()
    RuvoSheet("How much do you have right now?", onDismiss, eyebrow = "Update balance") {
        Text("Add up your bank balance and cash. Ruvo counts from here.", style = Ruvo.type.body, color = Ruvo.colors.inkSoft)
        Spacer(Modifier.height(Space.md))
        RuvoField(text, { text = it }, "Total", number = true, prefix = "₹ ")
        Spacer(Modifier.height(Space.lg))
        PrimaryButton("Save", {
            // Saved on the app's own scope: the sheet closes at once, which would otherwise cancel the save.
            parseRupees(text)?.let { v ->
                val before = state.runway.runwayDays
                com.ruvo.data.RuvoGraph.appScope.launch {
                    RuvoGraph.repository.setBalance(v)
                    // Every number follows the new balance; say how the runway moved once it has.
                    val after = RuvoGraph.repository.state.first { it.input.balance == v }.runway
                    val runway = if (after.unlimited) "" else if (after.runwayDays == before) " · runway still ${IndianFormat.days(after.runwayDays)}" else " · runway $before → ${IndianFormat.days(after.runwayDays)}"
                    com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Text("Balance saved: ${IndianFormat.rupees(v)}$runway"))
                }
            }
            onDismiss()
        }, Modifier.fillMaxWidth())
    }
}

/** A single transaction: re-file, rename, split, ignore or delete. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TxnSheet(id: String, startWithSplit: Boolean, onDismiss: () -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val item = state.activity.firstOrNull { it.txn.id == id } ?: return onDismiss()
    val t = item.txn
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val repo = RuvoGraph.repository
    var name by remember(id) { mutableStateOf(t.merchant) }
    var splitOpen by remember(id) { mutableStateOf(startWithSplit) }
    var splitSaved by remember(id) { mutableStateOf<String?>(null) }
    // The usual categories, then any the user made, then this payment's own if it is something else again.
    val categories = (listOf("Food", "Groceries", "Transport", "Shopping", "Entertainment", "Subscriptions", "Bills", "Education", "Health", "Other") +
        state.settings.customCategories.sorted() + t.category).distinct()
    var making by remember(id) { mutableStateOf(false) }
    var newCategory by remember(id) { mutableStateOf("") }

    RuvoSheet(IndianFormat.rupees(t.amount), onDismiss, eyebrow = "${IndianFormat.relativeDay(t.date, state.today)} · ${if (t.direction == Direction.CREDIT) "money in" else "payment"}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MerchantLogo(name.ifBlank { t.merchant }, t.category, size = 52.dp)
            Spacer(Modifier.width(Space.sm))
            RuvoField(name, { name = it }, "Name", Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.md))
        Eyebrow("Category")
        Spacer(Modifier.height(Space.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            categories.forEach { cat ->
                Chip(cat, t.category == cat, { com.ruvo.data.RuvoGraph.appScope.launch { repo.recategorise(id, name.ifBlank { t.merchant }, cat) } })
            }
            Chip(if (making) "Cancel" else "+ New category", making, { making = !making })
        }
        if (making) {
            Spacer(Modifier.height(Space.sm))
            RuvoField(newCategory, { newCategory = it.take(24) }, "Category name, e.g. Gym, Gifts, Travel")
            Spacer(Modifier.height(Space.xs))
            GhostButton("Use this category", {
                val n = newCategory.trim().replaceFirstChar { it.uppercase() }
                if (n.isNotBlank()) scope.launch {
                    repo.addCategory(n)
                    repo.recategorise(id, name.ifBlank { t.merchant }, n)
                    making = false; newCategory = ""
                }
            }, Modifier.fillMaxWidth())
        }
        if (t.direction == Direction.DEBIT) {
            Spacer(Modifier.height(Space.md))
            Eyebrow("Bucket")
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                // A payment is planned (Fixed) or everyday (Fuel); money for goals moves by pouring, not by paying.
                listOf(Bucket.FIXED, Bucket.FUEL).forEach { b -> Chip(bucketName(b), t.bucket == b, { com.ruvo.data.RuvoGraph.appScope.launch { repo.setBucket(id, b) } }, accent = c.bucket(b)) }
            }
            Spacer(Modifier.height(Space.sm))
            val oneOff = id in state.settings.oneOffTxns
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(oneOff, role = Role.Switch) { on ->
                    com.ruvo.data.RuvoGraph.appScope.launch { repo.setOneOff(id, on) }
                    com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Text(if (on) "Marked one-off · left out of your usual day" else "Counted in your usual day again"))
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("One-off?", style = Ruvo.type.bodyStrong, color = c.ink)
                    Text("Big and rare, like a trip. Still spent, but left out of your usual day.", style = Ruvo.type.caption, color = c.inkSoft)
                }
                androidx.compose.material3.Switch(oneOff, null)
            }
            Spacer(Modifier.height(Space.md))
            RuvoCard {
                Text(if (t.othersShare > 0) "Shared · your part ${IndianFormat.rupees(t.yourShare)}" else "Shared with friends?", style = Ruvo.type.bodyStrong, color = c.ink)
                // After Save, a clear "Saved" line right here (the sheet stays open, so the app-wide note would be hidden).
                splitSaved?.let { line ->
                    Spacer(Modifier.height(Space.xxs))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }) {
                        Icon(RuvoIcons.Check, null, Modifier.size(16.dp), tint = c.future)
                        Spacer(Modifier.width(6.dp))
                        Text(line, style = Ruvo.type.label, color = c.ink)
                    }
                }
                if (!splitOpen) {
                    Spacer(Modifier.height(Space.xs))
                    GhostButton(if (t.othersShare > 0) "Change split" else "Split it", { splitOpen = true }, Modifier.fillMaxWidth())
                } else {
                    Spacer(Modifier.height(Space.sm))
                    var owed by remember(id) { mutableStateOf(item.shares.map { it.person to it.amount }) }
                    com.ruvo.design.SplitEditor(t.amount, item.shares.map { it.person to it.amount }) { owed = it }
                    Spacer(Modifier.height(Space.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        if (t.othersShare > 0) GhostButton("Not shared", { com.ruvo.data.RuvoGraph.appScope.launch { repo.unsplit(id) }; splitOpen = false; splitSaved = "Saved · not shared any more" }, Modifier.weight(1f))
                        PrimaryButton("Save split", {
                            com.ruvo.data.RuvoGraph.appScope.launch { repo.splitByAmounts(id, owed) }
                            splitOpen = false
                            val names = owed.map { it.first }.let { if (it.size <= 2) it.joinToString(" and ") else "${it.size} friends" }
                            splitSaved = "Saved · $names ${if (owed.size == 1) "owes" else "owe"} you ${IndianFormat.rupees(owed.sumOf { it.second })}"
                        }, Modifier.weight(1f), enabled = owed.isNotEmpty())
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (t.status == TxnStatus.IGNORED) GhostButton("Count it again", { com.ruvo.data.RuvoGraph.appScope.launch { repo.restore(id) } }, Modifier.weight(1f))
            else GhostButton("Ignore", { RuvoGraph.appScope.launch { repo.ignore(id) }; onDismiss() }, Modifier.weight(1f))
            PrimaryButton("Done", {
                if (name != t.merchant && name.isNotBlank()) com.ruvo.data.RuvoGraph.appScope.launch { repo.recategorise(id, name, t.category) }
                if (t.status == TxnStatus.TO_SORT) com.ruvo.data.RuvoGraph.appScope.launch { repo.confirm(id) }
                onDismiss()
            }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.xs))
        GhostButton("Delete", { RuvoGraph.appScope.launch { repo.delete(id) }; onDismiss() }, Modifier.fillMaxWidth())
        if (item.source == com.ruvo.data.db.Source.SMS) {
            Text("Read from a bank SMS on this phone. The message itself is never uploaded.", Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkFaint)
        }
    }
}

/**
 * Expected money: amounts with a rough date, Sure or Maybe. The earliest Sure one becomes the next money; Maybe ones are
 * shown but never counted on. Lives in Settings → Runway, next to the usual day. The add form opens inside the card,
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
