package com.ruvo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val who = state.persona?.let { "${it.name}, ${it.age} · ${it.livesIn}" }
        ?: when (state.settings.cadence) {
            Cadence.MONTHLY -> "Money every month"
            Cadence.LUMP_SUM -> "One big amount a term"
            Cadence.IRREGULAR -> "Money that varies"
        }
    val read = Library.all.count { it.id in state.settings.readBooks }
    val ideas = state.settings.savedIdeas.size

    RuvoSheet("Profile", onDismiss) {
        // Who you are, on navy, with the three numbers that sum it up.
        RuvoCard(Modifier.fillMaxWidth().reveal(0), brush = c.heroBrush, border = null, elevated = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(c.amberBrush), contentAlignment = Alignment.Center) {
                    Text(state.displayName.firstOrNull { it.isLetter() }?.uppercase() ?: "R", style = Ruvo.type.headline, color = c.onAccent)
                }
                Spacer(Modifier.width(Space.sm))
                Column(Modifier.weight(1f)) {
                    Text(if (state.displayName == "there") "You" else state.displayName, style = Ruvo.type.headline, color = c.onHero)
                    Text(who, style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.72f))
                    if (state.isDemo) Text("Sample student, none of this is real", style = Ruvo.type.caption, color = if (c.isDark) c.fuel else c.fixedInk)
                }
                if (!state.isDemo) {
                    androidx.compose.material3.TextButton({ editing = !editing }) {
                        Text(if (editing) "Close" else "Edit", style = Ruvo.type.label, color = c.onHero)
                    }
                }
            }
            Spacer(Modifier.height(Space.md))
            Row {
                HeroStat("Runway", if (state.runway.unlimited) "No end" else IndianFormat.days(state.runway.runwayDays), Modifier.weight(1f))
                HeroStat("Goals", state.goals.size.toString(), Modifier.weight(1f))
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
        RuvoCard(Modifier.fillMaxWidth().reveal(1), brush = c.bucketBrush(com.ruvo.engine.Bucket.FIXED), border = null, elevated = !c.isDark) {
            Eyebrow("${state.today.month.name.lowercase().replaceFirstChar { it.uppercase() }} so far")
            Spacer(Modifier.height(Space.xs))
            Row {
                MonthFigure("Came in", flow.moneyIn, c.futureInk, Modifier.weight(1f))
                MonthFigure("Went out", flow.moneyOut, c.ink, Modifier.weight(1f))
                MonthFigure("Protected", state.runway.futureProtected, c.futureInk, Modifier.weight(1f))
            }
            Spacer(Modifier.height(Space.sm))
            val total = (flow.moneyOut + state.runway.futureProtected + state.runway.fuel.coerceAtLeast(0)).toFloat().coerceAtLeast(1f)
            SplitBar(
                listOf(
                    Segment(flow.moneyOut / total, c.fixed),
                    Segment(state.runway.futureProtected / total, c.future),
                    Segment(state.runway.fuel.coerceAtLeast(0) / total, c.fuel),
                ),
                height = 10.dp,
            )
            Spacer(Modifier.height(Space.xxs))
            Text("Spent, protected and still yours to use", style = Ruvo.type.caption, color = c.inkFaint)
        }

        // Mood, one tap away.
        Spacer(Modifier.height(Space.md))
        val theme = state.settings.theme
        Row(Modifier.reveal(2), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
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
        BigTile("Settings", "Mood, tracking, notifications, your data, help.", RuvoIcons.Settings, Modifier.reveal(4), amber = false) { onDismiss(); onSettings() }

        Spacer(Modifier.height(Space.md))
        RuvoCard(Modifier.fillMaxWidth().reveal(5), padding = PaddingValues(horizontal = Space.md, vertical = Space.xs)) {
            Shortcut(RuvoIcons.Calendar, "Your week", "Seven days, gently reviewed") { onDismiss(); onWeekly() }
            Shortcut(RuvoIcons.Bookmark, "Saved ideas", if (ideas == 0) "Save ideas while you read" else "$ideas saved from the Guide") { onDismiss(); onGuide() }
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
        Text(value, style = Ruvo.type.title, color = c.onHero, maxLines = 1)
        Text(label, style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.65f), maxLines = 1)
    }
}

@Composable
private fun MonthFigure(label: String, paise: Long, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val c = Ruvo.colors
    Column(modifier) {
        Amount(paise, style = Ruvo.type.bodyStrong, color = color)
        Text(label, style = Ruvo.type.caption, color = c.inkFaint)
    }
}

@Composable
private fun MoodPill(label: String, icon: ImageVector, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    Row(
        modifier.heightIn(min = 48.dp).clip(shape)
            .background(if (on) c.ink else c.ink.copy(alpha = if (c.isDark) 0.1f else 0.06f))
            .clickable(role = Role.RadioButton, onClickLabel = "Use $label", onClick = onClick)
            .padding(horizontal = Space.md),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = if (on) c.canvas else c.ink)
        Spacer(Modifier.width(Space.xs))
        Text(label, style = Ruvo.type.label, color = if (on) c.canvas else c.ink)
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
