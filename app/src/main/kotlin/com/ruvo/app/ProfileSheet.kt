package com.ruvo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ruvo.data.AppState
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Amount
import com.ruvo.design.BigTile
import com.ruvo.design.Eyebrow
import com.ruvo.design.GhostButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoLogo
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Segment
import com.ruvo.design.SplitBar
import com.ruvo.design.Space
import com.ruvo.design.ThemeChoice
import com.ruvo.design.reveal
import com.ruvo.engine.Cadence
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Insights
import com.ruvo.engine.guide.Library
import com.ruvo.feature.guide.MARKETPLACE_LINE
import kotlinx.coroutines.launch

/**
 * Your profile: who you are in Ruvo and how it is going, this month at a glance, a quick mood switch, the
 * Marketplace and Settings as big tiles, and shortcuts to the things people come back for.
 */
@Composable
fun ProfileSheet(
    state: AppState,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onMarketplace: () -> Unit,
    onGuide: () -> Unit,
    onPersonas: () -> Unit,
    onWeekly: () -> Unit,
) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var changingPin by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(state.settings.name) }
    // The name is already the title, so the line under it says only age and where they live.
    val who = state.persona?.let { "${it.age} · ${it.livesIn}" }
        ?: when (state.settings.cadence) {
            Cadence.MONTHLY -> "Money every month"
            Cadence.LUMP_SUM -> "One big amount a term"
            Cadence.IRREGULAR -> "Money that varies"
        }
    val read = Library.all.count { it.id in state.settings.readBooks }

    RuvoSheet("Profile", onDismiss) {
        // Who you are, on white glass, with the three numbers that sum it up. The avatar carries the brand blue.
        RuvoCard(Modifier.fillMaxWidth().reveal(0)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(62.dp).clip(CircleShape).background(c.brandBrush)
                        .border(3.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = if (c.isDark) 0.25f else 0.9f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.displayName.firstOrNull { it.isLetter() }?.uppercase() ?: "R", style = Ruvo.type.headline, color = c.onBrand)
                }
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(if (state.displayName == "there") "You" else state.displayName, style = Ruvo.type.headline, color = c.ink)
                    Text(who, style = Ruvo.type.caption, color = c.inkSoft)
                    if (state.isDemo) Text("Sample student, none of this is real", style = Ruvo.type.caption, color = c.inkFaint)
                }
                if (!state.isDemo) {
                    androidx.compose.material3.TextButton({ editing = !editing }) {
                        Text(if (editing) "Close" else "Edit", style = Ruvo.type.label, color = c.ink)
                    }
                }
            }
            Spacer(Modifier.height(Space.md))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.inner)).background(c.ink.copy(alpha = if (c.isDark) 0.08f else 0.05f)).padding(Space.sm)) {
                val goals = state.goals.size
                HeroStat("Runway", if (state.runway.unlimited) "No end" else IndianFormat.days(state.runway.runwayDays), Modifier.weight(1f))
                HeroStat(if (goals == 1) "Goal" else "Goals", goals.toString(), Modifier.weight(1f))
                HeroStat("Books read", "$read of ${Library.all.size}", Modifier.weight(1f))
            }
        }
        if (editing) {
            Spacer(Modifier.height(Space.sm))
            RuvoField(name, { name = it }, "What should Ruvo call you?")
            Spacer(Modifier.height(Space.xs))
            GhostButton("Save name", { scope.launch { RuvoGraph.repository.setName(name) }; editing = false }, Modifier.fillMaxWidth())
        }

        // This month so far: what came in, what went out, what is protected.
        Spacer(Modifier.height(Space.md))
        val from = state.today.withDayOfMonth(1)
        val flow = Insights.flow(state.activity.map { it.txn }, from, state.today)
        RuvoCard(Modifier.fillMaxWidth().reveal(1)) {
            Eyebrow("${state.today.month.name.lowercase().replaceFirstChar { it.uppercase() }} so far")
            Spacer(Modifier.height(Space.xs))
            // Money in is brand blue, protected is Future orange, what went out stays plain ink.
            Row {
                MonthFigure("Came in", flow.moneyIn, c.brandInk, Modifier.weight(1f))
                MonthFigure("Went out", flow.moneyOut, c.ink, Modifier.weight(1f))
                MonthFigure("Protected", state.runway.futureProtected, c.futureInk, Modifier.weight(1f))
            }
            Spacer(Modifier.height(Space.sm))
            val spentShade = c.ink.copy(alpha = if (c.isDark) 0.45f else 0.3f)
            val total = (flow.moneyOut + state.runway.futureProtected + state.runway.fuel.coerceAtLeast(0)).toFloat().coerceAtLeast(1f)
            SplitBar(
                listOf(
                    Segment(flow.moneyOut / total, spentShade),
                    Segment(state.runway.futureProtected / total, c.future),
                    Segment(state.runway.fuel.coerceAtLeast(0) / total, c.fuel),
                ),
                height = 10.dp,
            )
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                com.ruvo.design.LegendDot(spentShade, "Spent")
                com.ruvo.design.LegendDot(c.future, "Protected")
                com.ruvo.design.LegendDot(c.fuel, "Still yours")
            }
        }

        // Mood, one tap away: a frosted track with a white pill on the chosen one.
        Spacer(Modifier.height(Space.md))
        val theme = state.settings.theme
        Row(
            Modifier.reveal(2).fillMaxWidth().clip(RoundedCornerShape(Radius.chip))
                .background(if (c.isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f) else c.ink.copy(alpha = 0.07f))
                .border(1.dp, if (c.isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f), RoundedCornerShape(Radius.chip))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MoodPill("Daybreak", RuvoIcons.Sun, theme == ThemeChoice.DAYBREAK.name || (theme == ThemeChoice.SYSTEM.name && !c.isDark), Modifier.weight(1f)) {
                scope.launch { RuvoGraph.repository.setTheme(ThemeChoice.DAYBREAK.name) }
            }
            MoodPill("Night swim", RuvoIcons.Moon, theme == ThemeChoice.NIGHT_SWIM.name || (theme == ThemeChoice.SYSTEM.name && c.isDark), Modifier.weight(1f)) {
                scope.launch { RuvoGraph.repository.setTheme(ThemeChoice.NIGHT_SWIM.name) }
            }
        }

        Spacer(Modifier.height(Space.md))
        BigTile("Marketplace", MARKETPLACE_LINE, RuvoIcons.Store, Modifier.reveal(3)) { onDismiss(); onMarketplace() }
        Spacer(Modifier.height(Space.sm))
        BigTile("Settings", "Mood, tracking, notifications, your data, help.", RuvoIcons.Settings, Modifier.reveal(4), style = com.ruvo.design.TileStyle.GLASS) { onDismiss(); onSettings() }

        Spacer(Modifier.height(Space.md))
        RuvoCard(Modifier.fillMaxWidth().reveal(5), padding = PaddingValues(horizontal = Space.md, vertical = Space.xs)) {
            Shortcut(RuvoIcons.User, if (state.isDemo) "Switch sample student" else "Try a sample student", "Five made-up students to explore") { onDismiss(); onPersonas() }
            Shortcut(RuvoIcons.Lock, "Change PIN", "The 4-digit PIN you open Ruvo with") { changingPin = true }
            Shortcut(RuvoIcons.Shield, "Private by design", "Your account and data stay on this phone, encrypted", onClick = null)
        }

        Spacer(Modifier.height(Space.md))
        GhostButton("Sign out", { onDismiss(); RuvoGraph.repository.signOut() }, Modifier.fillMaxWidth())
        if (state.settings.accountPhone.isNotEmpty()) {
            Text(
                "Signed in as ${state.settings.accountName} · +91 ${com.ruvo.data.Mobile.masked(state.settings.accountPhone)}",
                Modifier.fillMaxWidth().padding(top = Space.xs), style = Ruvo.type.caption, color = c.inkFaint,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Spacer(Modifier.height(Space.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RuvoLogo(size = 26.dp)
            Spacer(Modifier.width(Space.sm))
            Text("Ruvo 1.0", style = Ruvo.type.caption, color = c.inkFaint)
        }
    }
    if (changingPin) com.ruvo.feature.onboarding.ChangePinSheet { changingPin = false }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) {
    val c = Ruvo.colors
    Column(modifier) {
        Text(value, style = Ruvo.type.title, color = c.ink, maxLines = 1)
        Text(label, style = Ruvo.type.caption, color = c.inkSoft, maxLines = 1)
    }
}

@Composable
private fun MonthFigure(label: String, paise: Long, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val c = Ruvo.colors
    Column(modifier) {
        Amount(paise, style = Ruvo.type.bodyStrong, color = color)
        Text(label, style = Ruvo.type.caption, color = c.inkSoft)
    }
}

/** One half of the mood switch: a white pill with navy text when chosen, plain text on the track when not. */
@Composable
private fun MoodPill(label: String, icon: ImageVector, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    val fg = if (on) c.onLight else c.inkSoft
    Row(
        modifier.heightIn(min = 48.dp)
            .then(if (on && !c.isDark) Modifier.shadow(6.dp, shape, ambientColor = c.shadow, spotColor = c.shadow) else Modifier)
            .clip(shape)
            .background(if (on) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Transparent)
            .semantics { selected = on }
            .clickable(role = Role.RadioButton, onClickLabel = "Use $label", onClick = onClick)
            .padding(horizontal = Space.md),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = fg)
        Spacer(Modifier.width(Space.xs))
        Text(label, style = Ruvo.type.label, color = fg)
    }
}

@Composable
private fun Shortcut(icon: ImageVector, title: String, line: String, onClick: (() -> Unit)?) {
    val c = Ruvo.colors
    var m = Modifier.fillMaxWidth().heightIn(min = 56.dp)
    if (onClick != null) m = m.clip(RoundedCornerShape(Radius.inner)).clickable(role = Role.Button, onClick = onClick)
    Row(m.padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.ink.copy(alpha = if (c.isDark) 0.1f else 0.06f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = c.ink)
        }
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = Ruvo.type.bodyStrong, color = c.ink)
            Text(line, style = Ruvo.type.caption, color = c.inkFaint)
        }
        if (onClick != null) Icon(RuvoIcons.Forward, null, Modifier.size(18.dp), tint = c.inkFaint)
    }
}
