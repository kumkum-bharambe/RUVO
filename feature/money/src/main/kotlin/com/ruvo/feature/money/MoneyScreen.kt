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
import androidx.compose.foundation.layout.width
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
import com.ruvo.engine.TxnStatus
import kotlinx.coroutines.launch

private enum class MoneyTab(val label: String) { ACTIVITY("Activity"), BUCKETS("Buckets"), OWED("Owed to you") }

@Composable
fun MoneyScreen(openTxn: (String) -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MoneyTab.ACTIVITY) }
    var balanceSheet by remember { mutableStateOf(false) }
    val c = Ruvo.colors

    LazyColumn(Modifier.fillMaxSize().background(c.canvas), contentPadding = PaddingValues(bottom = Space.dockClearance)) {
        item {
            Column(Modifier.statusBarsPadding().padding(horizontal = Space.gutter, vertical = Space.md)) {
                Eyebrow("Money")
                Text("Where it is, where it went", Modifier.semantics { heading() }, style = Ruvo.type.headline, color = c.ink)
            }
        }
        item {
            RuvoCard(Modifier.padding(horizontal = Space.gutter), color = c.ink, border = null, onClick = { balanceSheet = true }, onClickLabel = "Update balance") {
                Eyebrow("You have", color = c.canvas.copy(alpha = 0.7f))
                Amount(state.runway.balance, style = Ruvo.type.display, color = c.canvas)
                Spacer(Modifier.height(Space.xxs))
                Text("Across accounts and cash · tap to update", style = Ruvo.type.caption, color = c.canvas.copy(alpha = 0.7f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                MoneyTab.entries.forEach { t ->
                    val label = if (t == MoneyTab.OWED && state.owedToYou > 0) "${t.label} · ${IndianFormat.compact(state.owedToYou)}" else t.label
                    Chip(label, tab == t, { tab = t })
                }
            }
        }
        when (tab) {
            MoneyTab.ACTIVITY -> {
                val groups = state.activity.groupBy { it.txn.date }.toSortedMap(compareByDescending { it })
                if (groups.isEmpty()) item { Empty("Nothing yet. Pull the Orb down to add a spend, or turn on SMS tracking in Settings.") }
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
            MoneyTab.BUCKETS -> item { Buckets(state) }
            MoneyTab.OWED -> item { Owed(state) }
        }
    }
    if (balanceSheet) BalanceSheet(state) { balanceSheet = false }
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
        leading = { MerchantTile(t.merchant, if (credit) c.fuel else c.bucket(t.bucket)) },
        onClick = { open(t.id) },
    ) {
        Amount(
            if (credit) t.amount else -t.yourShare,
            color = when { ignored -> c.inkFaint; credit -> c.fuel; else -> c.ink }, signed = credit,
        )
    }
}

@Composable
private fun Buckets(state: AppState) {
    val c = Ruvo.colors
    val r = state.runway
    val total = (r.fixedReserved + r.futureProtected + r.fuel.coerceAtLeast(0)).coerceAtLeast(1)
    Column(Modifier.padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        listOf(Bucket.FIXED to r.fixedReserved, Bucket.FUEL to r.fuel.coerceAtLeast(0), Bucket.FUTURE to r.futureProtected).forEach { (b, amount) ->
            RuvoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(bucketName(b), color = c.bucket(b))
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
        RuvoCard(color = c.fuel.copy(alpha = if (c.isDark) 0.14f else 0.16f), border = null) {
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
