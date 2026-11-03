package com.ruvo.feature.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ruvo.data.OnboardingAnswers
import com.ruvo.data.RuvoGraph
import com.ruvo.data.sms.SmsInboxScanner
import com.ruvo.design.AccentButton
import com.ruvo.design.Chip
import com.ruvo.design.Eyebrow
import com.ruvo.design.GhostButton
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Radius
import com.ruvo.design.RollingNumber
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.design.parseRupees
import com.ruvo.engine.Cadence
import com.ruvo.engine.Commitment
import com.ruvo.engine.IncomePlan
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.RunwayEngine
import com.ruvo.engine.RunwayInput
import com.ruvo.engine.demo.DemoPersonas
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

private data class Promise(val name: String, val rupees: Int, val day: Int)

private val PROMISES = listOf(
    Promise("Rent or PG", 6_500, 5), Promise("Hostel mess", 3_200, 5), Promise("Phone recharge", 299, 20),
    Promise("Electricity share", 700, 15), Promise("Subscriptions", 199, 12), Promise("Bus or metro pass", 600, 1),
)

/**
 * The 90-second first run: how money reaches you, what you have, when the next money comes, what is
 * already promised, then the reveal. SMS permission is asked only after the reveal, once the value is clear.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingFlow(onDone: () -> Unit) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var adult by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var cadence by remember { mutableStateOf(Cadence.MONTHLY) }
    var balance by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var nextIn by remember { mutableIntStateOf(-1) }
    val chosen = remember { mutableStateListOf<Promise>() }
    var personaPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val context = LocalContext.current

    fun nextDate(): LocalDate? = when {
        cadence == Cadence.IRREGULAR -> null
        nextIn > 0 -> today.plusDays(nextIn.toLong())
        else -> DemoPersonas.nextDayOfMonth(today, 1)
    }
    fun answers() = OnboardingAnswers(
        name = name, cadence = cadence, balance = parseRupees(balance) ?: 0, expectedIncome = parseRupees(expected) ?: 0,
        nextIncome = nextDate(),
        commitments = chosen.map { Commitment(UUID.randomUUID().toString(), it.name, it.rupees * 100L, dueDayOfMonth = it.day) },
    )

    Column(Modifier.fillMaxSize().background(c.canvas).statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (step in 1..4) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.md), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..4).forEach { i ->
                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(Radius.chip)).background(if (i <= step) c.ink else c.hairline))
                }
            }
        }
        AnimatedContent(
            targetState = step, modifier = Modifier.weight(1f),
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally { w -> if (fwd) w / 3 else -w / 3 } + fadeIn()) togetherWith (slideOutHorizontally { w -> if (fwd) -w / 3 else w / 3 } + fadeOut())
            },
            label = "step",
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter, vertical = Space.lg)) {
                when (s) {
                    0 -> Welcome(adult, { adult = it })
                    1 -> {
                        Title("How does money reach you?", "Your rhythm changes the maths.")
                        listOf(
                            Cadence.MONTHLY to ("Every month" to "Pocket money, stipend or salary"),
                            Cadence.LUMP_SUM to ("One big amount" to "A scholarship or a semester's money"),
                            Cadence.IRREGULAR to ("It varies" to "Freelance, gigs, part-time shifts"),
                        ).forEach { (cd, label) ->
                            OptionCard(label.first, label.second, cadence == cd) { cadence = cd }
                        }
                        Spacer(Modifier.height(Space.md))
                        RuvoField(name, { name = it }, "What should Ruvo call you? (optional)")
                    }
                    2 -> {
                        Title("How much do you have right now?", "Bank plus cash. A rough number is fine.")
                        RuvoField(balance, { balance = it }, "Right now", number = true, prefix = "₹ ")
                    }
                    3 -> {
                        if (cadence == Cadence.IRREGULAR) {
                            Title("No fixed payday. That is fine.", "Ruvo will show how long your money lasts at your pace, rather than a date to reach.")
                        } else {
                            Title("When does the next money arrive?", "So Ruvo knows the finish line.")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                                val first = DemoPersonas.nextDayOfMonth(today, 1)
                                Chip("On the 1st (${IndianFormat.date(first)})", nextIn <= 0, { nextIn = -1 })
                                listOf(7, 15, 30, 60, 120).forEach { d -> Chip("In $d days", nextIn == d, { nextIn = d }) }
                            }
                            Spacer(Modifier.height(Space.md))
                            RuvoField(expected, { expected = it }, "Usually how much? (optional)", number = true, prefix = "₹ ")
                        }
                    }
                    4 -> {
                        Title("What is already promised?", "Ruvo sets these aside before showing what is free.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                            PROMISES.forEach { p ->
                                val on = p in chosen
                                Chip("${p.name} ${IndianFormat.rupees(p.rupees * 100L)}", on, { if (on) chosen.remove(p) else chosen.add(p) }, accent = c.fixed)
                            }
                        }
                        Text("You can change amounts later in Money.", Modifier.padding(top = Space.sm), style = Ruvo.type.caption, color = c.inkFaint)
                    }
                    5 -> Reveal(answers(), today)
                    else -> Tracking(onFinish = { withSms ->
                        scope.launch {
                            // Save the answers first; the inbox scan then adds to the fresh data.
                            RuvoGraph.repository.completeOnboarding(answers())
                            if (withSms) {
                                RuvoGraph.repository.setRealSms(true)
                                SmsInboxScanner.scan(context)
                            }
                            onDone()
                        }
                    })
                }
            }
        }
        Column(Modifier.padding(Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            when (step) {
                0 -> {
                    PrimaryButton("Start with my money", { step = 1 }, Modifier.fillMaxWidth(), enabled = adult)
                    GhostButton("Explore with a sample student", { personaPicker = true }, Modifier.fillMaxWidth())
                }
                in 1..4 -> Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    GhostButton("Back", { step-- }, Modifier.weight(1f))
                    PrimaryButton(if (step == 4) "Show my runway" else "Next", { step++ }, Modifier.weight(2f), enabled = step != 2 || (parseRupees(balance) ?: 0) > 0)
                }
                5 -> PrimaryButton("Continue", { step = 6 }, Modifier.fillMaxWidth())
                else -> {}
            }
        }
    }
    if (personaPicker) PersonaPicker(onDismiss = { personaPicker = false }) { personaPicker = false; onDone() }
}

@Composable
private fun Title(title: String, subtitle: String) {
    Text(title, Modifier.semantics { heading() }, style = Ruvo.type.display, color = Ruvo.colors.ink)
    Spacer(Modifier.height(Space.xs))
    Text(subtitle, style = Ruvo.type.body, color = Ruvo.colors.inkSoft)
    Spacer(Modifier.height(Space.xl))
}

@Composable
private fun OptionCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val c = Ruvo.colors
    RuvoCard(
        Modifier.fillMaxWidth().padding(bottom = Space.sm),
        color = if (selected) c.ink else c.surface, border = if (selected) null else c.hairline, onClick = onClick,
    ) {
        Text(title, style = Ruvo.type.title, color = if (selected) c.canvas else c.ink)
        Text(subtitle, style = Ruvo.type.caption, color = if (selected) c.canvas.copy(alpha = 0.7f) else c.inkSoft)
    }
}

@Composable
private fun Welcome(adult: Boolean, setAdult: (Boolean) -> Unit) {
    val c = Ruvo.colors
    Spacer(Modifier.height(Space.xxl))
    Eyebrow("Ruvo")
    Text("See how many days your money lasts.", Modifier.semantics { heading() }, style = Ruvo.type.display, color = c.ink)
    Spacer(Modifier.height(Space.md))
    Text(
        "Not rupees. Days. Ruvo sets aside what is promised, protects what you are saving, and shows how long the rest will carry you. Everything stays on this phone.",
        style = Ruvo.type.body, color = c.inkSoft,
    )
    Spacer(Modifier.height(Space.xl))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.inner)).clickable(role = Role.Checkbox) { setAdult(!adult) }.padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(adult, setAdult, colors = CheckboxDefaults.colors(checkedColor = c.ink, uncheckedColor = c.inkSoft, checkmarkColor = c.canvas))
        Text("I am 18 or older", style = Ruvo.type.bodyStrong, color = c.ink)
    }
}

@Composable
private fun Reveal(a: OnboardingAnswers, today: LocalDate) {
    val c = Ruvo.colors
    val input = RunwayInput(today, a.balance, IncomePlan(a.cadence, a.expectedIncome, a.nextIncome), a.commitments)
    val r = RunwayEngine.calculate(input)
    val count = remember { Animatable(0f) }
    LaunchedEffect(r.runwayDays) { count.animateTo(r.runwayDays.toFloat(), tween(1400)) }
    Spacer(Modifier.height(Space.xxl))
    Eyebrow("Your runway")
    Row(verticalAlignment = Alignment.Bottom) {
        RollingNumber(count.value.toInt(), Ruvo.type.hero, c.ink, spoken = IndianFormat.days(r.runwayDays))
        Text(" days", Modifier.padding(bottom = 18.dp), style = Ruvo.type.display, color = c.ink)
    }
    val line = when {
        r.daysToNextIncome == null -> "At an even pace, your money carries you ${IndianFormat.days(r.runwayDays)}."
        r.makesIt -> "You make it to your next money with ${IndianFormat.days(r.spareDays)} spare."
        else -> "That is ${IndianFormat.days(-(r.bufferDays ?: 0))} short of your next money. ${IndianFormat.rupees(r.safeDaily)} a day closes the gap."
    }
    Text(line, style = Ruvo.type.title, color = if (r.makesIt) c.ink else c.warning)
    Spacer(Modifier.height(Space.md))
    Text(
        "${IndianFormat.rupees(r.fixedReserved)} is set aside for what is promised. As you spend, Ruvo learns your real pace and the number sharpens.",
        style = Ruvo.type.body, color = c.inkSoft,
    )
}

@Composable
private fun Tracking(onFinish: (withSms: Boolean) -> Unit) {
    val c = Ruvo.colors
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        onFinish(granted[Manifest.permission.READ_SMS] == true)
    }
    Spacer(Modifier.height(Space.xxl))
    Eyebrow("Optional")
    Text("Let Ruvo read payment SMS?", Modifier.semantics { heading() }, style = Ruvo.type.display, color = c.ink)
    Spacer(Modifier.height(Space.md))
    listOf(
        "Only messages from bank and UPI sender IDs are read. Personal messages are skipped before they are opened.",
        "Everything is read on this phone. No message ever leaves it.",
        "Detected payments wait in your weekly sort, so you stay in control.",
    ).forEach { line ->
        Row(Modifier.padding(vertical = Space.xxs)) {
            Box(Modifier.padding(top = 9.dp).size(6.dp).clip(RoundedCornerShape(3.dp)).background(c.fuel))
            Spacer(Modifier.size(Space.sm))
            Text(line, style = Ruvo.type.body, color = c.ink)
        }
    }
    Spacer(Modifier.height(Space.xl))
    AccentButton("Turn on tracking", {
        val perms = buildList {
            add(Manifest.permission.READ_SMS); add(Manifest.permission.RECEIVE_SMS)
            if (android.os.Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(perms.toTypedArray())
    }, Modifier.fillMaxWidth())
    Spacer(Modifier.height(Space.sm))
    GhostButton("Not now", { onFinish(false) }, Modifier.fillMaxWidth())
    Text("You can add spends by pulling the Orb down, any time.", Modifier.padding(top = Space.sm), style = Ruvo.type.caption, color = c.inkFaint)
}

/** Choose one of the five sample students. Replaces whatever is on the phone with sample data. */
@Composable
fun PersonaPicker(onDismiss: () -> Unit, onPicked: () -> Unit) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    RuvoSheet("Pick a sample student", onDismiss, eyebrow = "Demo") {
        Text("Sample data only. None of it is real.", style = Ruvo.type.caption, color = c.inkFaint)
        Spacer(Modifier.height(Space.sm))
        DemoPersonas.all(today).forEach { p ->
            val r = RunwayEngine.calculate(p.input)
            RuvoCard(Modifier.fillMaxWidth().padding(bottom = Space.sm), onClick = {
                scope.launch { RuvoGraph.repository.loadPersona(p.id); onPicked() }
            }, onClickLabel = "Use ${p.name}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${p.name}, ${p.age} · ${p.livesIn}", style = Ruvo.type.title, color = c.ink)
                        Text(p.story, style = Ruvo.type.caption, color = c.inkSoft)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (r.unlimited) "∞" else r.runwayDays.toString(), style = Ruvo.type.display, color = if (r.makesIt) c.ink else c.warning)
                        Text("days", style = Ruvo.type.caption, color = c.inkFaint)
                    }
                }
            }
        }
    }
}
