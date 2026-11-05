package com.ruvo.feature.guide

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Chip
import com.ruvo.design.Covers
import com.ruvo.design.FillBar
import com.ruvo.design.GhostButton
import com.ruvo.design.Haptics
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.design.StatusTag
import com.ruvo.design.lighten
import com.ruvo.design.reveal
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Paise
import com.ruvo.engine.guide.Perks
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One line under the Marketplace tile, shared by Profile and the Guide. */
const val MARKETPLACE_LINE = "Real student offers on music, food, apps and learning. Checked by Ruvo, no ads."

private val KINDS = listOf("All", "Saved")

/**
 * The Marketplace: real student offers, each saying where it comes from, when Ruvo last checked it and when it
 * ends, so nothing stale passes for a live offer. Offers with a known saving show it a month and in days of runway
 * a year. Any offer can be saved for later; Saved collects them. Tap one for how to take it up. No ads, and Ruvo is
 * not paid for any listing.
 */
@Composable
fun MarketplaceSheet(onDismiss: () -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val today = state.today
    val daily = state.runway.dailySpend
    val saved = state.settings.savedOffers
    var kind by rememberSaveable { mutableStateOf("All") }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val listed = Perks.listed(today)
    val shown = listed.filter { kind == "All" || (kind == "Saved" && it.id in saved) || it.kind == kind }
    val live = listed.filter { Perks.status(it, today).state.let { s -> s == Perks.State.ACTIVE || s == Perks.State.ENDING } }
    val toggle: (String) -> Unit = { id -> scope.launch { RuvoGraph.repository.toggleOffer(id) }; Haptics.tick(view) }

    RuvoSheet("Marketplace", onDismiss, eyebrow = "Student offers") {
        // The Marketplace is yellow wherever it appears, in both moods, with navy text.
        val ink = c.onLight
        RuvoCard(Modifier.fillMaxWidth(), brush = c.marketBrush, border = null, elevated = true, glow = Color(0xFFFF8A45)) {
            Text("Real offers for students", style = Ruvo.type.title, color = ink)
            Text("Music, food, apps and learning. Save the ones you like and come back to them.", style = Ruvo.type.caption, color = ink.copy(alpha = 0.8f))
            Spacer(Modifier.height(Space.sm))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.inner)).background(Color.White.copy(alpha = 0.5f)).padding(Space.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(RuvoIcons.Shield, null, Modifier.size(16.dp), tint = ink)
                Spacer(Modifier.width(Space.xs))
                Text(
                    "Each offer shows where it comes from and when Ruvo checked it. Prices and terms change, so confirm in the brand's own app before you pay. Ruvo is not paid for any listing.",
                    style = Ruvo.type.caption, color = ink.copy(alpha = 0.85f),
                )
            }
        }
        // Top pick: the current offer that saves the most each month.
        live.filter { it.monthlySaving > 0 }.maxByOrNull { it.monthlySaving }?.let { best ->
            Spacer(Modifier.height(Space.md))
            RuvoCard(Modifier.fillMaxWidth(), onClick = { open = best.id }, onClickLabel = "See ${best.brand}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(RuvoIcons.Perk, null, Modifier.size(16.dp), tint = c.fuelInk)
                    Spacer(Modifier.width(6.dp))
                    Text("Top pick for you", Modifier.weight(1f), style = Ruvo.type.label, color = c.inkSoft)
                    SaveHeart(best.id in saved) { toggle(best.id) }
                }
                Spacer(Modifier.height(Space.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PerkLogo(best.brand)
                    Spacer(Modifier.width(Space.sm))
                    Column(Modifier.weight(1f)) {
                        Text(best.brand, style = Ruvo.type.title, color = c.ink)
                        Text(best.offer, style = Ruvo.type.caption, color = c.inkSoft, maxLines = 3)
                    }
                    Spacer(Modifier.width(Space.xs))
                    SavingBadge(best)
                }
                YearLine(best, daily)
            }
        }
        Spacer(Modifier.height(Space.md))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            KINDS.forEach { k ->
                val n = when (k) {
                    "All" -> listed.size
                    "Saved" -> listed.count { it.id in saved }
                    else -> listed.count { it.kind == k }
                }
                Chip("$k · $n", kind == k, { kind = k }, accent = c.fuel)
            }
        }
        Spacer(Modifier.height(Space.sm))
        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (shown.isEmpty()) {
                Text(
                    if (kind == "Saved") "Nothing saved yet. Tap the bookmark on any offer to keep it here." else "Nothing in this group right now.",
                    style = Ruvo.type.body, color = c.inkSoft,
                )
            }
            shown.forEachIndexed { i, p -> Listing(p, today, daily, p.id in saved, { toggle(p.id) }, Modifier.reveal(i)) { open = p.id } }
        }
        Spacer(Modifier.height(Space.md))
        Text(
            "Ruvo lists these to help, not to sell. Nothing here is advice; check the terms before you decide. No ads, no tracking.",
            style = Ruvo.type.caption, color = c.inkFaint,
        )
    }
    open?.let { id -> Perks.all.firstOrNull { it.id == id }?.let { ListingSheet(it, today, daily, it.id in saved, { toggle(it.id) }) { open = null } } }
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

/** Save or unsave an offer: a bookmark that fills yellow when saved. */
@Composable
private fun SaveHeart(saved: Boolean, onToggle: () -> Unit) {
    val c = Ruvo.colors
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(if (saved) c.fuel else c.ink.copy(alpha = if (c.isDark) 0.1f else 0.05f))
            .clickable(role = Role.Button, onClickLabel = if (saved) "Remove from saved offers" else "Save this offer", onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (saved) RuvoIcons.BookmarkFilled else RuvoIcons.Bookmark, null, Modifier.size(18.dp), tint = if (saved) c.onLight else c.ink)
    }
}

/** "That's about 4 days of your spending a year": only when the saving is known. */
@Composable
private fun YearLine(p: Perks.Perk, daily: Paise) {
    val days = Perks.daysPerYear(p, daily)
    if (p.monthlySaving <= 0) return
    Spacer(Modifier.height(Space.xs))
    Text(
        "Saves ${IndianFormat.rupees(p.monthlySaving * 12)} a year" + (if (days > 0) ", about ${IndianFormat.days(days)} of your usual spending." else "."),
        style = Ruvo.type.caption, color = Ruvo.colors.inkSoft,
    )
}

@Composable
private fun Listing(p: Perks.Perk, today: LocalDate, daily: Paise, saved: Boolean, onSave: () -> Unit, modifier: Modifier, onOpen: () -> Unit) {
    val c = Ruvo.colors
    val s = Perks.status(p, today)
    val look = statusLook(s)
    val over = s.state == Perks.State.EXPIRED
    RuvoCard(modifier.fillMaxWidth().graphicsLayer { alpha = if (over) 0.6f else 1f }, onClick = onOpen, onClickLabel = "See how to get ${p.brand}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PerkLogo(p.brand)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(p.brand, style = Ruvo.type.title, color = c.ink, maxLines = 2)
                Text(p.offer, style = Ruvo.type.caption, color = c.inkSoft, maxLines = 3)
            }
            Spacer(Modifier.width(Space.xs))
            if (!over) SavingBadge(p)
        }
        YearLine(p, daily)
        Spacer(Modifier.height(Space.sm))
        // What kind it is and when it ends, in words; a status only when it needs attention; the save heart.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p.kind + " · " + (p.ends?.let { e -> if (over) "ended ${IndianFormat.date(e, today)}" else "ends ${IndianFormat.date(e, today)}" } ?: "no end date"),
                Modifier.weight(1f), style = Ruvo.type.caption, color = c.inkSoft, maxLines = 1,
            )
            if (s.state != Perks.State.ACTIVE) { StatusTag(look.label, look.tint); Spacer(Modifier.width(Space.xs)) }
            SaveHeart(saved, onSave)
        }
        if (p.ends != null) {
            Spacer(Modifier.height(Space.xs))
            // How much of its time is left.
            FillBar(s.timeLeft, if (s.state == Perks.State.ACTIVE) c.fuel else look.tint, height = 4.dp)
        }
        Spacer(Modifier.height(Space.xxs))
        Text("${p.source} · checked ${ago(s.checkedDaysAgo)}", style = Ruvo.type.caption, color = c.inkFaint, maxLines = 2)
    }
}

/** "+₹70 a month" when the saving is known, otherwise the listing's own badge ("Free", "App deals"). Yellow. */
@Composable
private fun SavingBadge(p: Perks.Perk) {
    val ink = Ruvo.colors.onLight
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).background(Ruvo.colors.marketBrush).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (p.monthlySaving > 0) {
            Text("+${IndianFormat.rupees(p.monthlySaving)}", style = Ruvo.type.title, color = ink)
            Text("a month", style = Ruvo.type.caption, color = ink.copy(alpha = 0.75f))
        } else {
            Text(p.badge ?: p.kind, style = Ruvo.type.bodyStrong, color = ink, maxLines = 1)
        }
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

/** One listing in full: what it is, where it comes from, how to take it up, and a button to save it. */
@Composable
private fun ListingSheet(p: Perks.Perk, today: LocalDate, daily: Paise, saved: Boolean, onSave: () -> Unit, onDismiss: () -> Unit) {
    val c = Ruvo.colors
    val s = Perks.status(p, today)
    val look = statusLook(s)
    RuvoSheet(p.brand, onDismiss, eyebrow = p.kind) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PerkLogo(p.brand)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(p.offer, style = Ruvo.type.bodyStrong, color = c.ink)
                Spacer(Modifier.height(4.dp))
                StatusTag(look.label, look.tint)
            }
        }
        if (s.state != Perks.State.EXPIRED && p.monthlySaving > 0) {
            Spacer(Modifier.height(Space.md))
            val ink = c.onLight
            val days = Perks.daysPerYear(p, daily)
            RuvoCard(Modifier.fillMaxWidth(), brush = c.marketBrush, border = null, glow = Color(0xFFFF8A45)) {
                Text("Saves about", style = Ruvo.type.caption, color = ink.copy(alpha = 0.72f))
                Text("${IndianFormat.rupees(p.monthlySaving)} a month", style = Ruvo.type.title, color = ink)
                Text(
                    "${IndianFormat.rupees(p.monthlySaving * 12)} a year" + (if (days > 0) ", about ${IndianFormat.days(days)} of your usual spending." else "."),
                    style = Ruvo.type.caption, color = ink.copy(alpha = 0.78f),
                )
            }
        }
        Spacer(Modifier.height(Space.md))
        Text("How to get it", style = Ruvo.type.label, color = c.inkSoft)
        Spacer(Modifier.height(4.dp))
        Text(p.how, style = Ruvo.type.body, color = c.ink)
        Spacer(Modifier.height(Space.md))
        Facts(p, s, today)
        Spacer(Modifier.height(Space.sm))
        Text("Prices and terms change. Confirm in the brand's own app or site before you pay.", style = Ruvo.type.caption, color = c.inkFaint)
        Spacer(Modifier.height(Space.lg))
        PrimaryButton(if (saved) "Saved · tap to remove" else "Save this offer", onSave, Modifier.fillMaxWidth(), icon = if (saved) RuvoIcons.BookmarkFilled else RuvoIcons.Bookmark)
        Spacer(Modifier.height(Space.sm))
        GhostButton("Close", onDismiss, Modifier.fillMaxWidth())
    }
}

/**
 * A brand's mark: its initials on a bright colour picked from its name, so it stays the same, with a soft gloss.
 * Initials rather than real logos, so nothing pretends to be the brand's own artwork.
 */
@Composable
private fun PerkLogo(brand: String) {
    val (bg, fg) = Covers.brights[Math.floorMod(brand.hashCode(), Covers.brights.size)]
    val initials = brand.replace("'s", "").split(' ', '&', '.').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(bg.lighten(0.22f), bg)))
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = Ruvo.type.bodyStrong, color = fg)
    }
}
