package com.ruvo.feature.money

import android.content.Intent
import androidx.compose.foundation.background
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
import com.ruvo.design.MerchantTile
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
import kotlinx.coroutines.launch

private enum class MoneyTab(val label: String) { OVERVIEW("Overview"), ACTIVITY("Activity"), HISTORY("History"), OWED("Owed") }

private enum class Period(val label: String, val days: Long?) { WEEK("This week", 7), LAST_WEEK("Last week", 14), MONTH("This month", 31), ALL("All", null) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyScreen(openTxn: (String) -> Unit, openAdd: () -> Unit = {}) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MoneyTab.OVERVIEW) }
    var period by rememberSaveable { mutableStateOf(Period.WEEK) }
    var balanceSheet by remember { mutableStateOf(false) }
    val c = Ruvo.colors

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Space.dockClearance)) {
        item {
            Row(Modifier.statusBarsPadding().padding(horizontal = Space.gutter, vertical = Space.md), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Money")
                    Text("Where it is, where it went", Modifier.semantics { heading() }, style = Ruvo.type.headline, color = c.ink)
                }
                IconCircle(RuvoIcons.Plus, "Add a spend", openAdd, tint = c.onAccent, background = c.fuel)
            }
        }
        item { BalanceCard(state, Modifier.padding(horizontal = Space.gutter).reveal(0)) { balanceSheet = true } }
        item {
            SegmentedControl(
                MoneyTab.entries.map { if (it == MoneyTab.OWED && state.owedToYou > 0) "Owed · ${IndianFormat.compact(state.owedToYou)}" else it.label },
                tab.ordinal, { tab = MoneyTab.entries[it] },
                Modifier.padding(horizontal = Space.gutter, vertical = Space.md).reveal(1),
            )
        }
        when (tab) {
            MoneyTab.OVERVIEW -> {
                item { WhereItWent(state, Modifier.reveal(2)) }
                item { Buckets(state) }
            }
            MoneyTab.ACTIVITY -> {
                item {
                    FlowRow(Modifier.padding(horizontal = Space.gutter), horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Period.entries.forEach { p -> Chip(p.label, period == p, { period = p }) }
                    }
                }
                val inPeriod = state.activity.filter { entry ->
                    val ago = state.today.toEpochDay() - entry.txn.date.toEpochDay()
                    when (period) {
                        Period.WEEK -> ago in 0..6
                        Period.LAST_WEEK -> ago in 7..13
                        Period.MONTH -> entry.txn.date.month == state.today.month && entry.txn.date.year == state.today.year
                        Period.ALL -> true
                    }
                }
                val groups = inPeriod.groupBy { it.txn.date }.toSortedMap(compareByDescending { it })
                if (groups.isEmpty()) item { Empty(if (state.activity.isEmpty()) "Nothing yet. Pull the Orb down to add a spend, or turn on SMS tracking in Settings." else "Nothing in ${period.label.lowercase()}.") }
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
            MoneyTab.HISTORY -> item { History(state) }
            MoneyTab.OWED -> item { Owed(state) }
        }
    }
    if (balanceSheet) BalanceSheet(state) { balanceSheet = false }
}

/** The balance on navy, with how it splits into goals, planned and yours to spend. Can be hidden. */
@Composable
private fun BalanceCard(state: AppState, modifier: Modifier, onUpdate: () -> Unit) {
    val c = Ruvo.colors
    val r = state.runway
    val scope = rememberCoroutineScope()
    val hidden = state.settings.hideBalance
    RuvoCard(modifier.fillMaxWidth(), brush = c.heroBrush, border = null, elevated = true, onClick = onUpdate, onClickLabel = "Update balance") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("You have", color = c.onHero.copy(alpha = 0.7f))
                if (hidden) Text("₹ ••••", style = Ruvo.type.display, color = c.onHero)
                else Amount(r.balance, style = Ruvo.type.display, color = c.onHero)
            }
            IconCircle(
                if (hidden) RuvoIcons.Eye else RuvoIcons.EyeOff, if (hidden) "Show balance" else "Hide balance",
                { scope.launch { RuvoGraph.repository.setHideBalance(!hidden) } },
                tint = c.onHero, background = c.onHero.copy(alpha = 0.12f),
            )
        }
        Spacer(Modifier.height(Space.sm))
        val total = (r.fixedReserved + r.futureProtected + r.fuel.coerceAtLeast(0)).toFloat().coerceAtLeast(1f)
        SplitBar(listOf(Segment(r.futureProtected / total, c.future), Segment(r.fixedReserved / total, c.fixed), Segment(r.fuel.coerceAtLeast(0) / total, c.fuel)))
        Spacer(Modifier.height(Space.sm))
        Row {
            listOf(Triple(c.future, "In goals", r.futureProtected), Triple(c.fixed, "Planned", r.fixedReserved), Triple(c.fuel, "Yours to use", r.fuel.coerceAtLeast(0))).forEach { (col, label, v) ->
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(Modifier.padding(end = 6.dp).size(8.dp).clip(CircleShape).background(col))
                        Text(label, style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.7f), maxLines = 1)
                    }
                    if (hidden) Text("••••", style = Ruvo.type.bodyStrong, color = c.onHero) else Amount(v, style = Ruvo.type.bodyStrong, color = c.onHero)
                }
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text("Across accounts and cash · tap to update", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.6f))
    }
}

/** Spending by category over the last 30 days. */
@Composable
private fun WhereItWent(state: AppState, modifier: Modifier) {
    val c = Ruvo.colors
    val cats = remember(state.input.transactions, state.today) { Insights.byCategory(state.input.transactions, state.today, 30) }
    if (cats.isEmpty()) return
    val top = cats.first().second.toFloat().coerceAtLeast(1f)
    val palette = listOf(c.fuel, c.future, c.fixed, c.warning, c.sun, c.inkFaint)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun History(state: AppState) {
    val c = Ruvo.colors
    val months = remember(state.input.transactions, state.today) { Insights.months(state.input.transactions, state.today, 3) }
    Column(Modifier.padding(horizontal = Space.gutter)) {
        RuvoCard(Modifier.fillMaxWidth().reveal(2)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Received and spent", Modifier.weight(1f), style = Ruvo.type.title, color = c.ink)
                Text("Last 3 months", style = Ruvo.type.caption, color = c.inkFaint)
            }
            Spacer(Modifier.height(Space.md))
            val max = months.maxOfOrNull { maxOf(it.received, it.spent) }?.toFloat()?.coerceAtLeast(1f) ?: 1f
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                months.forEach { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PillBars(
                            listOf(
                                Pill("", m.received / max * 100f, spoken = "${m.month.month.name.lowercase()} received ${IndianFormat.spoken(m.received)}"),
                                Pill("", m.spent / max * 100f, highlight = true, spoken = "spent ${IndianFormat.spoken(m.spent)}"),
                            ),
                            Modifier.width(64.dp), height = 120.dp, color = c.future, highlightColor = c.fuel,
                        )
                        Text(m.month.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH), style = Ruvo.type.label, color = c.inkSoft)
                    }
                }
            }
            Spacer(Modifier.height(Space.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                LegendDot(c.future, "Received")
                LegendDot(c.fuel, "Spent")
            }
        }
        months.reversed().forEachIndexed { i, m ->
            RuvoCard(Modifier.fillMaxWidth().padding(top = Space.xs).reveal(3 + i)) {
                Text(m.month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + m.month.year, style = Ruvo.type.bodyStrong, color = c.ink)
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
        leading = { MerchantTile(t.merchant, if (credit) c.future else c.bucket(t.bucket)) },
        onClick = { open(t.id) },
    ) {
        Amount(
            if (credit) t.amount else -t.yourShare,
            color = when { ignored -> c.inkFaint; credit -> c.futureInk; else -> c.ink }, signed = credit,
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
                    state.commitments.forEach { cm ->
                        val due = r.reservedCommitments.firstOrNull { it.id == cm.id }
                        Row(Modifier.fillMaxWidth().padding(vertical = Space.xxs), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(cm.name, style = Ruvo.type.bodyStrong, color = c.ink)
                                Text(
                                    if (due != null) "Due ${due.dueDate?.let { IndianFormat.date(it, state.today) }}" else "Paid for this cycle",
                                    style = Ruvo.type.caption, color = c.inkFaint,
                                )
                            }
                            Amount(cm.amount, style = Ruvo.type.label)
                            if (due != null) {
                                Spacer(Modifier.width(Space.xs))
                                Chip("Paid", false, { scope.launch { RuvoGraph.repository.payCommitment(cm) } })
                            }
                        }
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
    Column(Modifier.padding(horizontal = Space.gutter)) {
        if (state.owed.isEmpty()) {
            Empty("Nobody owes you anything. Open a payment and tap Shared to split it.")
            return@Column
        }
        RuvoCard(brush = c.bucketBrush(Bucket.FUTURE), border = null) {
            Eyebrow("Owed to you", color = c.ink)
            Amount(state.owedToYou, style = Ruvo.type.display)
            Text("Ruvo never moves money. Friends pay you with a UPI link.", style = Ruvo.type.caption, color = c.inkSoft)
        }
        SectionTitle("Waiting")
        state.owed.forEach { o ->
            ListRow(
                title = o.person, subtitle = "${o.merchant} · ${IndianFormat.relativeDay(o.date, state.today)}",
                leading = { MerchantTile(o.person, c.fixed) },
            ) { Amount(o.amount) }
            Row(Modifier.fillMaxWidth().padding(bottom = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                GhostButton("Send UPI link", {
                    val link = RuvoGraph.repository.upiLink(o.amount, "${o.merchant} split")
                    val text = if (link != null) "Hey ${o.person}, your share for ${o.merchant} is ${IndianFormat.rupees(o.amount)}. Pay here: $link"
                    else "Hey ${o.person}, your share for ${o.merchant} is ${IndianFormat.rupees(o.amount)}."
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Send to ${o.person}"))
                }, Modifier.weight(1f))
                PrimaryButton("They paid", { scope.launch { RuvoGraph.repository.markSharePaid(o.shareId) } }, Modifier.weight(1f))
            }
        }
        if (state.settings.upiVpa.isBlank()) {
            Text("Add your UPI ID in Settings so the message carries a tap-to-pay link.", style = Ruvo.type.caption, color = c.inkFaint)
        }
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
            parseRupees(text)?.let { v -> scope.launch { RuvoGraph.repository.setBalance(v) } }
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
    var people by remember(id) { mutableStateOf(item.shares.joinToString(", ") { it.person }) }
    val categories = listOf("Food", "Groceries", "Transport", "Shopping", "Entertainment", "Subscriptions", "Bills", "Education", "Health", "Other")

    RuvoSheet(IndianFormat.rupees(t.amount), onDismiss, eyebrow = "${IndianFormat.relativeDay(t.date, state.today)} · ${if (t.direction == Direction.CREDIT) "money in" else "payment"}") {
        RuvoField(name, { name = it }, "Name")
        Spacer(Modifier.height(Space.md))
        Eyebrow("Category")
        Spacer(Modifier.height(Space.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            categories.forEach { cat ->
                Chip(cat, t.category == cat, { scope.launch { repo.recategorise(id, name.ifBlank { t.merchant }, cat) } })
            }
        }
        if (t.direction == Direction.DEBIT) {
            Spacer(Modifier.height(Space.md))
            Eyebrow("Bucket")
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Bucket.entries.forEach { b -> Chip(bucketName(b), t.bucket == b, { scope.launch { repo.setBucket(id, b) } }, accent = c.bucket(b)) }
            }
            Spacer(Modifier.height(Space.md))
            RuvoCard {
                Text(if (t.othersShare > 0) "Shared · your part ${IndianFormat.rupees(t.yourShare)}" else "Shared with friends?", style = Ruvo.type.bodyStrong, color = c.ink)
                if (!splitOpen) {
                    Spacer(Modifier.height(Space.xs))
                    GhostButton(if (t.othersShare > 0) "Change split" else "Split it", { splitOpen = true }, Modifier.fillMaxWidth())
                } else {
                    Spacer(Modifier.height(Space.xs))
                    RuvoField(people, { people = it }, "Names, separated by commas")
                    val names = people.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    if (names.isNotEmpty()) {
                        Text("${IndianFormat.rupees(t.amount / (names.size + 1))} each, you included", style = Ruvo.type.caption, color = c.inkSoft)
                    }
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        if (t.othersShare > 0) GhostButton("Not shared", { scope.launch { repo.unsplit(id) }; splitOpen = false }, Modifier.weight(1f))
                        PrimaryButton("Split equally", { if (names.isNotEmpty()) scope.launch { repo.split(id, names) }; splitOpen = false }, Modifier.weight(1f), enabled = names.isNotEmpty())
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (t.status == TxnStatus.IGNORED) GhostButton("Count it again", { scope.launch { repo.restore(id) } }, Modifier.weight(1f))
            else GhostButton("Ignore", { scope.launch { repo.ignore(id) }; onDismiss() }, Modifier.weight(1f))
            PrimaryButton("Done", {
                if (name != t.merchant && name.isNotBlank()) scope.launch { repo.recategorise(id, name, t.category) }
                if (t.status == TxnStatus.TO_SORT) scope.launch { repo.confirm(id) }
                onDismiss()
            }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.xs))
        GhostButton("Delete", { scope.launch { repo.delete(id) }; onDismiss() }, Modifier.fillMaxWidth())
        if (item.source == com.ruvo.data.db.Source.SMS) {
            Text("Read from a bank SMS on this phone. The message itself is never uploaded.", Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkFaint)
        }
    }
}
