package com.ruvo.feature.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ruvo.data.OnboardingAnswers
import com.ruvo.data.RuvoGraph
import com.ruvo.data.sms.SmsInboxScanner
import com.ruvo.design.AccentButton
import com.ruvo.design.AmbientBackdrop
import com.ruvo.design.Motion
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
    var goalName by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("") }
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

    AmbientBackdrop {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        if (step in 1..5) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.md), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { i ->
                    val fill by animateColorAsState(if (i <= step) c.fuel else c.ink.copy(alpha = 0.1f), label = "p$i")
                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(Radius.chip)).background(fill))
                }
            }
        }
        AnimatedContent(
            targetState = step, modifier = Modifier.weight(1f),
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(Motion.gentle()) { w -> if (fwd) w / 3 else -w / 3 } + fadeIn(Motion.gentle())) togetherWith (slideOutHorizontally(Motion.soft()) { w -> if (fwd) -w / 3 else w / 3 } + fadeOut(Motion.soft()))
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
                    5 -> {
                        Title("Do you want to save for a goal?", "Money with a name is harder to spend by accident. You can skip this.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                            listOf("Trip with friends", "Laptop", "Emergency cushion", "Course fees", "Phone").forEach { g -> Chip(g, goalName == g, { goalName = g }) }
                        }
                        Spacer(Modifier.height(Space.md))
                        RuvoField(goalName, { goalName = it }, "Goal name")
                        Spacer(Modifier.height(Space.sm))
                        RuvoField(goalTarget, { goalTarget = it }, "How much do you need?", number = true, prefix = "₹ ")
                        Text("Ruvo never counts goal money as spendable.", Modifier.padding(top = Space.sm), style = Ruvo.type.caption, color = c.inkFaint)
                    }
                    6 -> Reveal(answers(), today)
                    else -> Tracking(onFinish = { withSms ->
                        scope.launch {
                            // Save the answers first; the inbox scan then adds to the fresh data.
                            RuvoGraph.repository.completeOnboarding(answers())
                            val target = parseRupees(goalTarget) ?: 0
                            if (target > 0) RuvoGraph.repository.addGoal(goalName.ifBlank { "My goal" }, target, today.plusMonths(6), target / 6)
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
                    PrimaryButton("Next", { step++ }, Modifier.weight(2f), enabled = step != 2 || (parseRupees(balance) ?: 0) > 0)
                }
                5 -> Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    GhostButton("Not right now", { goalTarget = ""; step = 6 }, Modifier.weight(1f))
                    PrimaryButton("Show my runway", { step = 6 }, Modifier.weight(1.4f))
                }
                6 -> PrimaryButton("Continue", { step = 7 }, Modifier.fillMaxWidth())
                else -> {}
            }
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
        brush = if (selected) (if (c.isDark) c.amberBrush else c.heroBrush) else null,
        border = if (selected) null else androidx.compose.ui.graphics.Color.Unspecified, elevated = selected, onClick = onClick,
    ) {
        val on = if (c.isDark) c.onAccent else c.onHero
        Text(title, style = Ruvo.type.title, color = if (selected) on else c.ink)
        Text(subtitle, style = Ruvo.type.caption, color = if (selected) on.copy(alpha = 0.75f) else c.inkSoft)
    }
}

@Composable
private fun Welcome(adult: Boolean, setAdult: (Boolean) -> Unit) {
    val c = Ruvo.colors
    val pager = rememberPagerState { 4 }
    Spacer(Modifier.height(Space.md))
    Text("ruvo.", style = Ruvo.type.headline, color = c.ink)
    Spacer(Modifier.height(Space.md))
    HorizontalPager(pager, Modifier.fillMaxWidth().height(250.dp), pageSpacing = Space.md) { page ->
        val o = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - kotlin.math.abs(o) * 0.5f; val s = 1f - kotlin.math.abs(o) * 0.08f; scaleX = s; scaleY = s }) {
            when (page) {
                0 -> SlideOrb()
                1 -> SlideJobs(pager.currentPage == 1)
                2 -> SlideRunway()
                else -> SlideGoal()
            }
        }
    }
    Spacer(Modifier.height(Space.md))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(4) { i ->
            val w by animateDpAsState(if (i == pager.currentPage) 22.dp else 7.dp, Motion.snappy(), label = "dot$i")
            Box(Modifier.padding(horizontal = 3.dp).height(7.dp).width(w).clip(RoundedCornerShape(Radius.chip)).background(if (i == pager.currentPage) c.fuel else c.ink.copy(alpha = 0.15f)))
        }
    }
    Spacer(Modifier.height(Space.lg))
    val (title, line) = listOf(
        "Your money, without the mental load." to "See what you have, what you can spend, and what matters next.",
        "Give every rupee a job." to "Fixed, Fuel and Future. Know what is safe to spend.",
        "Know how many days you can last." to "Not rupees. Days. Your runway, in plain words.",
        "Save for what matters. Skip the guilt." to "Protect your goals and decide with calm guidance.",
    )[pager.currentPage]
    AnimatedContent(title, transitionSpec = { fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.soft()) }, label = "welcomeTitle") { t ->
        Text(t, Modifier.semantics { heading() }, style = Ruvo.type.display, color = c.ink)
    }
    Spacer(Modifier.height(Space.xs))
    Text("$line Everything stays on this phone.", style = Ruvo.type.body, color = c.inkSoft)
    Spacer(Modifier.height(Space.lg))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.inner)).clickable(role = Role.Checkbox) { setAdult(!adult) }.padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(adult, setAdult, colors = CheckboxDefaults.colors(checkedColor = c.fuel, uncheckedColor = c.inkSoft, checkmarkColor = c.onAccent))
        Text("I am 18 or older", style = Ruvo.type.bodyStrong, color = c.ink)
    }
}

/** Slide 1: Ruvo's pet, happy to meet you, on amber. */
@Composable
private fun SlideOrb() {
    val c = Ruvo.colors
    val t = rememberInfiniteTransition(label = "orbSlide")
    val bob by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(2800), RepeatMode.Reverse), label = "bob")
    val reduced = Ruvo.reducedMotion
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.card)).background(c.amberBrush), contentAlignment = Alignment.Center) {
        com.ruvo.feature.orb.RuvoPet(com.ruvo.feature.orb.PetMood.HAPPY, Modifier.size(190.dp).graphicsLayer { translationY = if (reduced) 0f else bob * density * 0.5f })
    }
}

/** Slide 2: three tilted bucket pills that pop in one after another. */
@Composable
private fun SlideJobs(active: Boolean) {
    val c = Ruvo.colors
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.card)).background(c.glass).padding(Space.md), verticalArrangement = Arrangement.Center) {
        listOf(Triple("Fixed", c.bucketBrush(com.ruvo.engine.Bucket.FIXED), -2f), Triple("Fuel", c.amberBrush, 2f), Triple("Future", c.bucketBrush(com.ruvo.engine.Bucket.FUTURE), -2f))
            .forEachIndexed { i, (label, brush, rot) ->
                val pop = remember { Animatable(0f) }
                LaunchedEffect(active) { if (active) { pop.snapTo(0f); kotlinx.coroutines.delay(i * 120L); pop.animateTo(1f, Motion.bouncy()) } }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp).graphicsLayer { rotationZ = rot; scaleX = 0.6f + 0.4f * pop.value; scaleY = 0.6f + 0.4f * pop.value; alpha = pop.value }
                        .clip(RoundedCornerShape(Radius.chip)).background(brush).padding(horizontal = Space.lg, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, Modifier.weight(1f), style = Ruvo.type.title, color = c.onAccent)
                    Text(listOf("Already planned", "Yours to spend", "Protected")[i], style = Ruvo.type.label, color = c.onAccent.copy(alpha = 0.75f))
                }
            }
    }
}

/** Slide 3: a runway card with day pills. */
@Composable
private fun SlideRunway() {
    val c = Ruvo.colors
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.card)).background(c.heroBrush).padding(Space.lg)) {
        Eyebrow("Runway", color = c.onHero.copy(alpha = 0.7f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("23", style = Ruvo.type.hero.copy(fontSize = Ruvo.type.hero.fontSize * 0.7f, lineHeight = Ruvo.type.hero.lineHeight * 0.7f), color = c.onHero)
            Text(" days", Modifier.padding(bottom = 10.dp), style = Ruvo.type.title, color = c.onHero)
        }
        Spacer(Modifier.weight(1f))
        com.ruvo.design.PillBars(listOf(40, 62, 35, 80, 55, 90, 48).mapIndexed { i, h -> com.ruvo.design.Pill(listOf("M", "T", "W", "T", "F", "S", "S")[i], h.toFloat()) }, height = 70.dp)
    }
}

/** Slide 4: a goal filling up. */
@Composable
private fun SlideGoal() {
    val c = Ruvo.colors
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.card)).background(c.bucketBrush(com.ruvo.engine.Bucket.FIXED)).padding(Space.lg), verticalArrangement = Arrangement.Center) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.card)).background(c.surface).padding(Space.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Goa trip", Modifier.weight(1f), style = Ruvo.type.title, color = c.ink)
                com.ruvo.design.Tag("50%")
            }
            Spacer(Modifier.height(Space.md))
            com.ruvo.design.FillBar(0.5f, c.future, height = 12.dp)
            Spacer(Modifier.height(Space.sm))
            Text("₹4,000 of ₹8,000 · protected", style = Ruvo.type.caption, color = c.inkSoft)
        }
    }
}
@Composable
private fun Reveal(a: OnboardingAnswers, today: LocalDate) {
    val c = Ruvo.colors
    val input = RunwayInput(today, a.balance, IncomePlan(a.cadence, a.expectedIncome, a.nextIncome), a.commitments)
    val r = RunwayEngine.calculate(input)
    val count = remember { Animatable(0f) }
    LaunchedEffect(r.runwayDays) { count.animateTo(r.runwayDays.toFloat(), tween(1400)) }
    Spacer(Modifier.height(Space.xl))
    RuvoCard(Modifier.fillMaxWidth(), brush = c.heroBrush, border = null, elevated = true, padding = androidx.compose.foundation.layout.PaddingValues(Space.xl)) {
        Eyebrow("Your runway", color = c.onHero.copy(alpha = 0.7f))
        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumber(count.value.toInt(), Ruvo.type.hero, c.onHero, spoken = IndianFormat.days(r.runwayDays))
            Text(" days", Modifier.padding(bottom = 16.dp), style = Ruvo.type.display, color = c.onHero, maxLines = 1, softWrap = false)
        }
    }
    Spacer(Modifier.height(Space.lg))
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
    Text("Add spends any time with the + button, or by pulling Ruvo's pet down.", Modifier.padding(top = Space.sm), style = Ruvo.type.caption, color = c.inkFaint)
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
