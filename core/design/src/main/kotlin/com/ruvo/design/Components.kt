package com.ruvo.design

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruvo.engine.Bucket
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Paise
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Springs used across the app. Interruptible, so a second tap mid-motion still feels natural.
 * Mature motion: things settle rather than bounce, except where something is physically pressed or stamped.
 */
object Motion {
    fun <T> soft() = spring<T>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
    /** Slow and calm, for entrances and large surfaces. */
    fun <T> gentle() = spring<T>(dampingRatio = 0.9f, stiffness = 140f)
    /** A rubber stamp landing: fast, firm, one small rebound. */
    fun <T> stamp() = spring<T>(dampingRatio = 0.5f, stiffness = 900f)

    /** Gap between items in a staggered entrance. */
    const val STAGGER_MS = 60L
}

/** Scales down slightly while pressed, springing back on release. */
@Composable
fun Modifier.pressable(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val reduced = Ruvo.reducedMotion
    val scale by animateFloatAsState(if (pressed && !reduced) 0.98f else 1f, Motion.snappy(), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * A hard, offset shadow in one flat colour, like a misregistered print. Draw it before clipping.
 */
fun Modifier.hardShadow(shape: Shape, offset: Dp, color: Color): Modifier = drawBehind {
    val o = offset.toPx()
    if (o <= 0f) return@drawBehind
    translate(o, o) { drawOutline(shape.createOutline(size, layoutDirection, this), color) }
}

/**
 * Ink-wipe entrance: the element is uncovered left to right like a pen stroke, a beat after the one before it.
 * Runs once when the element first appears. Reduced motion shows it immediately.
 */
fun Modifier.reveal(index: Int = 0): Modifier = composed {
    val reduced = Ruvo.reducedMotion
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    val shift = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(index.coerceIn(0, 12) * Motion.STAGGER_MS)
        launch { progress.animateTo(1f, Motion.gentle()) }
    }
    this
        .graphicsLayer {
            val p = progress.value
            alpha = (p * 1.6f).coerceIn(0f, 1f)
            translationX = (1f - p) * -shift
        }
        .drawWithContent {
            val p = progress.value.coerceIn(0f, 1f)
            if (p >= 0.999f) drawContent()
            else clipRect(right = size.width * (0.15f + 0.85f * p) + 1f) { this@drawWithContent.drawContent() }
        }
}

/**
 * Ruvo's card: flat paper on paper with a ruled edge. Pass [color] or [brush] for a printed panel, `border = null`
 * for no edge, and [elevated] to stamp it onto the page with a hard ink shadow.
 */
@Composable
fun RuvoCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    border: Color? = Color.Unspecified,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(Space.lg),
    brush: Brush? = null,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ruvo.colors
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(Radius.card)
    var m = modifier.pressable(interaction)
    if (elevated) m = m.hardShadow(shape, Radius.stamp, c.shadow)
    m = m.clip(shape)
    m = when {
        brush != null -> m.background(brush)
        color != Color.Unspecified -> m.background(color)
        else -> m.background(c.glass)
    }
    when {
        border == null -> {}
        elevated -> m = m.border(BorderStroke(1.5.dp, c.shadow), shape)
        border == Color.Unspecified && brush == null && color == Color.Unspecified -> m = m.border(BorderStroke(1.dp, c.glassEdge), shape)
        border == Color.Unspecified -> {}
        else -> m = m.border(BorderStroke(1.dp, border), shape)
    }
    if (onClick != null) m = m.clickable(interaction, indication = null, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Ruvo.colors.inkSoft) {
    Text(text.uppercase(), modifier, style = Ruvo.type.eyebrow, color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
}

/** A section heading in the serif voice, with a ledger rule running to the edge. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = Ruvo.colors
    Row(modifier.fillMaxWidth().padding(top = Space.xl, bottom = Space.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.semantics { heading() }, style = Ruvo.type.title, color = c.ink)
        Spacer(Modifier.width(Space.sm))
        Box(Modifier.weight(1f).height(1.dp).background(c.glassEdge))
        if (action != null && onAction != null) {
            Text(
                action,
                Modifier.clip(RoundedCornerShape(Radius.chip)).clickable(role = Role.Button, onClick = onAction)
                    .heightIn(min = 48.dp).padding(start = Space.sm, top = Space.sm, bottom = Space.sm),
                style = Ruvo.type.label, color = c.inkSoft,
            )
        }
    }
}

/** The main call to action: a haldi block stamped onto the page with a hard ink shadow. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val c = Ruvo.colors
    if (enabled) PillButton(text, onClick, modifier, background = c.fuel, content = c.onAccent, border = c.shadow, icon = icon, stamped = true)
    else PillButton(text, onClick, modifier, background = c.hairline, content = c.inkFaint, icon = icon, enabled = false)
}

/** A filled block in a specific accent, for bucket-specific actions. */
@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Ruvo.colors.fuel, icon: ImageVector? = null) {
    PillButton(text, onClick, modifier, background = accent, content = Ruvo.colors.onAccent, border = Ruvo.colors.shadow, icon = icon, stamped = true)
}

/** Ink block: a strong secondary action. */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Ruvo.colors
    PillButton(text, onClick, modifier, background = c.ink, content = c.canvas, icon = icon)
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Ruvo.colors
    PillButton(text, onClick, modifier, background = Color.Transparent, content = c.ink, border = c.ink, icon = icon)
}

/** Outlined block in any colour, e.g. paper on the ink ticket. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Ruvo.colors.onHero, icon: ImageVector? = null) {
    PillButton(text, onClick, modifier, background = Color.Transparent, content = color, border = color.copy(alpha = 0.8f), icon = icon)
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
    stamped: Boolean = false,
) {
    val c = Ruvo.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(Radius.chip)
    // Stamped buttons sit on a hard shadow; pressing pushes the button down onto it.
    val depth by animateDpAsState(if (stamped && !pressed) Radius.stamp else 0.dp, Motion.stamp(), label = "depth")
    val push = if (stamped) Radius.stamp - depth else 0.dp
    var m = modifier.padding(end = if (stamped) Radius.stamp else 0.dp, bottom = if (stamped) Radius.stamp else 0.dp)
        .offset(x = push, y = push).heightIn(min = 54.dp)
    if (stamped) m = m.hardShadow(shape, depth, c.shadow)
    m = m.clip(shape).background(background)
    if (border != null) m = m.border(1.5.dp, border, shape)
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
        Text(text, style = Ruvo.type.bodyStrong, color = content, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Selectable chip for categories, cadence and amounts. Selected chips are printed in ink. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Ruvo.colors.ink) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    val fill = if (selected) accent else c.surface
    val ink = when {
        !selected -> c.ink
        accent == c.ink -> c.canvas
        else -> c.onAccent
    }
    Box(
        modifier.heightIn(min = 48.dp).clip(shape)
            .background(fill)
            .border(1.dp, if (selected) accent else c.glassEdge, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Ruvo.type.label, color = ink)
    }
}

/** Small printed label, e.g. "Sample", "On track". */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: Color = Ruvo.colors.ink.copy(alpha = 0.08f), content: Color = Ruvo.colors.ink) {
    Text(
        text,
        modifier.clip(RoundedCornerShape(2.dp)).background(color).padding(horizontal = 8.dp, vertical = 3.dp),
        style = Ruvo.type.eyebrow, color = content,
    )
}

/** Square icon button with a 48 dp target. */
@Composable
fun IconCircle(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Ruvo.colors.ink, background: Color = Ruvo.colors.surface) {
    val shape = RoundedCornerShape(Radius.card)
    Box(
        modifier.size(48.dp).clip(shape).background(background).border(1.dp, Ruvo.colors.glassEdge, shape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(22.dp), tint = tint) }
}

/** Ledger tabs: an ink-ruled box with an ink block that slides under the chosen option. */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    BoxWithConstraints(modifier.fillMaxWidth().clip(shape).border(1.5.dp, c.ink, shape).padding(3.dp)) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, Motion.snappy(), label = "seg")
        Box(Modifier.offset(x = x).width(w).height(46.dp).clip(RoundedCornerShape(2.dp)).background(c.ink))
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier.width(w).height(46.dp)
                        .semantics { this.selected = on }
                        .clickable(role = Role.Tab, onClickLabel = "Show $label") { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = Ruvo.type.label, color = if (on) c.canvas else c.inkSoft, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Digits that roll to their new value instead of swapping, like a departure board. Each digit sits in its own slot
 * so the number never jitters sideways.
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
    Text(text, modifier.semantics { contentDescription = IndianFormat.spoken(paise) }, style = style, color = color, maxLines = 1)
}

/**
 * A ledger column for one bucket. All three share one layout and height: a small coloured icon block, the name
 * and the amount set in the serif. Colour is never the only cue.
 */
@Composable
fun BucketCard(bucket: Bucket, amount: Paise, caption: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = Ruvo.colors
    val accent = c.bucket(bucket)
    // Plain cards, no coloured edge: the coloured icon block and the name carry the meaning.
    RuvoCard(modifier.height(148.dp), onClick = onClick, onClickLabel = "Open ${bucketName(bucket)}", padding = PaddingValues(0.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(Radius.inner)).background(accent), contentAlignment = Alignment.Center) {
                Icon(RuvoIcons.forBucket(bucket), null, Modifier.size(18.dp), tint = c.onAccent)
            }
            Spacer(Modifier.height(Space.md))
            Eyebrow(bucketName(bucket), color = c.bucketInk(bucket))
            Spacer(Modifier.height(2.dp))
            Amount(amount, style = Ruvo.type.title.copy(fontFeatureSettings = "lnum, tnum"))
            Text(caption, style = Ruvo.type.caption, color = c.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

fun bucketName(b: Bucket) = when (b) {
    Bucket.FIXED -> "Fixed"
    Bucket.FUEL -> "Fuel"
    Bucket.FUTURE -> "Future"
}

fun bucketMeaning(b: Bucket) = when (b) {
    Bucket.FIXED -> "Already planned"
    Bucket.FUEL -> "Yours to spend"
    Bucket.FUTURE -> "Protected for goals"
}

/** A ledger row: leading tile, two lines of text, trailing content, a rule underneath. 64 dp tall. */
@Composable
fun ListRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = Ruvo.colors
    var m = modifier.fillMaxWidth().heightIn(min = 64.dp)
    if (onClick != null) m = m.clickable(role = Role.Button, onClick = onClick)
    Column(m) {
        Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(title, style = Ruvo.type.bodyStrong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = Ruvo.type.caption, color = c.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(Space.xs))
            trailing()
        }
        Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.dp).background(c.hairline))
    }
}

/** Letter tile for merchants: the first letter, in the serif, on a tinted square. */
@Composable
fun MerchantTile(name: String, accent: Color, size: Dp = 44.dp) {
    val c = Ruvo.colors
    Box(Modifier.size(size).clip(RoundedCornerShape(Radius.inner)).background(accent.copy(alpha = if (c.isDark) 0.24f else 0.28f)), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "·", style = Ruvo.type.title, color = c.ink)
    }
}

/** A flat progress bar with an animated fill. */
@Composable
fun FillBar(progress: Float, accent: Color, modifier: Modifier = Modifier, height: Dp = 8.dp, track: Color = Color.Unspecified) {
    val reduced = Ruvo.reducedMotion
    val c = Ruvo.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), if (reduced) spring(stiffness = Spring.StiffnessHigh) else Motion.gentle(), label = "fill")
    val trackColor = if (track != Color.Unspecified) track else c.ink.copy(alpha = if (c.isDark) 0.14f else 0.09f)
    val shape = RoundedCornerShape(2.dp)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(trackColor)) {
        Box(Modifier.fillMaxWidth(p).height(height).clip(shape).background(accent))
    }
}

/** One segment of a [SplitBar]. */
data class Segment(val fraction: Float, val color: Color)

/** A single bar split into printed segments that slide into place. */
@Composable
fun SplitBar(segments: List<Segment>, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val total = segments.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(1f)
    Row(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(2.dp)).background(c.ink.copy(alpha = if (c.isDark) 0.14f else 0.09f)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.filter { it.fraction > 0f }.forEachIndexed { i, s ->
            val grown = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(Unit) { delay(i * 90L); grown.animateTo(1f, Motion.gentle()) }
            Box(Modifier.weight((s.fraction / total * grown.value).coerceAtLeast(0.0001f)).fillMaxHeight().background(s.color))
        }
        val rest = 1f - segments.sumOf { it.fraction.toDouble() }.toFloat() / total
        if (rest > 0.001f) Spacer(Modifier.weight(rest))
    }
}

/** One bar in a [PillBars] chart. */
data class Pill(val label: String, val value: Float, val highlight: Boolean = false, val spoken: String = "")

/**
 * Square-shouldered bars that rise from a baseline one after another, over a ruled baseline.
 * The tallest is the biggest value. Used for daily spending and month-by-month comparisons.
 */
@Composable
fun PillBars(pills: List<Pill>, modifier: Modifier = Modifier, height: Dp = 120.dp, color: Color = Ruvo.colors.fuel, highlightColor: Color = Ruvo.colors.ink) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val max = pills.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            pills.forEachIndexed { i, pill ->
                val grow = remember { Animatable(if (reduced) 1f else 0f) }
                LaunchedEffect(pill.value) { delay(i * 45L); grow.animateTo(1f, Motion.soft()) }
                Box(
                    Modifier.weight(1f).height(height).semantics { contentDescription = pill.spoken.ifEmpty { "${pill.label}: ${pill.value.toInt()}" } },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val f = (pill.value / max).coerceIn(0.04f, 1f) * grow.value
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(f.coerceAtLeast(0.001f)).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (pill.highlight) highlightColor else color),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(c.ink))
        if (pills.any { it.label.isNotEmpty() }) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pills.forEach { pill ->
                    Text(pill.label, Modifier.weight(1f), style = Ruvo.type.caption, color = if (pill.highlight) c.ink else c.inkFaint, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Square swatch plus label, for chart legends. */
@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = Ruvo.type.caption, color = Ruvo.colors.inkSoft)
    }
}
