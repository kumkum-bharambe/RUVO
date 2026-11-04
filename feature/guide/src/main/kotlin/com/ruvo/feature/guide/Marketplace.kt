package com.ruvo.feature.guide

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Chip
import com.ruvo.design.Covers
import com.ruvo.design.FillBar
import com.ruvo.design.GhostButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.design.reveal
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.guide.Perks
import java.time.LocalDate

/** One line under the Marketplace tile, shared by Profile and the Guide. */
const val MARKETPLACE_LINE = "Student savings, ways to earn and free learning. Sample offers, no ads."

private val KINDS = listOf("All", "Student offers", "Save", "Earn", "Learn")

/**
 * The Marketplace, after Ruvo 7: every listing says who published it, when Ruvo last checked it, when it ends and
 * how much time is left, so nothing stale passes for a live offer. Current listings come first. Each shows its
 * saving in days of runway. Tap one for how to take it up. Sample listings from made-up brands; no ads.
 */
@Composable
fun MarketplaceSheet(onDismiss: () -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val today = state.today
    val daily = state.runway.dailySpend
    var kind by rememberSaveable { mutableStateOf("All") }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val listed = Perks.listed(today)
    val shown = listed.filter { kind == "All" || it.kind == kind }
    val live = listed.filter { Perks.status(it, today).state != Perks.State.EXPIRED }
    val monthDays = live.sumOf { Perks.daysSaved(it, daily) }

    RuvoSheet("Marketplace", onDismiss, eyebrow = "Student savings and opportunities") {
        RuvoCard(Modifier.fillMaxWidth(), brush = c.heroBrush, border = null, elevated = true) {
            Text("Worth up to", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.7f))
            Text(if (monthDays > 0) "${IndianFormat.days(monthDays)} a month" else "More income", style = Ruvo.type.display, color = c.onHero)
            Text("of runway, if you used every current saving here at how you spend now.", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.75f))
            Spacer(Modifier.height(Space.sm))
            Row(verticalAlignment = Alignment.Top) {
                Icon(RuvoIcons.Shield, null, Modifier.size(16.dp), tint = c.onHero)
                Spacer(Modifier.width(Space.xs))
                Text(
                    "Every listing shows who published it, when Ruvo last checked it and when it ends. These are samples with made-up brands, not real offers yet.",
                    style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.85f),
                )
            }
        }
        Spacer(Modifier.height(Space.md))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            KINDS.forEach { k ->
                val n = if (k == "All") listed.size else listed.count { it.kind == k }
                if (n > 0) Chip("$k · $n", kind == k, { kind = k })
            }
        }
        Spacer(Modifier.height(Space.sm))
        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (shown.isEmpty()) Text("Nothing in this group right now.", style = Ruvo.type.body, color = c.inkSoft)
            shown.forEachIndexed { i, p -> Listing(p, today, daily, Modifier.reveal(i)) { open = p.id } }
        }
        Spacer(Modifier.height(Space.md))
        Text(
            "Ruvo lists these to help, not to sell. Nothing here is advice; check the terms before you decide. No ads, no tracking.",
            style = Ruvo.type.caption, color = c.inkFaint,
        )
    }
    open?.let { id -> Perks.all.firstOrNull { it.id == id }?.let { ListingSheet(it, today, daily) { open = null } } }
}

/** The tint for each kind of listing, so categories read at a glance (with their name, never colour alone). */
@Composable
private fun kindTint(kind: String): Color {
    val c = Ruvo.colors
    return when (kind) {
        "Save" -> c.fuel
        "Earn" -> c.future
        "Learn" -> c.fixed
        else -> Color(0xFFFF8FB0)
    }
}

private data class StatusLook(val label: String, val tint: Color)

@Composable
private fun statusLook(s: Perks.Status): StatusLook {
    val c = Ruvo.colors
    return when (s.state) {
        Perks.State.ACTIVE -> StatusLook("Active", c.fuel)
        Perks.State.ENDING -> StatusLook("Ends in ${IndianFormat.days(s.daysLeft ?: 0)}", c.future)
        Perks.State.RECHECK -> StatusLook("Needs a re-check", c.fixed)
        Perks.State.EXPIRED -> StatusLook("Ended", c.inkFaint)
    }
}

private fun ago(days: Int) = when {
    days <= 0 -> "today"
    days == 1 -> "yesterday"
    else -> "$days days ago"
}

@Composable
private fun Pill(text: String, tint: Color) {
    val c = Ruvo.colors
    Text(
        text,
        Modifier.clip(RoundedCornerShape(Radius.chip)).background(tint.copy(alpha = if (c.isDark) 0.28f else 0.24f)).padding(horizontal = 10.dp, vertical = 4.dp),
        style = Ruvo.type.label, color = c.ink, maxLines = 1,
    )
}

@Composable
private fun Listing(p: Perks.Perk, today: LocalDate, daily: Long, modifier: Modifier, onOpen: () -> Unit) {
    val c = Ruvo.colors
    val s = Perks.status(p, today)
    val look = statusLook(s)
    val days = Perks.daysSaved(p, daily)
    val over = s.state == Perks.State.EXPIRED
    RuvoCard(modifier.fillMaxWidth().graphicsLayer { alpha = if (over) 0.6f else 1f }, onClick = onOpen, onClickLabel = "See how to get ${p.brand}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(p.kind, kindTint(p.kind))
            Spacer(Modifier.weight(1f))
            Pill(look.label, look.tint)
        }
        Spacer(Modifier.height(Space.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PerkLogo(p.brand)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(p.brand, style = Ruvo.type.title, color = c.ink)
                Text(p.offer, style = Ruvo.type.caption, color = c.inkSoft)
            }
        }
        if (!over) {
            Spacer(Modifier.height(Space.xs))
            Text(
                when {
                    days > 0 -> "Saves about ${IndianFormat.days(days)} of runway a month"
                    p.kind == "Earn" -> "Adds days instead of saving them"
                    else -> "Free while you study"
                },
                style = Ruvo.type.bodyStrong, color = c.futureInk,
            )
        }
        Spacer(Modifier.height(Space.sm))
        Facts(p, s, today)
        Spacer(Modifier.height(Space.sm))
        FillBar(s.timeLeft, look.tint, height = 6.dp)
        Spacer(Modifier.height(Space.xxs))
        Text(if (over) "This offer has ended." else "Time left in this offer", style = Ruvo.type.caption, color = c.inkFaint)
    }
}

/** Source, last check and end date, each on its own line with a quiet label. */
@Composable
private fun Facts(p: Perks.Perk, s: Perks.Status, today: LocalDate) {
    val c = Ruvo.colors
    val rows = listOf(
        "Source" to "${p.source} · ${p.sourceType}",
        "Checked" to "${IndianFormat.date(p.checked, today)} · ${ago(s.checkedDaysAgo)}",
        "Ends" to (p.ends?.let { e -> IndianFormat.date(e, today) + (s.daysLeft?.takeIf { it >= 0 }?.let { " · ${IndianFormat.days(it)} left" } ?: "") } ?: "No end date"),
    )
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.inner)).background(c.ink.copy(alpha = if (c.isDark) 0.06f else 0.04f)).padding(Space.sm),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { (k, v) ->
            Row {
                Text(k, Modifier.width(68.dp), style = Ruvo.type.caption, color = c.inkFaint)
                Text(v, style = Ruvo.type.caption, color = c.ink)
            }
        }
    }
}

/** One listing in full: what it is, where it comes from, and the one step to take it up. */
@Composable
private fun ListingSheet(p: Perks.Perk, today: LocalDate, daily: Long, onDismiss: () -> Unit) {
    val c = Ruvo.colors
    val s = Perks.status(p, today)
    val look = statusLook(s)
    val days = Perks.daysSaved(p, daily)
    RuvoSheet(p.brand, onDismiss, eyebrow = "${p.kind} · sample listing") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PerkLogo(p.brand)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(p.offer, style = Ruvo.type.bodyStrong, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Pill(look.label, look.tint)
            }
        }
        if (s.state != Perks.State.EXPIRED && days > 0) {
            Spacer(Modifier.height(Space.md))
            RuvoCard(Modifier.fillMaxWidth(), brush = c.bucketBrush(com.ruvo.engine.Bucket.FUEL), border = null) {
                Text("Worth about", style = Ruvo.type.caption, color = c.onAccent.copy(alpha = 0.7f))
                Text("${IndianFormat.days(days)} of runway a month", style = Ruvo.type.title, color = c.onAccent)
                Text("${IndianFormat.rupees(p.monthlySaving)} a month at about ${IndianFormat.rupees(daily)} a day.", style = Ruvo.type.caption, color = c.onAccent.copy(alpha = 0.75f))
            }
        }
        Spacer(Modifier.height(Space.md))
        Text("How to get it", style = Ruvo.type.label, color = c.inkSoft)
        Spacer(Modifier.height(4.dp))
        Text(p.how, style = Ruvo.type.body, color = c.ink)
        Spacer(Modifier.height(Space.md))
        Facts(p, s, today)
        if (s.state == Perks.State.RECHECK) {
            Spacer(Modifier.height(Space.sm))
            Text("Ruvo last checked this over a month ago, so confirm it still runs before you count on it.", style = Ruvo.type.caption, color = c.fixedInk)
        }
        Spacer(Modifier.height(Space.lg))
        GhostButton("Close", onDismiss, Modifier.fillMaxWidth())
    }
}

/** A made-up brand's mark: its initials on a poster colour picked from its name, so it stays the same. */
@Composable
private fun PerkLogo(brand: String) {
    val (bg, fg, _) = Covers.palettes[Math.floorMod(brand.hashCode(), Covers.palettes.size)]
    val initials = brand.split(' ', '&', '.').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(bg), contentAlignment = Alignment.Center) {
        Text(initials, style = Ruvo.type.bodyStrong, color = fg)
    }
}
