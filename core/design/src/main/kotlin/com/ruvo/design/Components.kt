package com.ruvo.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruvo.engine.Bucket
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Paise

/** Springs used across the app. Interruptible, so a second tap mid-motion still feels natural. */
object Motion {
    fun <T> soft() = spring<T>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
}

/** Scales down slightly while pressed, springing back on release. */
@Composable
fun Modifier.pressable(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val reduced = Ruvo.reducedMotion
    val scale by animateFloatAsState(if (pressed && !reduced) 0.97f else 1f, Motion.snappy(), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun RuvoCard(
    modifier: Modifier = Modifier,
    color: Color = Ruvo.colors.surface,
    border: Color? = Ruvo.colors.hairline,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(Space.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(Radius.card)
    var m = modifier.pressable(interaction).clip(shape).background(color)
    if (border != null) m = m.border(BorderStroke(1.dp, border), shape)
    if (onClick != null) m = m.clickable(interaction, indication = null, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Ruvo.colors.inkSoft) {
    Text(text.uppercase(), modifier, style = Ruvo.type.eyebrow, color = color)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = Space.xl, bottom = Space.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).semantics { heading() }, style = Ruvo.type.title, color = Ruvo.colors.ink)
        if (action != null && onAction != null) {
            Text(
                action,
                Modifier.clip(RoundedCornerShape(Radius.chip)).clickable(role = Role.Button, onClick = onAction)
                    .heightIn(min = 48.dp).padding(horizontal = Space.sm, vertical = Space.sm),
                style = Ruvo.type.label, color = Ruvo.colors.inkSoft,
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val c = Ruvo.colors
    PillButton(text, onClick, modifier, background = if (enabled) c.ink else c.hairline, content = if (enabled) c.canvas else c.inkFaint, icon = icon, enabled = enabled)
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Ruvo.colors.fuel, icon: ImageVector? = null) {
    PillButton(text, onClick, modifier, background = accent, content = Ruvo.colors.onAccent, icon = icon)
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Ruvo.colors
    PillButton(text, onClick, modifier, background = Color.Transparent, content = c.ink, border = c.hairline, icon = icon)
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    background: Color,
    content: Color,
    border: Color? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(Radius.chip)
    var m = modifier.pressable(interaction).heightIn(min = 56.dp).clip(shape).background(background)
    if (border != null) m = m.border(1.dp, border, shape)
    Row(
        m.clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.lg, vertical = Space.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = content)
            Spacer(Modifier.width(Space.xs))
        }
        Text(text, style = Ruvo.type.bodyStrong, color = content, textAlign = TextAlign.Center)
    }
}

/** Selectable chip used for categories, cadence, amounts. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Ruvo.colors.ink) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    Box(
        modifier.heightIn(min = 48.dp).clip(shape)
            .background(if (selected) accent else c.surface)
            .border(1.dp, if (selected) accent else c.hairline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Ruvo.type.label, color = if (selected) (if (accent == c.ink) c.canvas else c.onAccent) else c.ink)
    }
}

/** Round icon button with a 48 dp target. */
@Composable
fun IconCircle(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Ruvo.colors.ink, background: Color = Ruvo.colors.surface) {
    Box(
        modifier.size(48.dp).clip(CircleShape).background(background).border(1.dp, Ruvo.colors.hairline, CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(22.dp), tint = tint) }
}

/**
 * Digits that roll to their new value instead of swapping. Each digit sits in its own slot so
 * the number never jitters sideways, whatever the font's figures do.
 */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    spoken: String = value.toString(),
) {
    val reduced = Ruvo.reducedMotion
    val text = value.toString()
    Row(modifier.clearAndSetSemantics { contentDescription = spoken }) {
        text.forEachIndexed { index, ch ->
            val place = text.length - index
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    if (reduced) fadeIn() togetherWith fadeOut()
                    else {
                        val up = targetState > initialState
                        (slideInVertically(Motion.soft()) { h -> if (up) h else -h } + fadeIn()) togetherWith
                            (slideOutVertically(Motion.soft()) { h -> if (up) -h else h } + fadeOut())
                    }
                },
                label = "digit$place",
            ) { d -> Text(d.toString(), style = style, color = color) }
        }
    }
}

/** An amount in rupees with Indian grouping and a spoken label. */
@Composable
fun Amount(paise: Paise, modifier: Modifier = Modifier, style: TextStyle = Ruvo.type.amount, color: Color = Ruvo.colors.ink, signed: Boolean = false) {
    val text = (if (signed && paise > 0) "+" else "") + IndianFormat.rupees(paise)
    Text(text, modifier.semantics { contentDescription = IndianFormat.spoken(paise) }, style = style, color = color)
}

/**
 * The three bucket cards share one layout. Each carries its name and icon, so colour is never the only cue.
 */
@Composable
fun BucketCard(bucket: Bucket, amount: Paise, caption: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = Ruvo.colors
    val accent = c.bucket(bucket)
    RuvoCard(modifier, onClick = onClick, onClickLabel = "Open ${bucketName(bucket)}", padding = PaddingValues(Space.md)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = if (c.isDark) 0.18f else 0.16f)), contentAlignment = Alignment.Center) {
            Icon(RuvoIcons.forBucket(bucket), null, Modifier.size(20.dp), tint = accent)
        }
        Spacer(Modifier.height(Space.sm))
        Eyebrow(bucketName(bucket))
        Spacer(Modifier.height(2.dp))
        Amount(amount, style = Ruvo.type.amount)
        Text(caption, style = Ruvo.type.caption, color = c.inkFaint, maxLines = 2)
    }
}

fun bucketName(b: Bucket) = when (b) {
    Bucket.FIXED -> "Fixed"
    Bucket.FUEL -> "Fuel"
    Bucket.FUTURE -> "Future"
}

fun bucketMeaning(b: Bucket) = when (b) {
    Bucket.FIXED -> "Already promised"
    Bucket.FUEL -> "Free to spend"
    Bucket.FUTURE -> "Protected for goals"
}

/** A row in a list: leading icon tile, two lines of text, trailing content. 64 dp tall. */
@Composable
fun ListRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    var m = modifier.fillMaxWidth().heightIn(min = 64.dp)
    if (onClick != null) m = m.clip(RoundedCornerShape(Radius.inner)).clickable(role = Role.Button, onClick = onClick)
    Row(m.padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = Ruvo.type.bodyStrong, color = Ruvo.colors.ink, maxLines = 1)
            Text(subtitle, style = Ruvo.type.caption, color = Ruvo.colors.inkFaint, maxLines = 1)
        }
        trailing()
    }
}

/** Letter tile for merchants: first letter on a soft accent disc. */
@Composable
fun MerchantTile(name: String, accent: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase() ?: "·", style = Ruvo.type.bodyStrong, color = accent)
    }
}

/** A thin progress bar with a liquid-coloured fill. */
@Composable
fun FillBar(progress: Float, accent: Color, modifier: Modifier = Modifier, height: Dp = 10.dp) {
    val reduced = Ruvo.reducedMotion
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), if (reduced) spring(stiffness = Spring.StiffnessHigh) else Motion.soft(), label = "fill")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(Radius.chip)).background(Ruvo.colors.hairline)) {
        Box(Modifier.fillMaxWidth(p).height(height).clip(RoundedCornerShape(Radius.chip)).background(accent))
    }
}
