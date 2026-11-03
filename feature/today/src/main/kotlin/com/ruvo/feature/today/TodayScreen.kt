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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.ActivityItem
import com.ruvo.data.AppState
import com.ruvo.data.RuvoGraph
import com.ruvo.design.BucketCard
import com.ruvo.design.Eyebrow
import com.ruvo.design.IconCircle
import com.ruvo.design.MerchantTile
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
import com.ruvo.engine.guide.Lessons
import com.ruvo.engine.guide.Perks
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
)

@Composable
fun TodayScreen(actions: TodayActions) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    TodayContent(state, actions)
}

@Composable
fun TodayContent(state: AppState, a: TodayActions) {
    val c = Ruvo.colors
    val r = state.runway
    LazyColumn(
        Modifier.fillMaxSize().background(c.canvas),
        contentPadding = PaddingValues(bottom = Space.dockClearance),
    ) {
        item {
            Header(state, a)
        }
        item {
            RunwayHero(r, a.openWhy, Modifier.padding(horizontal = Space.gutter))
            Spacer(Modifier.height(Space.md))
            DayStripRow(r)
        }
        item {
            Row(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                BucketCard(Bucket.FIXED, r.fixedReserved, "${r.reservedCommitments.size} promised", Modifier.weight(1f), a.openMoney)
                BucketCard(Bucket.FUEL, r.fuel.coerceAtLeast(0), "Free to spend", Modifier.weight(1f), a.openWhy)
                BucketCard(Bucket.FUTURE, r.futureProtected, "${state.goals.size} goals", Modifier.weight(1f), a.openGoals)
            }
        }
        if (state.toSort.isNotEmpty()) {
            item { SortCard(state.toSort.size, a.openSort) }
        }
        item { BuyCard(a.openBuy) }
        item {
            val lesson = Lessons.forNow(state.input, r)
            RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm), onClick = { a.openLesson(lesson.id) }, onClickLabel = "Read lesson") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(RuvoIcons.Book, null, Modifier.size(20.dp), tint = c.fixed)
                    Spacer(Modifier.width(Space.xs))
                    Eyebrow("A small idea", color = c.fixed)
                }
                Spacer(Modifier.height(Space.xs))
                Text(lesson.title, style = Ruvo.type.title, color = c.ink)
                Text(lesson.body, style = Ruvo.type.body, color = c.inkSoft, maxLines = 2)
            }
        }
        // An occasional perk card: one a day, rotating, with its saving in days.
        item {
            val perk = Perks.all[(state.today.dayOfYear) % Perks.all.size]
            val days = Perks.daysSaved(perk, r.dailySpend)
            if (days > 0) {
                RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm), color = c.sun.copy(alpha = if (c.isDark) 0.14f else 0.22f), border = null, onClick = a.openGoals, onClickLabel = "See perks") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(RuvoIcons.Perk, null, Modifier.size(20.dp), tint = c.ink)
                        Spacer(Modifier.width(Space.xs))
                        Eyebrow("Perk · sample", color = c.ink)
                        Spacer(Modifier.weight(1f))
                        Text("Saves you ${IndianFormat.days(days)}", style = Ruvo.type.label, color = c.ink)
                    }
                    Spacer(Modifier.height(Space.xs))
                    Text("${perk.brand}: ${perk.offer}", style = Ruvo.type.bodyStrong, color = c.ink)
                }
            }
        }
        item { SectionTitle("Recent", Modifier.padding(horizontal = Space.gutter), action = "See all", onAction = a.openMoney) }
        val recent = state.activity.filter { it.txn.status != com.ruvo.engine.TxnStatus.IGNORED }.take(4)
        recent.forEach { entry -> item(key = entry.txn.id) { ActivityLine(entry, state, a.openTxn) } }
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
                    "Sample student · tap to switch",
                    Modifier.padding(top = 4.dp).clip(RoundedCornerShape(Radius.chip))
                        .background(c.fixed.copy(alpha = 0.14f))
                        .clickable(role = Role.Button, onClick = a.openPersonas)
                        .padding(horizontal = Space.sm, vertical = 6.dp),
                    style = Ruvo.type.caption, color = c.fixed,
                )
            }
        }
        if (state.isDemo) IconCircle(RuvoIcons.Spark, "Switch sample student", a.openPersonas)
        Spacer(Modifier.width(Space.xs))
        IconCircle(RuvoIcons.Settings, "Settings", a.openSettings)
    }
}

@Composable
private fun SortCard(count: Int, open: () -> Unit) {
    val c = Ruvo.colors
    RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm), color = c.ink, border = null, onClick = open, onClickLabel = "Start the weekly sort") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.fuel), contentAlignment = Alignment.Center) {
                Text(count.toString(), style = Ruvo.type.title, color = c.onAccent)
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(if (count == 1) "1 payment to sort" else "$count payments to sort", style = Ruvo.type.bodyStrong, color = c.canvas)
                Text("Swipe through them in about two minutes", style = Ruvo.type.caption, color = c.canvas.copy(alpha = 0.7f))
            }
            Icon(RuvoIcons.Forward, null, tint = c.canvas)
        }
    }
}

@Composable
private fun BuyCard(open: () -> Unit) {
    val c = Ruvo.colors
    RuvoCard(Modifier.padding(horizontal = Space.gutter).padding(top = Space.sm), onClick = open, onClickLabel = "Open Before you buy") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(RuvoIcons.Cart, null, Modifier.size(22.dp), tint = c.future)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text("Before you buy", style = Ruvo.type.bodyStrong, color = c.ink)
                Text("Type a price, see what it costs in days", style = Ruvo.type.caption, color = c.inkSoft)
            }
            Icon(RuvoIcons.Forward, null, tint = c.inkSoft)
        }
    }
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
        leading = { MerchantTile(t.merchant, if (credit) c.fuel else c.bucket(t.bucket)) },
        onClick = { open(t.id) },
    ) {
        Amount(if (credit) t.amount else -t.yourShare, color = if (credit) c.fuel else c.ink, signed = credit)
    }
}
