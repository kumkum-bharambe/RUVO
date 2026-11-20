package com.ruvo.app

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.ruvo.data.AppState
import com.ruvo.data.RuvoGraph
import com.ruvo.data.sms.SmsInboxScanner
import com.ruvo.design.Chip
import com.ruvo.design.Daybreak
import com.ruvo.design.Eyebrow
import com.ruvo.design.GhostButton
import com.ruvo.design.Haptics
import com.ruvo.design.NightSwim
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoColors
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoSheet
import com.ruvo.design.SectionTitle
import com.ruvo.design.Space
import com.ruvo.design.ThemeChoice
import com.ruvo.design.bucketName
import com.ruvo.design.parseRupees
import com.ruvo.engine.BeforeYouBuy
import com.ruvo.engine.Bucket
import com.ruvo.engine.Direction
import com.ruvo.engine.IndianFormat
import kotlinx.coroutines.launch

/**
 * Quick-add in three taps: amount, category, done. Pulled down from the Orb, or opened from the widget,
 * the Quick Settings tile or the app shortcut. Shows the cost in days as you type.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(onDismiss: () -> Unit) {
    val c = Ruvo.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val state = RuvoGraph.repository.state.value
    var amount by remember { mutableStateOf("") }
    var what by remember { mutableStateOf("") }
    var moneyIn by remember { mutableStateOf(false) }
    var bucket by remember { mutableStateOf(Bucket.FUEL) }
    var sharing by remember { mutableStateOf(false) }
    var topUp by remember { mutableStateOf(false) }
    var oneOff by remember { mutableStateOf(false) }
    var splitCategory by remember { mutableStateOf("Food") }
    var owed by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var making by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    val paise = parseRupees(amount) ?: 0
    val quick = listOf("Food", "Transport", "Groceries", "Shopping", "Entertainment", "Education", "Bills", "Other")

    fun save(category: String) {
        if (paise <= 0) return
        Haptics.confirm(view)
        // Everything the sheet knows, copied now, and saved on the app's own scope: the sheet closes straight away,
        // which would otherwise cancel the split before it was written (that is why splits went missing from Owed).
        val amountNow = paise; val label = what.ifBlank { category }; val incoming = moneyIn; val bucketNow = bucket
        val isTopUp = incoming && topUp; val isOneOff = !incoming && oneOff
        val shares = if (!incoming && sharing) owed.filter { it.second > 0 } else emptyList()
        // A short line confirms it (shown by the app shell, read out by TalkBack): "Saved ₹250 · runway 24 → 23 days",
        // or for a split, who owes you what.
        if (shares.isNotEmpty()) {
            val names = shares.map { it.first }.let { if (it.size <= 2) it.joinToString(" and ") else "${it.size} friends" }
            com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Text("Split saved · $names ${if (shares.size == 1) "owes" else "owe"} you ${IndianFormat.rupees(shares.sumOf { it.second })}"))
        } else {
            com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Saved(amountNow, incoming, RuvoGraph.repository.state.value.runway.runwayDays))
        }
        RuvoGraph.appScope.launch {
            // A top-up from home is money in that is not a payday and does not move the next-money date.
            if (isTopUp) { RuvoGraph.repository.addTopUp(amountNow, what.trim()); return@launch }
            val id = RuvoGraph.repository.addManual(
                amountNow, label, if (incoming) "Income" else category,
                if (incoming) Bucket.FUEL else bucketNow, if (incoming) Direction.CREDIT else Direction.DEBIT,
            )
            // A one-off still leaves the balance, but stays out of the usual daily average.
            if (isOneOff) RuvoGraph.repository.setOneOff(id, true)
            // Shared on the way in: friends' parts go straight to Owed in Money.
            if (shares.isNotEmpty()) RuvoGraph.repository.splitByAmounts(id, shares)
        }
        onDismiss()
    }

    RuvoSheet(if (moneyIn) "Money in" else "Add a spend", onDismiss, eyebrow = "Quick add") {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            Chip("Spent", !moneyIn, { moneyIn = false })
            Chip("Received", moneyIn, { moneyIn = true }, accent = c.fuel)
        }
        Spacer(Modifier.height(Space.md))
        RuvoField(amount, { amount = it }, "Amount", number = true, prefix = "₹ ")
        if (paise > 0 && !moneyIn) {
            val b = BeforeYouBuy.evaluate(state.runway, paise)
            Text(
                "About ${if (b.costDays == Math.floor(b.costDays)) IndianFormat.days(b.costDays.toInt()) else "${b.costDays} days"} of spending · runway ${state.runway.runwayDays} → ${b.runwayAfter}",
                Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkSoft,
            )
        }
        Spacer(Modifier.height(Space.sm))
        RuvoField(what, { what = it }, if (moneyIn) "From (optional)" else "What was it? (optional)")
        Spacer(Modifier.height(Space.md))
        if (moneyIn) {
            SwitchLine("Top-up from home", "Extra money between paydays. Your next money date stays the same.", topUp) { topUp = it }
            Spacer(Modifier.height(Space.md))
            PrimaryButton(
                when {
                    paise <= 0 -> "Type the amount to save"
                    topUp -> "Save · ${IndianFormat.rupees(paise)} top-up"
                    else -> "Save · ${IndianFormat.rupees(paise)} received"
                },
                { save("Income") }, Modifier.fillMaxWidth(), enabled = paise > 0,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                // A payment is planned (Fixed) or everyday (Fuel); money for goals moves by pouring, not by paying.
                listOf(Bucket.FIXED, Bucket.FUEL).forEach { b -> Chip(bucketName(b), bucket == b, { bucket = b }, accent = c.bucket(b)) }
            }
            Spacer(Modifier.height(Space.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shared with friends?", Modifier.weight(1f), style = Ruvo.type.bodyStrong, color = c.ink)
                androidx.compose.material3.Switch(sharing, { sharing = it }, enabled = paise > 0)
            }
            val splitting = sharing && paise > 0
            if (splitting) {
                Spacer(Modifier.height(Space.xs))
                com.ruvo.design.SplitEditor(paise, emptyList()) { owed = it }
            }
            Spacer(Modifier.height(Space.xs))
            SwitchLine("One-off?", "Big and rare, like a trip or a deposit. Left out of your usual day.", oneOff) { oneOff = it }
            Spacer(Modifier.height(Space.md))
            // Pick a category, then Save: nothing is saved until the Save button, so a stray tap never records a payment.
            Eyebrow("Pick a category")
            Spacer(Modifier.height(Space.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                (quick + state.settings.customCategories.sorted()).forEach { cat ->
                    Chip(cat, !making && splitCategory == cat, { splitCategory = cat; making = false })
                }
                Chip(if (making) "Cancel" else "+ New category", making, { making = !making })
            }
            // Their own category: name it once, and it is offered every time from now on.
            if (making) {
                Spacer(Modifier.height(Space.sm))
                RuvoField(newCategory, { newCategory = it.take(24) }, "Category name, e.g. Gym, Gifts, Travel")
                Spacer(Modifier.height(Space.xs))
                PrimaryButton(
                    "Save in ${newCategory.trim().ifBlank { "new category" }}",
                    {
                        val n = newCategory.trim().replaceFirstChar { it.uppercase() }
                        RuvoGraph.appScope.launch { RuvoGraph.repository.addCategory(n) }
                        save(n)
                    },
                    Modifier.fillMaxWidth(), enabled = paise > 0 && newCategory.isNotBlank(),
                )
            }
            if (!making) {
                val friends = owed.filter { it.second > 0 }
                Spacer(Modifier.height(Space.md))
                PrimaryButton(
                    when {
                        paise <= 0 -> "Type the amount to save"
                        splitting && friends.isEmpty() -> "Add a friend's name to save"
                        splitting -> "Save split · your part ${IndianFormat.rupees(paise - friends.sumOf { it.second })}"
                        else -> "Save · ${IndianFormat.rupees(paise)} in $splitCategory"
                    },
                    { save(splitCategory) },
                    Modifier.fillMaxWidth(), enabled = paise > 0 && (!splitting || friends.isNotEmpty()),
                )
            }
        }
    }
}

/** Settings: the theme switch with both moods, data and SMS controls, privacy, and delete everything. */
@Composable
fun SettingsSheet(state: AppState, onDismiss: () -> Unit, onPersonas: () -> Unit, onReplayTour: () -> Unit = {}) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repo = RuvoGraph.repository
    val s = state.settings
    var vpa by remember { mutableStateOf(s.upiVpa) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(s.name) }
    var openFaq by remember { mutableStateOf<Int?>(null) }
    var scanned by remember { mutableStateOf<Int?>(null) }
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.READ_SMS] == true) scope.launch {
            repo.setRealSms(true)
            scanned = SmsInboxScanner.scan(context)
        }
    }

    RuvoSheet("Settings", onDismiss) {
        if (!state.isDemo) {
            SectionTitle("Your name")
            RuvoField(name, { name = it }, "What should Ruvo call you?")
            Spacer(Modifier.height(Space.xs))
            GhostButton("Save name", { com.ruvo.data.RuvoGraph.appScope.launch { repo.setName(name) } }, Modifier.fillMaxWidth())
        }

        SectionTitle("Tracking")
        val smsAvailable = remember { com.ruvo.data.sms.SmsSupport.available(context) }
        if (!smsAvailable) {
            Text(
                "This shared version does not read SMS. Sample students show sample bank messages, and you can add spends with + or by pulling Ruvo's pet down.",
                style = Ruvo.type.caption, color = c.inkSoft,
            )
        }
        if (smsAvailable) Toggle("Read payment SMS", "Bank and card payment messages only, read on this phone", s.realSms) { on ->
            if (on) {
                val perms = buildList {
                    add(Manifest.permission.READ_SMS); add(Manifest.permission.RECEIVE_SMS)
                    if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                smsLauncher.launch(perms.toTypedArray())
            } else com.ruvo.data.RuvoGraph.appScope.launch { repo.setRealSms(false) }
        }
        if (smsAvailable) WhatRuvoReads(s.demoSms)
        scanned?.let { Text("Found $it new payments in the last 90 days. They are waiting in your weekly sort.", style = Ruvo.type.caption, color = c.fuel) }
        if (smsAvailable) Toggle("Demo SMS", "Accept test messages from any sender, for emulator demos", s.demoSms) { on -> com.ruvo.data.RuvoGraph.appScope.launch { repo.setDemoSms(on) } }
        Toggle("Spoken replies", "Ruvo reads answers aloud in your chat language", s.spokenReplies) { on -> com.ruvo.data.RuvoGraph.appScope.launch { repo.setSpokenReplies(on) } }

        SectionTitle("Notifications")
        Text("Calm and occasional. Each one can be switched off.", style = Ruvo.type.caption, color = c.inkSoft)
        // Android 13 and later ask once before any notification can show; switching one on asks if needed.
        var allowed by remember { mutableStateOf(com.ruvo.data.work.RuvoNotify.allowed(context)) }
        val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
        val ask = { if (!allowed && android.os.Build.VERSION.SDK_INT >= 33) notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        if (!allowed) {
            Text("Notifications are off for Ruvo on this phone.", Modifier.padding(top = Space.xs), style = Ruvo.type.bodyStrong, color = c.warning)
            GhostButton("Turn on notifications", { ask() }, Modifier.fillMaxWidth().padding(top = Space.xs))
        }
        // One switch for all of them; the individual ones show only while notifications are on.
        val anyOn = s.notifyDaily || s.notifyNews || s.notifyCapture || s.notifyWeekly || s.notifyGoals
        Toggle("Notifications", if (anyOn) "On. Choose which ones below." else "Off. Ruvo sends nothing.", anyOn) { on ->
            if (on) ask()
            com.ruvo.data.RuvoGraph.appScope.launch { repo.setNotifications(capture = on, weekly = on, goals = on, daily = on, news = on) }
            com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Text(if (on) "Notifications on" else "Notifications off"))
        }
        if (anyOn) {
            Toggle("Daily update", "Each morning: your runway and today's amount", s.notifyDaily) { on -> if (on) ask(); com.ruvo.data.RuvoGraph.appScope.launch { repo.setNotifications(daily = on) } }
            Toggle("New offers and books", "When a student offer or a Guide book is added", s.notifyNews) { on -> if (on) ask(); com.ruvo.data.RuvoGraph.appScope.launch { repo.setNotifications(news = on) } }
            if (smsAvailable) Toggle("Payments to confirm", "When Ruvo reads a new payment from a bank SMS", s.notifyCapture) { on -> if (on) ask(); com.ruvo.data.RuvoGraph.appScope.launch { repo.setNotifications(capture = on) } }
            Toggle("Weekly review", "A Sunday nudge to sort and review your week", s.notifyWeekly) { on -> if (on) ask(); com.ruvo.data.RuvoGraph.appScope.launch { repo.setNotifications(weekly = on) } }
            Toggle("Goal moments", "When a goal is reached or falls behind", s.notifyGoals) { on -> if (on) ask(); com.ruvo.data.RuvoGraph.appScope.launch { repo.setNotifications(goals = on) } }
        }

        SectionTitle("Runway")
        Toggle("Make it last until", "A Home card with a daily amount to reach a date you choose. Shows by itself when next money is over 45 days away.", s.allowanceOn) { on -> com.ruvo.data.RuvoGraph.appScope.launch { repo.setAllowanceOn(on) } }
        Spacer(Modifier.height(Space.sm))
        com.ruvo.feature.today.UsualDayEditor(s.dailyEstimate)
        Spacer(Modifier.height(Space.md))
        com.ruvo.feature.money.ExpectedMoneyCard(state)

        SectionTitle("Money view")
        Toggle("Hide my balance", "Shows •••• on Money instead of the amount", s.hideBalance) { on -> com.ruvo.data.RuvoGraph.appScope.launch { repo.setHideBalance(on) } }

        SectionTitle("Splits")
        RuvoField(vpa, { vpa = it }, "Your UPI ID, for payment links")
        Spacer(Modifier.height(Space.xs))
        GhostButton("Save UPI ID", { com.ruvo.data.RuvoGraph.appScope.launch { repo.setUpiVpa(vpa) } }, Modifier.fillMaxWidth())

        SectionTitle("Privacy")
        Text(
            "Everything stays on this phone, in an encrypted database. Your account and PIN live here too; there is no server, no ads and no analytics. " +
                "SMS are read here and never uploaded. Live answers, if you add your own key, send only totals and your runway, " +
                "never transactions or messages. Ruvo explains your own numbers; it does not give investment, tax or legal advice.",
            style = Ruvo.type.body, color = c.inkSoft,
        )
        Spacer(Modifier.height(Space.md))
        GhostButton("Export my data", {
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "My Ruvo data")
                putExtra(android.content.Intent.EXTRA_TEXT, repo.exportText())
            }
            context.startActivity(android.content.Intent.createChooser(send, "Export my Ruvo data"))
        }, Modifier.fillMaxWidth())
        Text("A plain-text copy of your transactions, goals and bills, made on this phone. You choose where it goes.", Modifier.padding(top = Space.xxs), style = Ruvo.type.caption, color = c.inkFaint)
        Spacer(Modifier.height(Space.md))
        if (!confirmClear) {
            GhostButton("Clear all transactions", { confirmClear = true }, Modifier.fillMaxWidth())
        } else {
            RuvoCard(color = c.warning.copy(alpha = 0.12f), border = null) {
                Text("Removes every transaction record. Your balance, goals and bills stay as they are.", style = Ruvo.type.body, color = c.ink)
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    GhostButton("Keep them", { confirmClear = false }, Modifier.weight(1f))
                    PrimaryButton("Clear", { com.ruvo.data.RuvoGraph.appScope.launch { repo.clearTransactions() }; confirmClear = false }, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(Space.md))
        if (!confirmDelete) {
            GhostButton("Delete everything", { confirmDelete = true }, Modifier.fillMaxWidth())
        } else {
            RuvoCard(color = c.warning.copy(alpha = 0.12f), border = null) {
                Text("This removes every transaction, goal and setting from this phone. It cannot be undone.", style = Ruvo.type.body, color = c.ink)
                Spacer(Modifier.height(Space.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    GhostButton("Keep my data", { confirmDelete = false }, Modifier.weight(1f))
                    PrimaryButton("Delete", { RuvoGraph.appScope.launch { repo.deleteEverything() }; onDismiss() }, Modifier.weight(1f))
                }
            }
        }
        SectionTitle("Help and support")
        // The welcome tour again: every tab, one part at a time.
        PrimaryButton("Replay the tour", { onReplayTour() }, Modifier.fillMaxWidth(), icon = com.ruvo.design.RuvoIcons.Help)
        Spacer(Modifier.height(Space.sm))
        listOf(
            "What is runway?" to "How many days your Fuel lasts at your usual daily spend. Next to it, Ruvo shows when your next money arrives and how many days spare you have.",
            "How do I change my balance?" to "Money → tap the You have card → type what you have in bank and cash → Save. Runway, Fuel, the buckets, the daily amount and the widget all update straight away.",
            "How do I split a bill?" to "Tap + → type the amount → switch on Shared with friends → add each friend → pick a category → Save split. Their part shows in Money → Owed → Owed to you, and They paid settles it.",
            "A friend paid for me. How do I track it?" to "Money → Owed → You owe → A friend paid for me → who, how much, what for → Save. Ruvo keeps that money aside in Fixed until you tap I paid them back.",
            "I spent money. How do I add it?" to "Tap + on Home or Money (or pull Ruvo's pet down) → type the amount → pick a category → Save. Received money works the same way with Received.",
            "Why does Ruvo not count my goal money?" to "Money in a goal is protected for its purpose. Leaving it out of Fuel means a goal can never quietly shrink without you deciding.",
            "Does Ruvo read my messages?" to "Only if you switch on Read payment SMS. Then it reads payment messages from the banks and card companies on its list, on this phone only, and never uploads them. Settings → Tracking → What Ruvo reads has the details.",
            "Can Ruvo move or hold my money?" to "No. Ruvo never holds, moves or invests money. It explains your own numbers.",
            "Will Ruvo stop me buying something?" to "Never. Before you buy shows what a purchase changes. The choice is always yours.",
        ).forEachIndexed { i, (q, a) ->
            val open = openFaq == i
            RuvoCard(Modifier.fillMaxWidth().padding(bottom = Space.xs), onClick = { openFaq = if (open) null else i }, onClickLabel = if (open) "Hide answer" else "Show answer", padding = androidx.compose.foundation.layout.PaddingValues(Space.md)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(q, Modifier.weight(1f), style = Ruvo.type.bodyStrong, color = c.ink)
                    Text(if (open) "–" else "+", style = Ruvo.type.title, color = c.inkSoft)
                }
                androidx.compose.animation.AnimatedVisibility(open) {
                    Text(a, Modifier.padding(top = Space.xs), style = Ruvo.type.body, color = c.inkSoft)
                }
            }
        }

        SectionTitle("About Ruvo")
        Text("Your money, without the mental load. Ruvo helps you see what you have, what you can spend, and what matters next.", style = Ruvo.type.body, color = c.inkSoft)
        Spacer(Modifier.height(Space.xs))
        Text("Ruvo 1.2 · made for students · portfolio build · Marketplace offers are checked by hand and may change", style = Ruvo.type.caption, color = c.inkFaint)
    }
}

@Composable
private fun Toggle(title: String, subtitle: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val c = Ruvo.colors
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Ruvo.type.bodyStrong, color = c.ink)
            Text(subtitle, style = Ruvo.type.caption, color = c.inkSoft)
        }
        Switch(
            on, onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = c.onBrand, checkedTrackColor = c.brand, checkedBorderColor = c.brand, uncheckedThumbColor = c.inkSoft, uncheckedTrackColor = c.hairline, uncheckedBorderColor = c.hairline),
        )
    }
}

/** A small preview of each mood, drawn with that mood's own colours. */
@Composable
internal fun MoodCard(name: String, line: String, palette: RuvoColors, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.inner)
    // Painted in the mood's own sky, with a pane of its glass and its brand blue, so you see it before you pick it.
    val card = RoundedCornerShape(Radius.card)
    androidx.compose.foundation.layout.Column(
        modifier.clip(card)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(palette.glowCool.compositeOver(palette.canvasTop), palette.canvas)))
            .border(if (selected) 2.dp else 1.dp, if (selected) c.ink else c.hairline, card)
            .clickable(role = androidx.compose.ui.semantics.Role.RadioButton, onClickLabel = "Use $name", onClick = onClick)
            .padding(Space.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(palette.fixed, palette.fuel, palette.future).forEach { col ->
                Box(Modifier.size(18.dp).clip(CircleShape).background(col))
            }
        }
        Spacer(Modifier.height(Space.sm))
        Box(Modifier.fillMaxWidth().height(28.dp).clip(shape).background(palette.glassTop).border(1.dp, palette.glassEdge, shape)) {
            Box(Modifier.fillMaxWidth(0.6f).height(28.dp).clip(shape).background(palette.brand.copy(alpha = 0.85f)))
        }
        Spacer(Modifier.height(Space.sm))
        Text(name, style = Ruvo.type.bodyStrong, color = palette.ink)
        Text(line, style = Ruvo.type.caption, color = palette.inkSoft)
    }
}

/** A labelled switch row: the whole row toggles, so the touch target is the full width. */
@Composable
internal fun SwitchLine(title: String, detail: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val c = Ruvo.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(on, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Ruvo.type.bodyStrong, color = c.ink)
            Text(detail, style = Ruvo.type.caption, color = c.inkSoft)
        }
        Spacer(Modifier.width(Space.xs))
        androidx.compose.material3.Switch(on, null)
    }
}

/**
 * "What Ruvo reads": the SMS trust line. Every sentence matches the code: SmsParser only accepts known bank and card
 * sender IDs (unless Demo SMS is on), the inbox scan skips other senders before reading the text, the scan covers 90
 * days once, and only the amount, date, payee and reference are kept, in the encrypted database. Nothing is uploaded.
 */
@Composable
private fun WhatRuvoReads(demoSms: Boolean) {
    val c = Ruvo.colors
    var open by remember { mutableStateOf(false) }
    RuvoCard(
        Modifier.fillMaxWidth().padding(vertical = Space.xs), onClick = { open = !open },
        onClickLabel = if (open) "Hide what Ruvo reads" else "Show what Ruvo reads",
        padding = androidx.compose.foundation.layout.PaddingValues(Space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("What Ruvo reads", Modifier.weight(1f), style = Ruvo.type.bodyStrong, color = c.ink)
            Text(if (open) "–" else "+", style = Ruvo.type.title, color = c.inkSoft)
        }
        androidx.compose.animation.AnimatedVisibility(open) {
            Column(Modifier.padding(top = Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                listOf(
                    "Only payment messages from banks and card companies on Ruvo's list. UPI alerts come from your bank, so they are included.",
                    "Messages from anyone else are skipped before their text is read.",
                    "When you switch this on, Ruvo looks back over the last 90 days once. After that, only new messages.",
                    "Everything is read on this phone. Ruvo keeps the amount, date, who was paid and the bank reference in its encrypted database, not the whole message.",
                    "Nothing is uploaded. You can switch this off any time.",
                ).forEach { Text("• $it", style = Ruvo.type.body, color = c.inkSoft) }
                if (demoSms) Text(
                    "Demo SMS is on, so test messages from any sender are accepted too. Turn it off when you use Ruvo with your real messages.",
                    style = Ruvo.type.bodyStrong, color = c.warning,
                )
            }
        }
    }
}
