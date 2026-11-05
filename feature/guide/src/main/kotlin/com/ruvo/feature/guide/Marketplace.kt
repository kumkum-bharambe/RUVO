package com.ruvo.feature.guide

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import com.ruvo.design.StatusTag
import com.ruvo.design.lighten
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
    val live = listed.filter { Perks.status(it, today).state.let { s -> s == Perks.State.ACTIVE || s == Perks.State.ENDING } }
    val monthDays = live.sumOf { Perks.daysSaved(it, daily) }

    RuvoSheet("Marketplace", onDismiss, eyebrow = "Student savings and opportunities") {
        // The Marketplace is yellow wherever it appears, in both moods, with navy text.
        val ink = c.onLight
        RuvoCard(Modifier.fillMaxWidth(), brush = c.marketBrush, border = null, elevated = true, glow = Color(0xFFFF8A45)) {
            Text("Worth up to", style = Ruvo.type.caption, color = ink.copy(alpha = 0.72f))
            Text(if (monthDays > 0) "${IndianFormat.days(monthDays)} a month" else "More income", style = Ruvo.type.display, color = ink)
            Text("of runway, if you used every current saving here at how you spend now.", style = Ruvo.type.caption, color = ink.copy(alpha = 0.78f))
            Spacer(Modifier.height(Space.sm))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.inner)).background(Color.White.copy(alpha = 0.5f)).padding(Space.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(RuvoIcons.Shield, null, Modifier.size(16.dp), tint = ink)
                Spacer(Modifier.width(Space.xs))
                Text(
                    "Every listing shows who published it, when Ruvo last checked it and when it ends. These are samples with made-up brands, not real offers yet.",
                    style = Ruvo.type.caption, color = ink.copy(alpha = 0.85f),
                )
            }
        }
        // Top pick: the current listing worth the most days, so the best first step is obvious.
        live.maxByOrNull { Perks.daysSaved(it, daily) }?.takeIf { Perks.daysSaved(it, daily) > 0 }?.let { best ->
            Spacer(Modifier.height(Space.md))
            RuvoCard(Modifier.fillMaxWidth(), onClick = { open = best.id }, onClickLabel = "See ${best.brand}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(RuvoIcons.Perk, null, Modifier.size(16.dp), tint = c.fuelInk)
                    Spacer(Modifier.width(6.dp))
                    Text("Top pick for you", style = Ruvo.type.label, color = c.inkSoft)
                }
                Spacer(Modifier.height(Space.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PerkLogo(best.brand)
                    Spacer(Modifier.width(Space.sm))
                    Column(Modifier.weight(1f)) {
                        Text(best.brand, style = Ruvo.type.title, color = c.ink)
                        Text(best.offer, style = Ruvo.type.caption, color = c.inkSoft, maxLines = 2)
                    }
                    Spacer(Modifier.width(Space.xs))
                    DaysBadge(Perks.daysSaved(best, daily))
                }
            }
        }
        Spacer(Modifier.height(Space.md))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            KINDS.forEach { k ->
                val n = if (k == "All") listed.size else listed.count { it.kind == k }
                if (n > 0) Chip("$k · $n", kind == k, { kind = k }, accent = c.fuel)
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

private data class StatusLook(val label: String, val tint: Color)

/** Colour only where it means something: a running listing needs no label; ending, stale and over each get a dot. */
@Composable
private fun statusLook(s: Perks.Status): StatusLook {
    val c = Ruvo.colors
    return when (s.state) {
        Perks.State.ACTIVE -> StatusLook("Running", c.fuel)
        Perks.State.ENDING -> StatusLook("Ends in ${IndianFormat.days(s.daysLeft ?: 0)}", c.warning)
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
private fun Listing(p: Perks.Perk, today: LocalDate, daily: Long, modifier: Modifier, onOpen: () -> Unit) {
    val c = Ruvo.colors
    val s = Perks.status(p, today)
    val look = statusLook(s)
    val days = Perks.daysSaved(p, daily)
    val over = s.state == Perks.State.EXPIRED
    RuvoCard(modifier.fillMaxWidth().graphicsLayer { alpha = if (over) 0.6f else 1f }, onClick = onOpen, onClickLabel = "See how to get ${p.brand}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PerkLogo(p.brand)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(p.brand, style = Ruvo.type.title, color = c.ink, maxLines = 1)
                Text(p.offer, style = Ruvo.type.caption, color = c.inkSoft, maxLines = 2)
            }
            Spacer(Modifier.width(Space.xs))
            when {
                over -> Unit
                days > 0 -> DaysBadge(days)
                p.kind == "Earn" -> KindBadge("Earn")
                else -> KindBadge("Free")
            }
        }
        Spacer(Modifier.height(Space.sm))
        // What kind it is and when it ends, in words; a status only when it needs attention.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p.kind + " · " + (p.ends?.let { e -> if (over) "ended ${IndianFormat.date(e, today)}" else "ends ${IndianFormat.date(e, today)}" } ?: "no end date"),
                Modifier.weight(1f), style = Ruvo.type.caption, color = c.inkSoft, maxLines = 1,
            )
            if (s.state != Perks.State.ACTIVE) StatusTag(look.label, look.tint)
        }
        Spacer(Modifier.height(Space.xs))
        // How much of its time is left.
        FillBar(s.timeLeft, if (s.state == Perks.State.ACTIVE) c.fuel else look.tint, height = 4.dp)
        Spacer(Modifier.height(Space.xxs))
        Text("${p.source} · checked ${ago(s.checkedDaysAgo)}", style = Ruvo.type.caption, color = c.inkFaint, maxLines = 1)
    }
}

/** "+5 days": what a listing is worth in runway, the one number worth reading first. Marketplace yellow. */
@Composable
private fun DaysBadge(days: Int) {
    val ink = Ruvo.colors.onLight
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Ruvo.colors.marketBrush).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("+$days", style = Ruvo.type.title, color = ink)
        Text(if (days == 1) "day / month" else "days / month", style = Ruvo.type.caption, color = ink.copy(alpha = 0.75f))
    }
}

/** "Earn" or "Free", for listings that are not about saving money. */
@Composable
private fun KindBadge(text: String) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(14.dp)).background(Ruvo.colors.marketBrush).padding(horizontal = 12.dp, vertical = 8.dp),
        style = Ruvo.type.bodyStrong, color = Ruvo.colors.onLight, maxLines = 1,
    )
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
                StatusTag(look.label, look.tint)
            }
        }
        if (s.state != Perks.State.EXPIRED && days > 0) {
            Spacer(Modifier.height(Space.md))
            val ink = c.onLight
            RuvoCard(Modifier.fillMaxWidth(), brush = c.marketBrush, border = null, glow = Color(0xFFFF8A45)) {
                Text("Worth about", style = Ruvo.type.caption, color = ink.copy(alpha = 0.72f))
                Text("${IndianFormat.days(days)} of runway a month", style = Ruvo.type.title, color = ink)
                Text("${IndianFormat.rupees(p.monthlySaving)} a month at about ${IndianFormat.rupees(daily)} a day.", style = Ruvo.type.caption, color = ink.copy(alpha = 0.78f))
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

/**
 * A made-up brand's mark: its initials on a bright colour picked from its name, so it stays the same, with a soft
 * gloss across the top like a real app icon.
 */
@Composable
private fun PerkLogo(brand: String) {
    val (bg, fg) = Covers.brights[Math.floorMod(brand.hashCode(), Covers.brights.size)]
    val initials = brand.split(' ', '&', '.').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(bg.lighten(0.22f), bg)))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = Ruvo.type.bodyStrong, color = fg)
    }
}
