package com.ruvo.feature.guide

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Chip
import com.ruvo.design.Covers
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.design.Tag
import com.ruvo.design.reveal
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.guide.Perks

/** One line under the Marketplace tile, shared by Profile and the Guide. */
const val MARKETPLACE_LINE = "Student savings, ways to earn and free learning. Sample offers, no ads."

/**
 * The Marketplace: sample offers from made-up brands, each showing what it is worth in runway days. Opened from
 * the same tile in Profile and in the Guide. Not advice, no ads, no tracking.
 */
@Composable
fun MarketplaceSheet(onDismiss: () -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val daily = state.runway.dailySpend
    val kinds = listOf("All") + Perks.all.map { it.kind }.distinct()
    var kind by rememberSaveable { mutableStateOf("All") }
    val shown = Perks.all.filter { kind == "All" || it.kind == kind }
    val monthDays = Perks.all.sumOf { Perks.daysSaved(it, daily) }

    RuvoSheet("Marketplace", onDismiss, eyebrow = "Student savings and opportunities") {
        RuvoCard(Modifier.fillMaxWidth(), brush = c.heroBrush, border = null, elevated = true) {
            Text("Worth up to", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.7f))
            Text(if (monthDays > 0) "${IndianFormat.days(monthDays)} a month" else "More income", style = Ruvo.type.display, color = c.onHero)
            Text("if you used every saving here, at how you spend now.", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(Space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            kinds.forEach { k -> Chip(k, kind == k, { kind = k }) }
        }
        Spacer(Modifier.height(Space.sm))
        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            shown.forEachIndexed { i, p -> PerkRow(p, daily, Modifier.reveal(i)) }
        }
        Spacer(Modifier.height(Space.md))
        Row(verticalAlignment = Alignment.Top) {
            Icon(RuvoIcons.Shield, null, Modifier.size(18.dp), tint = c.inkFaint)
            Spacer(Modifier.width(Space.xs))
            Text(
                "Sample offers with made-up brands. They are not Ruvo advice: check the terms before you decide. No ads, no tracking.",
                style = Ruvo.type.caption, color = c.inkFaint,
            )
        }
    }
}

@Composable
private fun PerkRow(p: Perks.Perk, daily: Long, modifier: Modifier) {
    val c = Ruvo.colors
    val days = Perks.daysSaved(p, daily)
    RuvoCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PerkLogo(p.brand)
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.brand, style = Ruvo.type.bodyStrong, color = c.ink)
                    Spacer(Modifier.width(Space.xs))
                    Tag(p.kind)
                }
                Text(p.offer, style = Ruvo.type.caption, color = c.inkSoft)
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text(
            when {
                days > 0 -> "Saves you about ${IndianFormat.days(days)} a month"
                p.kind == "Earn" -> "Adds days instead of saving them"
                else -> "Free while you study"
            },
            style = Ruvo.type.label, color = c.futureInk,
        )
    }
}

/** A made-up brand's mark: its initials on a poster colour picked from its name, so it stays the same. */
@Composable
private fun PerkLogo(brand: String) {
    val (bg, fg, _) = Covers.palettes[Math.floorMod(brand.hashCode(), Covers.palettes.size)]
    val initials = brand.split(' ', '&', '.').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(bg), contentAlignment = Alignment.Center) {
        Text(initials, style = Ruvo.type.bodyStrong, color = fg)
    }
}
