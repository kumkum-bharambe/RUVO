package com.ruvo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ruvo.data.AppState
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Eyebrow
import com.ruvo.design.GhostButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoField
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoLogo
import com.ruvo.design.RuvoSheet
import com.ruvo.design.Space
import com.ruvo.design.reveal
import com.ruvo.engine.Cadence
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.guide.Library
import kotlinx.coroutines.launch

/**
 * Your profile: who you are in Ruvo, three numbers about how it is going, and the two places people look for
 * most, the Marketplace and Settings, set as big stamped tiles so they are never hidden.
 */
@Composable
fun ProfileSheet(
    state: AppState,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onMarketplace: () -> Unit,
    onPersonas: () -> Unit,
    onWeekly: () -> Unit,
) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(state.settings.name) }
    val who = state.persona?.let { "${it.name}, ${it.age} · ${it.livesIn}" }
        ?: when (state.settings.cadence) {
            Cadence.MONTHLY -> "Money every month"
            Cadence.LUMP_SUM -> "One big amount a term"
            Cadence.IRREGULAR -> "Money that varies"
        }
    RuvoSheet("Profile", onDismiss) {
        Row(Modifier.reveal(0), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(c.ink), contentAlignment = Alignment.Center) {
                Text(state.displayName.firstOrNull { it.isLetter() }?.uppercase() ?: "R", style = Ruvo.type.headline, color = c.canvas)
            }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(if (state.displayName == "there") "You" else state.displayName, style = Ruvo.type.headline, color = c.ink)
                Text(who, style = Ruvo.type.label, color = c.inkSoft)
                if (state.isDemo) Text("Sample student · none of this is real", style = Ruvo.type.caption, color = c.fixedInk)
            }
        }
        if (!state.isDemo) {
            Spacer(Modifier.height(Space.sm))
            if (!editing) {
                androidx.compose.material3.TextButton({ editing = true }) { Text("Edit name", style = Ruvo.type.label, color = c.ink) }
            } else {
                RuvoField(name, { name = it }, "What should Ruvo call you?")
                Spacer(Modifier.height(Space.xs))
                GhostButton("Save name", { scope.launch { RuvoGraph.repository.setName(name) }; editing = false }, Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(Space.lg))
        Row(Modifier.reveal(1), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            Stat("Runway", if (state.runway.unlimited) "No end" else IndianFormat.days(state.runway.runwayDays), Modifier.weight(1f))
            Stat("Goals", state.goals.size.toString(), Modifier.weight(1f))
            Stat("Books read", "${Library.all.count { it.id in state.settings.readBooks }} of ${Library.all.size}", Modifier.weight(1f))
        }

        Spacer(Modifier.height(Space.lg))
        Tile(
            "Marketplace", "Student savings, ways to earn and free learning. Sample offers, no ads.",
            RuvoIcons.Perk, Modifier.reveal(2), brush = true, onClick = { onDismiss(); onMarketplace() },
        )
        Spacer(Modifier.height(Space.md))
        Tile(
            "Settings", "Mood, tracking, notifications, your data, help.",
            RuvoIcons.Settings, Modifier.reveal(3), brush = false, onClick = { onDismiss(); onSettings() },
        )

        Spacer(Modifier.height(Space.lg))
        Eyebrow("More")
        Spacer(Modifier.height(Space.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            GhostButton("Your week", { onDismiss(); onWeekly() }, Modifier.weight(1f), icon = RuvoIcons.Calendar)
            GhostButton(if (state.isDemo) "Switch student" else "Try a sample", { onDismiss(); onPersonas() }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Space.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RuvoLogo(size = 28.dp)
            Spacer(Modifier.width(Space.sm))
            Text("Ruvo 1.0 · everything stays on this phone", style = Ruvo.type.caption, color = c.inkFaint)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    val c = Ruvo.colors
    RuvoCard(modifier, padding = PaddingValues(Space.sm)) {
        Eyebrow(label)
        Text(value, style = Ruvo.type.title, color = c.ink, maxLines = 1)
    }
}

/** A big stamped tile: haldi for the Marketplace, ink for Settings. */
@Composable
private fun Tile(title: String, line: String, icon: ImageVector, modifier: Modifier, brush: Boolean, onClick: () -> Unit) {
    val c = Ruvo.colors
    val fg = if (brush) c.onAccent else c.canvas
    RuvoCard(
        modifier.fillMaxWidth(), brush = if (brush) c.amberBrush else null, color = if (brush) androidx.compose.ui.graphics.Color.Unspecified else c.ink,
        elevated = true, onClick = onClick, onClickLabel = "Open $title", padding = PaddingValues(Space.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(Radius.card)).background(fg.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(24.dp), tint = fg) }
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = Ruvo.type.title, color = fg)
                Text(line, style = Ruvo.type.caption, color = fg.copy(alpha = 0.75f))
            }
            Icon(RuvoIcons.Forward, null, Modifier.size(22.dp), tint = fg)
        }
    }
}
