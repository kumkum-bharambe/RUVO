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
        val shares = if (!incoming && sharing) owed.filter { it.second > 0 } else emptyList()
        // A short "Saved ₹250 · runway 24 → 23 days" line confirms it (shown by the app shell, read out by TalkBack).
        com.ruvo.data.Notices.post(com.ruvo.data.Notices.Notice.Saved(amountNow, incoming, RuvoGraph.repository.state.value.runway.runwayDays))
        RuvoGraph.appScope.launch {
            val id = RuvoGraph.repository.addManual(
                amountNow, label, if (incoming) "Income" else category,
                if (incoming) Bucket.FUEL else bucketNow, if (incoming) Direction.CREDIT else Direction.DEBIT,
            )
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
            PrimaryButton("Add ${if (paise > 0) IndianFormat.rupees(paise) else ""}", { save("Income") }, Modifier.fillMaxWidth(), enabled = paise > 0)
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
            if (sharing && paise > 0) {
                Spacer(Modifier.height(Space.xs))
                com.ruvo.design.SplitEditor(paise, emptyList()) { owed = it }
            }
            Spacer(Modifier.height(Space.md))
            Eyebrow("Tap a category to save")
            Spacer(Modifier.height(Space.xs))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                (quick + state.settings.customCategories.sorted()).forEach { cat -> Chip(cat, false, { save(cat) }) }
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
        }
    }
}

/** Settings: the theme switch with both moods, data and SMS controls, privacy, and delete everything. */
@Composable
fun SettingsSheet(state: AppState, onDismiss: () -> Unit, onPersonas: () -> Unit) {
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
        Eyebrow("Mood")
        Spacer(Modifier.height(Space.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            MoodCard("Daybreak", "Soft light, amber dawn glow", Daybreak, s.theme == ThemeChoice.DAYBREAK.name, Modifier.weight(1f)) {
                scope.launch { repo.setTheme(ThemeChoice.DAYBREAK.name) }
            }
            MoodCard("Night swim", "True black, quiet glow", NightSwim, s.theme == ThemeChoice.NIGHT_SWIM.name, Modifier.weight(1f)) {
                scope.launch { repo.setTheme(ThemeChoice.NIGHT_SWIM.name) }
            }
        }
        Spacer(Modifier.height(Space.xs))
        Chip("Follow my phone", s.theme == ThemeChoice.SYSTEM.name, { scope.launch { repo.setTheme(ThemeChoice.SYSTEM.name) } })

        if (!state.isDemo) {
            SectionTitle("Your name")
            RuvoField(name, { name = it }, "What should Ruvo call you?")
            Spacer(Modifier.height(Space.xs))
            GhostButton("Save name", { scope.launch { repo.setName(name) } }, Modifier.fillMaxWidth())
        }

        SectionTitle("Your data")
        if (state.isDemo) {
            Text("Showing sample student ${state.persona?.name}. None of it is real.", style = Ruvo.type.body, color = c.inkSoft)
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                GhostButton("Switch", onPersonas, Modifier.weight(1f))
                PrimaryButton("Use my own", { scope.launch { repo.deleteEverything() } }, Modifier.weight(1f))
            }
        } else {
            GhostButton("Explore a sample student", onPersonas, Modifier.fillMaxWidth())
        }

        SectionTitle("Tracking")
        val smsAvailable = remember { com.ruvo.data.sms.SmsSupport.available(context) }
        if (!smsAvailable) {
            Text(
                "This shared version does not read SMS. Sample students show sample bank messages, and you can add spends with + or by pulling Ruvo's pet down.",
                style = Ruvo.type.caption, color = c.inkSoft,
            )
        }
        if (smsAvailable) Toggle("Read payment SMS", "Bank and UPI senders only, read on this phone", s.realSms) { on ->
            if (on) {
                val perms = buildList {
                    add(Manifest.permission.READ_SMS); add(Manifest.permission.RECEIVE_SMS)
                    if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                smsLauncher.launch(perms.toTypedArray())
            } else scope.launch { repo.setRealSms(false) }
        }
        scanned?.let { Text("Found $it new payments in the last 90 days. They are waiting in your weekly sort.", style = Ruvo.type.caption, color = c.fuel) }
        if (smsAvailable) Toggle("Demo SMS", "Accept test messages from any sender, for emulator demos", s.demoSms) { on -> scope.launch { repo.setDemoSms(on) } }
        Toggle("Spoken replies", "Ruvo reads answers aloud in your chat language", s.spokenReplies) { on -> scope.launch { repo.setSpokenReplies(on) } }

        SectionTitle("Notifications")
        Text("Calm and occasional. Each one can be switched off.", style = Ruvo.type.caption, color = c.inkSoft)
        Toggle("New payments found", "When an SMS payment is tracked", s.notifyCapture) { on -> scope.launch { repo.setNotifications(capture = on) } }
        Toggle("Weekly review", "A Sunday nudge to sort and review your week", s.notifyWeekly) { on -> scope.launch { repo.setNotifications(weekly = on) } }
        Toggle("Goal moments", "When a goal is reached or falls behind", s.notifyGoals) { on -> scope.launch { repo.setNotifications(goals = on) } }

        SectionTitle("Money view")
        Toggle("Hide my balance", "Shows •••• on Money instead of the amount", s.hideBalance) { on -> scope.launch { repo.setHideBalance(on) } }

        SectionTitle("Splits")
        RuvoField(vpa, { vpa = it }, "Your UPI ID, for payment links")
        Spacer(Modifier.height(Space.xs))
        GhostButton("Save UPI ID", { scope.launch { repo.setUpiVpa(vpa) } }, Modifier.fillMaxWidth())

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
                    PrimaryButton("Clear", { scope.launch { repo.clearTransactions() }; confirmClear = false }, Modifier.weight(1f))
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
        listOf(
            "What is runway?" to "How many days your Fuel lasts at your usual daily spend. Next to it, Ruvo shows when your next money arrives and how many days spare you have.",
            "Why does Ruvo not count my goal money?" to "Money in a goal is protected for its purpose. Leaving it out of Fuel means a goal can never quietly shrink without you deciding.",
            "Does Ruvo read my messages?" to "Only if you switch on Read payment SMS. Then it reads bank and UPI senders on this phone only, and never uploads them.",
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
        Text("Ruvo 1.0 · made for students · portfolio build · Marketplace offers are checked by hand and may change", style = Ruvo.type.caption, color = c.inkFaint)
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
private fun MoodCard(name: String, line: String, palette: RuvoColors, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
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
