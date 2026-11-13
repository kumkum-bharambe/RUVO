package com.ruvo.feature.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Chip
import com.ruvo.design.FillBar
import com.ruvo.design.Haptics
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.design.StatusTag
import com.ruvo.design.reveal
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.guide.Perks
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One line under the Marketplace tile, shared by Profile and the Guide. */
const val MARKETPLACE_LINE = "Real student offers on music, food, apps and learning. Checked by Ruvo, no ads."

/**
 * Where "Open" takes the student: the brand's own page for the offer. Kept here, not in the engine, because it is
 * only a link; the offer itself, its source and its check date stay in [Perks].
 */
private val LINKS = mapOf(
    "spotify" to "https://www.spotify.com/in-en/student/",
    "youtube" to "https://www.youtube.com/premium/student",
    "swiggy" to "https://www.swiggy.com/",
    "swiggylite" to "https://www.swiggy.com/",
    "mcdonalds" to "https://www.mcdonaldsindia.com/",
    "github" to "https://education.github.com/pack",
    "figma" to "https://www.figma.com/education/",
    "internshala" to "https://internshala.com/",
)

/** Each brand's tile: a soft two-stop gradient and the ink that reads on it. */
private val TILES = mapOf(
    "spotify" to Triple(Color(0xFFFFA76B), Color(0xFFFF7A3D), Color.White),
    "youtube" to Triple(Color(0xFFFF8FB8), Color(0xFFFF5C9A), Color.White),
    "swiggy" to Triple(Color(0xFFA58BFF), Color(0xFF7C5CF0), Color.White),
    "swiggylite" to Triple(Color(0xFFB39DFF), Color(0xFF8B6CFF), Color.White),
    "mcdonalds" to Triple(Color(0xFFFFD36B), Color(0xFFFFBE2E), Color(0xFF141B4D)),
    "github" to Triple(Color(0xFF5A73E6), Color(0xFF2338A8), Color.White),
    "figma" to Triple(Color(0xFFFFA76B), Color(0xFFFF7A3D), Color.White),
    "internshala" to Triple(Color(0xFF7FB2FF), Color(0xFF4A8CF0), Color.White),
)

/**
 * The Marketplace: real student offers, each saying what kind it is, when it ends, where it comes from and when Ruvo
 * last checked it. A yellow badge gives the saving at a glance. "How to get it" opens right on the card, with a
 * button to the brand's own app or site. Any offer can be saved; Saved collects them. No ads, and Ruvo is not paid
 * for any listing.
 */
@Composable
fun MarketplaceSheet(onDismiss: () -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val today = state.today
    val saved = state.settings.savedOffers
    var kind by rememberSaveable { mutableStateOf("All") }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val listed = Perks.listed(today)
    val shown = listed.filter { kind == "All" || it.id in saved }
    val toggle: (String) -> Unit = { id -> scope.launch { RuvoGraph.repository.toggleOffer(id) }; Haptics.tick(view) }

    RuvoSheet("Marketplace", onDismiss, eyebrow = "Student offers") {
        // The Marketplace is yellow wherever it appears, in both moods, with navy text.
        val ink = c.onLight
        RuvoCard(Modifier.fillMaxWidth(), brush = c.marketBrush, border = null, elevated = true, glow = Color(0xFFFF8A45), padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
            Text("Real offers for students", style = Ruvo.type.headline, color = ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "Checked by Ruvo on ${IndianFormat.date(Perks.all.maxOf { it.checked }, today)}. No ads, no paid listings. Open an offer to go to the brand's own app or site.",
                style = Ruvo.type.body, color = ink.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            Chip("All", kind == "All", { kind = "All" }, accent = c.fuel)
            Chip("Saved · ${listed.count { it.id in saved }}", kind == "Saved", { kind = "Saved" }, accent = c.fuel)
        }
        Spacer(Modifier.height(Space.sm))
        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            if (shown.isEmpty()) {
                RuvoCard(Modifier.fillMaxWidth()) {
                    Text("No saved offers yet", Modifier.fillMaxWidth(), style = Ruvo.type.title, color = c.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap the bookmark on any offer to keep it here.", Modifier.fillMaxWidth(), style = Ruvo.type.body, color = c.inkSoft, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            shown.forEachIndexed { i, p ->
                Listing(p, today, p.id in saved, open == p.id, { toggle(p.id) }, Modifier.reveal(i)) { open = if (open == p.id) null else p.id }
            }
        }
        Spacer(Modifier.height(Space.md))
        Text(
            "Ruvo lists these to help, not to sell. Nothing here is advice; check the terms before you decide. No ads, no tracking.",
            style = Ruvo.type.caption, color = c.inkFaint,
        )
    }
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
private fun SaveMark(saved: Boolean, onToggle: () -> Unit) {
    val c = Ruvo.colors
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(if (saved) c.fuel else c.ink.copy(alpha = if (c.isDark) 0.1f else 0.06f))
            .clickable(role = Role.Button, onClickLabel = if (saved) "Remove from saved offers" else "Save this offer", onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (saved) RuvoIcons.BookmarkFilled else RuvoIcons.Bookmark, null, Modifier.size(20.dp), tint = if (saved) c.onLight else c.ink)
    }
}

@Composable
private fun Listing(p: Perks.Perk, today: LocalDate, saved: Boolean, expanded: Boolean, onSave: () -> Unit, modifier: Modifier, onOpen: () -> Unit) {
    val c = Ruvo.colors
    val uri = LocalUriHandler.current
    val s = Perks.status(p, today)
    val look = statusLook(s)
    val over = s.state == Perks.State.EXPIRED
    RuvoCard(
        modifier.fillMaxWidth().graphicsLayer { alpha = if (over) 0.6f else 1f },
        onClick = onOpen, onClickLabel = if (expanded) "Hide how to get ${p.brand}" else "How to get ${p.brand}",
        padding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            PerkLogo(p)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(p.brand, style = Ruvo.type.title, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(p.offer, style = Ruvo.type.body, color = c.inkSoft)
            }
            Spacer(Modifier.width(Space.xs))
            if (!over) SavingBadge(p)
        }
        Spacer(Modifier.height(Space.sm))
        // What kind it is and when it ends, in words; a status only when it needs attention; the bookmark.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p.kind + " · " + (p.ends?.let { e -> if (over) "ended ${IndianFormat.date(e, today)}" else "ends ${IndianFormat.date(e, today)}" } ?: "no end date"),
                Modifier.weight(1f), style = Ruvo.type.body, color = c.inkSoft, maxLines = 1,
            )
            if (s.state != Perks.State.ACTIVE) { StatusTag(look.label, look.tint); Spacer(Modifier.width(Space.xs)) }
            SaveMark(saved, onSave)
        }
        if (p.ends != null) {
            Spacer(Modifier.height(Space.xs))
            // How much of its time is left.
            FillBar(s.timeLeft, if (s.state == Perks.State.ACTIVE) c.fuel else look.tint, height = 4.dp)
        }
        Spacer(Modifier.height(Space.xs))
        Text("${p.source} · checked ${ago(s.checkedDaysAgo)}", style = Ruvo.type.caption, color = c.inkFaint, maxLines = 2)
        Spacer(Modifier.height(Space.xs))
        // "How to get it" opens in place, so the list never jumps to another screen.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (expanded) "▾" else "▸", style = Ruvo.type.label, color = c.brandInk)
            Spacer(Modifier.width(6.dp))
            Text("How to get it", style = Ruvo.type.bodyStrong, color = c.brandInk)
        }
        AnimatedVisibility(expanded) {
            Column {
                Spacer(Modifier.height(Space.xs))
                Text(p.how, style = Ruvo.type.body, color = c.ink)
                LINKS[p.id]?.let { link ->
                    Spacer(Modifier.height(Space.sm))
                    PrimaryButton("Open ${p.brand.substringBefore(' ')}", { runCatching { uri.openUri(link) } }, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(Space.xs))
                Text("Prices and terms change. Confirm in the brand's own app or site before you pay.", style = Ruvo.type.caption, color = c.inkFaint)
            }
        }
    }
}

/** "Save ₹70/mo" when the saving is known, otherwise the listing's own badge ("Free", "App deals"). Yellow, one line. */
@Composable
private fun SavingBadge(p: Perks.Perk) {
    val ink = Ruvo.colors.onLight
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(Ruvo.colors.marketBrush).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            if (p.monthlySaving > 0) "Save ${IndianFormat.rupees(p.monthlySaving)}/mo" else p.badge ?: p.kind,
            style = Ruvo.type.bodyStrong, color = ink, maxLines = 1,
        )
    }
}

/**
 * A brand's mark: its initials on a bright gradient tile with a white ring. Initials rather than real logos, so
 * nothing pretends to be the brand's own artwork.
 */
@Composable
private fun PerkLogo(p: Perks.Perk) {
    val (top, bottom, fg) = TILES[p.id] ?: Triple(Color(0xFFFFA76B), Color(0xFFFF7A3D), Color.White)
    val initials = p.brand.replace("'s", "").split(' ', '&', '.').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(top, bottom)))
            .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = Ruvo.type.title, color = fg)
    }
}
