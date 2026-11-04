package com.ruvo.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.ActivityItem
import com.ruvo.data.AppState
import com.ruvo.data.RuvoGraph
import com.ruvo.design.BucketCard
import com.ruvo.design.Chip
import com.ruvo.design.Pill
import com.ruvo.design.PillBars
import com.ruvo.design.Segment
import com.ruvo.design.SplitBar
import com.ruvo.design.StatusChips
import com.ruvo.design.StatusItem
import com.ruvo.design.reveal
import com.ruvo.design.Eyebrow
import com.ruvo.design.IconCircle
import com.ruvo.design.MerchantLogo
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoIcons
import com.ruvo.design.SectionTitle
import com.ruvo.design.Space
import com.ruvo.design.ListRow
import com.ruvo.design.Amount
import com.ruvo.engine.Bucket
import com.ruvo.engine.Direction
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Insights
import com.ruvo.engine.guide.Lessons
import com.ruvo.engine.guide.Perks
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/** What Today can ask the app shell to open. */
data class TodayActions(
    val openWhy: () -> Unit,
    val openSort: () -> Unit,
    val openBuy: () -> Unit,
    val openSettings: () -> Unit,
    val openMoney: () -> Unit,
    val openGoals: () -> Unit,
    val openPersonas: () -> Unit,
    val openTxn: (String) -> Unit,
    val openLesson: (String) -> Unit,
    val openWeekly: () -> Unit = {},
    val openMonthly: () -> Unit = {},
    val openChat: (String?) -> Unit = {},
    val openGuide: () -> Unit = {},
    val openAdd: () -> Unit = {},
    val openProfile: () -> Unit = openSettings,
)

/** Your profile: a round initial, the one place besides the Orb that is round, because it is a person. */
@Composable
fun Avatar(name: String, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val c = Ruvo.colors
    Box(
        Modifier.size(size).clip(CircleShape).background(c.ink)
            .clickable(role = Role.Button, onClickLabel = "Open your profile", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.firstOrNull { it.isLetter() }?.uppercase() ?: "R", style = Ruvo.type.title, color = c.canvas)
    }
}

@Composable
fun TodayScreen(actions: TodayActions) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    TodayContent(state, actions)
}

@Composable
fun TodayContent(state: AppState, a: TodayActions) {
    val c = Ruvo.colors
    val r = state.runway
    var insightHidden by rememberSaveable { mutableStateOf(false) }
    val week = remember(state.input.transactions, state.today) { Insights.week(state.input.transactions, state.today) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Space.dockClearance),
    ) {
        item { Header(state, a) }
        // Ruvo 6's black status chips: the four numbers that matter, each one tap from its detail.
        item {
            val r0 = state.runway
            val upcoming = comingUp(state).sumOf { it.first.amount }
            StatusChips(
                listOf(
                    StatusItem(if (r0.unlimited) "No end · runway" else "${IndianFormat.days(r0.runwayDays)} runway", c.fuel, a.openWhy),
                    StatusItem("${IndianFormat.rupees(r0.fuel.coerceAtLeast(0))} available", c.fuel, a.openMoney),
                    StatusItem("${IndianFormat.rupees(r0.futureProtected)} protected", c.future, a.openGoals),
                    StatusItem("${IndianFormat.rupees(upcoming)} coming up", c.fixed, a.openMoney),
                ),
                Modifier.reveal(0),
            )
            Spacer(Modifier.height(Space.xs))
        }
        item {
            RunwayHero(r, a.openWhy, Modifier.padding(horizontal = Space.gutter).reveal(0))
        }
        item {
            Row(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm).reveal(2), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                val promised = r.reservedCommitments.size
                val total = (r.fixedReserved + r.fuel.coerceAtLeast(0) + r.futureProtected).coerceAtLeast(1).toFloat()
                BucketCard(Bucket.FIXED, r.fixedReserved, if (promised == 1) "1 planned" else "$promised planned", Modifier.weight(1f), r.fixedReserved / total, a.openMoney)
                BucketCard(Bucket.FUEL, r.fuel.coerceAtLeast(0), "To spend", Modifier.weight(1f), r.fuel.coerceAtLeast(0) / total, a.openWhy)
                val goals = state.goals.size
                BucketCard(Bucket.FUTURE, r.futureProtected, if (goals == 1) "1 goal" else "$goals goals", Modifier.weight(1f), r.futureProtected / total, a.openGoals)
            }
        }
        if (state.toSort.isNotEmpty()) {
            item { SortCard(state.toSort.size, a.openSort, Modifier.reveal(3)) }
        }
        // Ruvo noticed: the biggest change this week, explained calmly. Dismissable for the session.
        val change = week.categories.firstOrNull { (it.changePercent ?: 0) >= 15 && it.thisPeriod >= 30_000 }
        if (change != null && !insightHidden) {
            item { InsightCard(change, onWhy = { a.openChat("Where did most of my money go?") }, onHide = { insightHidden = true }, modifier = Modifier.reveal(4)) }
        }
        item { DailySpendingCard(state, Modifier.reveal(5)) }
        val upcoming = comingUp(state)
        if (upcoming.isNotEmpty()) {
            item { SectionTitle("Coming up", Modifier.padding(horizontal = Space.gutter), action = "See all", onAction = a.openMoney) }
            item {
                Row(Modifier.padding(horizontal = Space.gutter).reveal(7), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    upcoming.take(2).forEach { (cm, date) ->
                        RuvoCard(Modifier.weight(1f), padding = PaddingValues(Space.md)) {
                            Eyebrow(IndianFormat.date(date, state.today), color = c.fixedInk)
                            Spacer(Modifier.height(Space.xxs))
                            Text(cm.name, style = Ruvo.type.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Amount(cm.amount, style = Ruvo.type.title)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = Space.gutter).padding(top = Space.md).reveal(8), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                // One review: this week and this month on one sheet.
                ReviewTile("Your review", "This week and this month", RuvoIcons.Chart, Modifier.weight(1f), a.openWeekly)
            }
        }
        item { BuyCard(a.openBuy, Modifier.reveal(9)) }
        item {
            val lesson = Lessons.forNow(state.input, r)
            RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm).reveal(10), onClick = { a.openLesson(lesson.id) }, onClickLabel = "Read ${lesson.title}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(RuvoIcons.Book, null, Modifier.size(20.dp), tint = c.brandInk)
                    Spacer(Modifier.width(Space.xs))
                    Eyebrow("A small idea", color = c.brandInk)
                    Spacer(Modifier.weight(1f))
                    Text("5 min read", style = Ruvo.type.caption, color = c.inkFaint)
                }
                Spacer(Modifier.height(Space.xs))
                Text(lesson.title, style = Ruvo.type.title, color = c.ink)
                Text(lesson.body, style = Ruvo.type.body, color = c.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Space.xs))
                Text("Open the book ›", style = Ruvo.type.label, color = c.brandInk)
            }
        }
        // An occasional perk card: one a day, rotating, with its saving in days.
        item {
            // Only listings that are current and recently checked; never one that has ended or gone stale.
            val current = Perks.listed(state.today).filter { Perks.status(it, state.today).state.let { s -> s == Perks.State.ACTIVE || s == Perks.State.ENDING } }
            val perk = current.getOrNull(state.today.dayOfYear % current.size.coerceAtLeast(1)) ?: return@item
            val days = Perks.daysSaved(perk, r.dailySpend)
            if (days > 0) {
                RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm).reveal(11), brush = c.bucketBrush(Bucket.FIXED), border = null, onClick = a.openGuide, onClickLabel = "See the marketplace") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(RuvoIcons.Perk, null, Modifier.size(20.dp), tint = c.onAccent)
                        Spacer(Modifier.width(Space.xs))
                        Eyebrow("Perk · sample", color = c.onAccent.copy(alpha = 0.75f))
                        Spacer(Modifier.weight(1f))
                        Text("Saves you ${IndianFormat.days(days)}", style = Ruvo.type.label, color = c.onAccent)
                    }
                    Spacer(Modifier.height(Space.xs))
                    Text("${perk.brand}: ${perk.offer}", style = Ruvo.type.bodyStrong, color = c.onAccent)
                }
            }
        }
        item { SectionTitle("Recent", Modifier.padding(horizontal = Space.gutter), action = "See all", onAction = a.openMoney) }
        val recent = state.activity.filter { it.txn.status != com.ruvo.engine.TxnStatus.IGNORED }.take(4)
        recent.forEach { entry -> item(key = entry.txn.id) { ActivityLine(entry, state, a.openTxn) } }
    }
}

/** Next date each recurring commitment falls due, soonest first. */
private fun comingUp(state: AppState): List<Pair<com.ruvo.engine.Commitment, LocalDate>> =
    state.commitments.mapNotNull { cm ->
        val date = cm.dueDate ?: cm.dueDayOfMonth?.let { day ->
            val thisMonth = state.today.withDayOfMonth(day.coerceAtMost(state.today.lengthOfMonth()))
            if (!thisMonth.isBefore(state.today)) thisMonth
            else state.today.plusMonths(1).let { it.withDayOfMonth(day.coerceAtMost(it.lengthOfMonth())) }
        }
        date?.takeIf { !it.isBefore(state.today) }?.let { cm to it }
    }.sortedBy { it.second }

@Composable
private fun InsightCard(change: Insights.CategoryChange, onWhy: () -> Unit, onHide: () -> Unit, modifier: Modifier) {
    val c = Ruvo.colors
    RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm).then(modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(c.brand.copy(alpha = if (c.isDark) 0.22f else 0.12f)), contentAlignment = Alignment.Center) {
                Icon(RuvoIcons.Bulb, null, Modifier.size(18.dp), tint = c.brandInk)
            }
            Spacer(Modifier.width(Space.xs))
            Eyebrow("Ruvo noticed", color = c.brandInk)
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            "${change.category} spending is ${Insights.changeWords(change.changePercent ?: 0, "last week")}.",
            style = Ruvo.type.title, color = c.ink,
        )
        Text("${IndianFormat.rupees(change.thisPeriod)} this week. Small amounts are easy to miss, and easy to change.", style = Ruvo.type.caption, color = c.inkSoft)
        Spacer(Modifier.height(Space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            Chip("See why", true, onWhy)
            Chip("Not now", false, onHide)
        }
    }
}

/** The last seven days as pills. The tallest is the biggest day; today is drawn in ink. */
@Composable
private fun DailySpendingCard(state: AppState, modifier: Modifier) {
    val c = Ruvo.colors
    val days = remember(state.input.transactions, state.today) { Insights.dailySpending(state.input.transactions, state.today, 7) }
    val total = days.sumOf { it.spent }
    // Tap a day to see what went out. It starts on the biggest day.
    var chosen by rememberSaveable { mutableStateOf<Int?>(null) }
    val sel = chosen ?: days.indices.maxByOrNull { days[it].spent } ?: days.lastIndex
    val day = days[sel]
    val payments = state.input.transactions.filter { it.countsAsSpending && it.date == day.date }
    RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm).then(modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Daily spending")
                Amount(total, style = Ruvo.type.title, countUp = true)
            }
            com.ruvo.design.Tag("Last 7 days")
        }
        Spacer(Modifier.height(Space.md))
        PillBars(
            days.map { d ->
                Pill(
                    d.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                    (d.spent / 100).toFloat(),
                    spoken = "${IndianFormat.date(d.date, state.today)}, ${IndianFormat.spoken(d.spent)}",
                )
            },
            height = 104.dp,
            selected = sel, onSelect = { chosen = it }, tooltip = { IndianFormat.rupees(days[it].spent) },
        )
        Spacer(Modifier.height(Space.md))
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        Spacer(Modifier.height(Space.sm))
        Row {
            Text(
                day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", " + IndianFormat.date(day.date),
                Modifier.weight(1f), style = Ruvo.type.bodyStrong, color = c.ink,
            )
            Amount(day.spent, style = Ruvo.type.bodyStrong)
        }
        if (payments.isEmpty()) {
            Text("Nothing spent on this day.", Modifier.padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkFaint)
        } else {
            payments.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(top = Space.xs), verticalAlignment = Alignment.CenterVertically) {
                    MerchantLogo(p.merchant, p.category, size = 30.dp)
                    Spacer(Modifier.width(Space.sm))
                    Column(Modifier.weight(1f)) {
                        Text(p.merchant.ifBlank { p.category }, style = Ruvo.type.label, color = c.ink, maxLines = 1)
                        Text(p.category, style = Ruvo.type.caption, color = c.inkFaint)
                    }
                    Amount(-p.yourShare, style = Ruvo.type.label)
                }
            }
        }
    }
}

@Composable
private fun ReviewTile(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, open: () -> Unit) {
    ActionRow(title, sub, icon, modifier, open)
}

/** The one shape every "open something" row on Home uses: a quiet icon, a title and line, and an arrow. */
@Composable
private fun ActionRow(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, open: () -> Unit) {
    val c = Ruvo.colors
    RuvoCard(modifier, onClick = open, onClickLabel = "Open $title") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.ink.copy(alpha = if (c.isDark) 0.1f else 0.06f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = c.ink)
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(title, style = Ruvo.type.bodyStrong, color = c.ink)
                Text(sub, style = Ruvo.type.caption, color = c.inkSoft)
            }
            Icon(RuvoIcons.Forward, null, Modifier.size(18.dp), tint = c.inkSoft)
        }
    }
}
@Composable
private fun Header(state: AppState, a: TodayActions) {
    val c = Ruvo.colors
    val hour = LocalTime.now().hour
    val hello = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val day = state.today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Space.gutter, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Eyebrow("$day, ${IndianFormat.date(state.today)}")
            Text("$hello, ${state.displayName}", style = Ruvo.type.headline, color = c.ink)
            if (state.isDemo) {
                Text(
                    "Sample student · switch",
                    Modifier.padding(top = 4.dp).clip(RoundedCornerShape(Radius.chip))
                        .background(c.brand.copy(alpha = if (c.isDark) 0.22f else 0.1f))
                        .clickable(role = Role.Button, onClick = a.openPersonas)
                        .padding(horizontal = Space.sm, vertical = 6.dp),
                    style = Ruvo.type.caption, color = c.brandInk,
                )
            }
        }
        // Adding by hand is always one tap away, not hidden behind the Orb gesture.
        IconCircle(RuvoIcons.Plus, "Add a payment", a.openAdd, tint = c.onBrand, background = c.brand)
        Spacer(Modifier.width(Space.xs))
        Avatar(state.displayName, a.openProfile)
    }
}

@Composable
private fun SortCard(count: Int, open: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm).then(modifier), brush = c.softAmberBrush, border = null, elevated = true, onClick = open, onClickLabel = "Start the weekly sort") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
                Text(count.toString(), style = Ruvo.type.title, color = if (c.isDark) c.canvas else c.surface)
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(if (count == 1) "1 new payment found" else "$count new payments found", style = Ruvo.type.bodyStrong, color = c.onAccent)
                Text("Swipe through them in about two minutes", style = Ruvo.type.caption, color = c.onAccent.copy(alpha = 0.72f))
            }
            Icon(RuvoIcons.Forward, null, tint = c.onAccent)
        }
    }
}

@Composable
private fun BuyCard(open: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    ActionRow("Before you buy", "Type a price, see what it costs in days", RuvoIcons.Cart, Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm).then(modifier), open)
}

@Composable
fun ActivityLine(item: ActivityItem, state: AppState, open: (String) -> Unit) {
    val c = Ruvo.colors
    val t = item.txn
    val credit = t.direction == Direction.CREDIT
    val tag = when {
        t.status == com.ruvo.engine.TxnStatus.TO_SORT -> "To sort"
        item.source == com.ruvo.data.db.Source.SMS -> "From SMS"
        item.source == com.ruvo.data.db.Source.DEMO -> "Sample"
        else -> "Added by you"
    }
    val shared = if (t.othersShare > 0) " · shared" else ""
    ListRow(
        title = t.merchant,
        subtitle = "${IndianFormat.relativeDay(t.date, state.today)} · ${t.category}$shared · $tag",
        modifier = Modifier.padding(horizontal = Space.gutter),
        leading = { MerchantLogo(t.merchant, t.category) },
        onClick = { open(t.id) },
    ) {
        Amount(if (credit) t.amount else -t.yourShare, color = if (credit) c.futureInk else c.ink, signed = credit)
    }
}
