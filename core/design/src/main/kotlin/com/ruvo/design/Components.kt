package com.ruvo.design

import androidx.compose.animation.AnimatedContent
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
 * Mature motion: things settle rather than bounce, except where something is physically pulled.
 */
object Motion {
    fun <T> soft() = spring<T>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    fun <T> snappy() = spring<T>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
    /** Slow and calm, for entrances and large surfaces. */
    fun <T> gentle() = spring<T>(dampingRatio = 0.9f, stiffness = 140f)

    /** A firm landing with one small rebound. */
    fun <T> stamp() = spring<T>(dampingRatio = 0.5f, stiffness = 900f)

    /** Gap between items in a staggered entrance. */
    const val STAGGER_MS = 55L
}

/** A hard, offset shadow in one flat colour. Draw it before clipping. */
fun Modifier.hardShadow(shape: androidx.compose.ui.graphics.Shape, offset: Dp, color: Color): Modifier = this.then(
    Modifier.drawBehind {
        val o = offset.toPx()
        if (o > 0f) translate(o, o) { drawOutline(shape.createOutline(size, layoutDirection, this), color) }
    },
)

/**
 * A soft, wide shadow drawn only outside a rounded shape, so it never greys the frosted glass on top of it. A few
 * stacked rings fade out from the edge, which reads like a blur without needing one. Draw it before clipping.
 */
fun Modifier.softShadow(radius: Dp, color: Color, spread: Dp = 14.dp, drop: Dp = 6.dp): Modifier = this.then(
    Modifier.drawBehind {
        val r = radius.toPx().coerceAtMost(size.minDimension / 2f)
        val outline = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(r)))
        }
        val s = spread.toPx()
        val y = drop.toPx()
        val steps = 7
        clipPath(outline, androidx.compose.ui.graphics.ClipOp.Difference) {
            for (i in 1..steps) {
                val e = s * i / steps
                drawRoundRect(
                    color.copy(alpha = color.alpha / steps * 0.85f),
                    Offset(-e * 0.8f, -e * 0.5f + y),
                    androidx.compose.ui.geometry.Size(size.width + e * 1.6f, size.height + e),
                    androidx.compose.ui.geometry.CornerRadius(r + e),
                )
            }
        }
    },
)

/** Scales down slightly while pressed, springing back on release. */
@Composable
fun Modifier.pressable(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val reduced = Ruvo.reducedMotion
    val scale by animateFloatAsState(if (pressed && !reduced) 0.975f else 1f, Motion.snappy(), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Staggered entrance: the element rises 18 dp and fades in, a beat after the one before it.
 * Runs once when the element first appears. Reduced motion shows it immediately.
 */
fun Modifier.reveal(index: Int = 0): Modifier = composed {
    val reduced = Ruvo.reducedMotion
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    val rise = with(LocalDensity.current) { 18.dp.toPx() }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(index.coerceIn(0, 12) * Motion.STAGGER_MS)
        launch { progress.animateTo(1f, Motion.gentle()) }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * rise
        val s = 0.985f + 0.015f * p
        scaleX = s; scaleY = s
    }
}

/**
 * Ruvo's card. By default it is frosted glass over the glowing canvas with a light top edge.
 * Pass [color] or [brush] for a filled card; pass `border = null` for no edge.
 */
@Composable
fun RuvoCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    border: Color? = Color.Unspecified,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(Space.md),
    brush: Brush? = null,
    elevated: Boolean = false,
    /** The coloured light that drifts inside a [brush] card. Defaults to brand blue on deep cards, white on pale ones. */
    glow: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ruvo.colors
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(Radius.card)
    val glassCard = brush == null && color == Color.Unspecified
    var m = modifier.pressable(interaction)
    // Glass floats on a soft blue shadow by day (drawn only outside the card, so the frost stays clean); at night
    // the glass needs none. Filled cards that ask to be raised get a coloured shadow.
    if (glassCard && !c.isDark) m = m.softShadow(Radius.card, c.shadow)
    else if (elevated) m = m.shadow(18.dp, shape, ambientColor = c.shadow, spotColor = c.shadow)
    m = m.clip(shape)
    m = when {
        // Gradient cards glow like the bucket boxes: two soft lights drift inside, light sweeps across now
        // and then, and a glassy white edge catches the top. Reduced motion keeps the lights still.
        brush != null -> {
            val reduced = Ruvo.reducedMotion
            val loop = rememberInfiniteTransition(label = "cardGlow")
            val t by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(11_000, easing = LinearEasing)), label = "drift")
            val sweep by loop.animateFloat(-0.6f, 2.4f, infiniteRepeatable(tween(8_000, easing = LinearEasing)), label = "sweep")
            // Deep cards (the blue brand cards, and the bucket cards at night) get gentle lights; pale cards (the
            // yellow Marketplace, the pearly review, Daybreak's pastels) get brighter ones.
            val pale = brush == c.marketBrush || brush == c.irisBrush
            val deep = !pale && (c.isDark || brush == c.heroBrush || brush == c.brandBrush)
            val second = glow ?: if (deep) c.brand else Color.White
            m.background(brush).drawWithContent {
                val a = (if (reduced) 0.2f else t) * 2f * PI.toFloat()
                val r = size.maxDimension * 0.5f
                val one = Offset(size.width * (0.85f + 0.08f * cos(a)), size.height * (0.15f + 0.12f * sin(a)))
                val two = Offset(size.width * (0.12f + 0.08f * sin(a)), size.height * (0.9f + 0.08f * cos(a)))
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = if (deep) 0.12f else 0.55f), Color.Transparent), one, r), r, one)
                drawCircle(Brush.radialGradient(listOf(second.copy(alpha = if (deep) 0.3f else 0.4f), Color.Transparent), two, r), r, two)
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (deep) 0.08f else 0.3f), Color.Transparent), endY = size.height * 0.4f))
                if (!reduced) {
                    val x = size.width * sweep
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = if (deep) 0.08f else 0.24f), Color.Transparent), Offset(x - size.width * 0.3f, 0f), Offset(x + size.width * 0.3f, size.height)))
                }
                drawContent()
            }.border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = if (deep) 0.24f else 0.9f), Color.White.copy(alpha = 0.05f))), shape)
        }
        color != Color.Unspecified -> m.background(color)
        // Frosted glass: milkier at the top where light falls, with a bright line along the top edge.
        else -> m.background(Brush.verticalGradient(listOf(c.glassTop, c.glass)))
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = if (c.isDark) 0.22f else 1f), Color.Transparent)),
                    size = androidx.compose.ui.geometry.Size(size.width, 1.5.dp.toPx()),
                )
            }
    }
    when {
        border == null -> {}
        border == Color.Unspecified && glassCard ->
            m = m.border(BorderStroke(1.dp, Brush.verticalGradient(listOf(c.glassEdge, c.glassEdge.copy(alpha = c.glassEdge.alpha * 0.3f)))), shape)
        border == Color.Unspecified -> {}
        else -> m = m.border(BorderStroke(1.dp, border), shape)
    }
    if (onClick != null) m = m.clickable(interaction, indication = null, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Ruvo.colors.inkSoft) {
    // Sentence case as written: "Daily spending", not "DAILY SPENDING". Names and months keep their capitals.
    Text(text, modifier, style = Ruvo.type.eyebrow, color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
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

/** The main call to action: a brand-blue pill that glows a little, with white text. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val c = Ruvo.colors
    if (enabled) {
        PillButton(text, onClick, modifier, background = Color.Transparent, brush = c.brandBrush, content = c.onBrand, icon = icon, glow = c.brand)
    } else {
        PillButton(text, onClick, modifier, background = c.hairline, content = c.inkFaint, icon = icon, enabled = false)
    }
}

/** A filled pill in a specific accent, for bucket-specific actions. */
@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Ruvo.colors.fuel, icon: ImageVector? = null) {
    PillButton(text, onClick, modifier, background = accent, content = if (accent.luminance() < 0.45f) Color.White else Color(0xFF141B4D), icon = icon)
}

/** Navy pill (white in Night swim): a strong secondary action. */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Ruvo.colors
    PillButton(text, onClick, modifier, background = c.ink, content = if (c.isDark) c.canvas else c.surface, icon = icon)
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Ruvo.colors
    PillButton(text, onClick, modifier, background = Color.Transparent, content = c.ink, border = c.ink.copy(alpha = 0.85f), icon = icon)
}

/** Outlined pill in any colour, e.g. white on the navy hero. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Ruvo.colors.onHero, icon: ImageVector? = null) {
    PillButton(text, onClick, modifier, background = Color.Transparent, content = color, border = color.copy(alpha = 0.7f), icon = icon)
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
    brush: Brush? = null,
    glow: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(Radius.chip)
    val lift by animateDpAsState(if (pressed) 3.dp else 12.dp, Motion.snappy(), label = "lift")
    var m = modifier.pressable(interaction).heightIn(min = 50.dp)
    if (glow != null) m = m.shadow(lift, shape, ambientColor = glow, spotColor = glow)
    m = m.clip(shape).background(background)
    if (brush != null) m = m.background(brush)
    if (border != null) m = m.border(1.5.dp, border, shape)
    Row(
        m.clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.lg, vertical = Space.sm),
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

/**
 * Selectable chip used for categories, cadence, amounts. Unselected it is a white glass pill; selected it fills with
 * ink (navy by day, white at night) or with [accent], e.g. yellow in the Marketplace or a bucket's colour.
 */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Ruvo.colors.ink) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    val fill = if (selected) accent else c.glassTop
    val text_ = when {
        !selected -> c.ink
        accent.luminance() < 0.45f -> Color.White
        else -> Color(0xFF141B4D)
    }
    Box(
        modifier.heightIn(min = 48.dp).clip(shape)
            .background(fill)
            .border(1.dp, if (selected) fill else if (c.isDark) Color.White.copy(alpha = 0.14f) else c.hairline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.md, vertical = Space.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Ruvo.type.label, color = text_)
    }
}

/** A quiet status label with a coloured dot, e.g. "On track". The dot carries the colour; the pill stays calm. */
@Composable
fun StatusTag(text: String, dot: Color, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    Row(
        modifier.clip(RoundedCornerShape(Radius.chip)).background(c.ink.copy(alpha = if (c.isDark) 0.1f else 0.06f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(text, style = Ruvo.type.label, color = c.ink, maxLines = 1)
    }
}

/** Small rounded label, e.g. "Sample", "On track". */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: Color = Ruvo.colors.ink.copy(alpha = 0.08f), content: Color = Ruvo.colors.ink) {
    Text(
        text,
        modifier.clip(RoundedCornerShape(Radius.chip)).background(color).padding(horizontal = 10.dp, vertical = 4.dp),
        style = Ruvo.type.eyebrow, color = content,
    )
}

/** Round icon button with a 48 dp target. */
@Composable
fun IconCircle(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Ruvo.colors.ink, background: Color = Ruvo.colors.glass) {
    Box(
        modifier.size(48.dp).clip(CircleShape).background(background).border(1.dp, Ruvo.colors.glassEdge, CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(22.dp), tint = tint) }
}

/**
 * A pill segmented control on a frosted track. The chosen option is a white pill that slides across on a spring,
 * with navy text, in both moods.
 */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val shape = RoundedCornerShape(Radius.chip)
    BoxWithConstraints(
        modifier.fillMaxWidth().clip(shape)
            .background(if (c.isDark) Color.White.copy(alpha = 0.08f) else c.ink.copy(alpha = 0.07f))
            .border(1.dp, if (c.isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.7f), shape)
            .padding(4.dp),
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, Motion.snappy(), label = "seg")
        Box(
            Modifier.offset(x = x).width(w).height(48.dp)
                .shadow(if (c.isDark) 0.dp else 6.dp, shape, ambientColor = c.shadow, spotColor = c.shadow)
                .clip(shape).background(Color.White),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier.width(w).height(48.dp).clip(shape)
                        .semantics { this.selected = on }
                        .clickable(role = Role.Tab, onClickLabel = "Show $label") { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = Ruvo.type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(600)), color = if (on) c.onLight else c.inkSoft, maxLines = 1, softWrap = false)
                }
            }
        }
    }
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
fun Amount(paise: Paise, modifier: Modifier = Modifier, style: TextStyle = Ruvo.type.amount, color: Color = Ruvo.colors.ink, signed: Boolean = false, countUp: Boolean = false) {
    // Big amounts count up from zero when they first appear, easing out over 0.8 s.
    val reduced = Ruvo.reducedMotion
    val shown = remember { Animatable(if (countUp && !reduced) 0f else 1f) }
    LaunchedEffect(Unit) { if (countUp && !reduced) shown.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    val value = if (shown.value >= 1f) paise else (paise * shown.value).toLong() / 100 * 100
    val text = (if (signed && paise > 0) "+" else "") + IndianFormat.rupees(value)
    Text(text, modifier.semantics { contentDescription = IndianFormat.spoken(paise) }, style = style, color = color, maxLines = 1)
}

/** One chip in the status row. */
data class StatusItem(val label: String, val dot: Color, val onClick: () -> Unit)

/**
 * Status chips that glide past like a ticker, about 28 dp a second, looping forever. Each has a pulsing coloured
 * dot and catches a slow shimmer. Dragging pauses the glide; tapping a chip opens what it describes. With reduced
 * motion the row stands still and scrolls by hand.
 */
@Composable
fun StatusChips(items: List<StatusItem>, modifier: Modifier = Modifier) {
    val reduced = Ruvo.reducedMotion
    if (items.isEmpty()) return
    // A very long list that repeats the chips, started in the middle so it can glide on without running out.
    val loopCount = if (reduced) items.size else Int.MAX_VALUE
    val list = rememberLazyListState(initialFirstVisibleItemIndex = if (reduced) 0 else (Int.MAX_VALUE / 2).let { it - it % items.size })
    val speed = with(LocalDensity.current) { 28.dp.toPx() }
    LaunchedEffect(reduced, items.size) {
        if (reduced) return@LaunchedEffect
        var last = 0L
        while (true) {
            val now = withInfiniteAnimationFrameNanos { it }
            if (last != 0L && !list.isScrollInProgress) list.dispatchRawDelta(speed * ((now - last) / 1_000_000_000f).coerceAtMost(0.05f))
            last = now
        }
    }
    // Frosted glass pills: milky white on Daybreak, smoky at night, each with a bright edge.
    val c = Ruvo.colors
    val dark = c.isDark
    val fill = if (dark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.82f)
    val text = c.ink
    val edge = if (dark) Color.White.copy(alpha = 0.18f) else Color.White
    val shine = if (dark) Color.White.copy(alpha = 0.16f) else Color.White
    val loop = rememberInfiniteTransition(label = "chips")
    val t by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "t")
    val sweep by loop.animateFloat(-0.6f, 1.6f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "sweep")
    LazyRow(
        // Offscreen first, so the edge fade erases only the chips and never the screen behind them.
        modifier.fillMaxWidth().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
            // Chips fade in and out at the screen edges instead of being cut off.
            drawContent()
            val fade = 28.dp.toPx()
            drawRect(Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), endX = fade), blendMode = BlendMode.DstOut)
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = size.width - fade), blendMode = BlendMode.DstOut)
        },
        state = list, horizontalArrangement = Arrangement.spacedBy(Space.xs),
        contentPadding = PaddingValues(horizontal = if (reduced) Space.gutter else 0.dp, vertical = 8.dp),
    ) {
        items(loopCount) { n ->
            val i = n % items.size
            val item = items[i]
            val pulse = if (reduced) 0f else (t * 1.55f + i * 0.2f) % 1f
            val shape = RoundedCornerShape(Radius.chip)
            Row(
                Modifier
                    .then(if (dark) Modifier else Modifier.softShadow(Radius.chip, c.shadow, spread = 7.dp, drop = 3.dp))
                    .clip(shape).background(fill).border(1.dp, edge, shape)
                    .clickable(role = Role.Button, onClickLabel = "Open ${item.label}", onClick = item.onClick)
                    .drawWithContent {
                        drawContent()
                        if (!reduced) {
                            val x = size.width * (((sweep + i * 0.35f) % 2.2f) - 0.6f)
                            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, shine, Color.Transparent), startX = x, endX = x + size.width * 0.4f))
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(8.dp + 8.dp * pulse).clip(CircleShape).background(item.dot.copy(alpha = (1f - pulse) * 0.55f)))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(item.dot))
                }
                Spacer(Modifier.width(6.dp))
                Text(item.label, style = Ruvo.type.label, color = text, maxLines = 1)
            }
        }
    }
}

/**
 * The three bucket cards share one layout and one height. Each is filled with its bucket's colour and still
 * carries its name, so colour is never the only cue.
 */
@Composable
fun BucketCard(bucket: Bucket, amount: Paise, caption: String, modifier: Modifier = Modifier, share: Float = 0f, onClick: (() -> Unit)? = null) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val ink = c.onAccent
    val loop = rememberInfiniteTransition(label = "bucket")
    val drift by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(9000 + bucket.ordinal * 1700, easing = LinearEasing)), label = "drift")
    val shape = RoundedCornerShape(Radius.card)
    Box(
        modifier.height(118.dp)
            .shadow(if (c.isDark) 0.dp else 10.dp, shape, ambientColor = c.bucket(bucket), spotColor = c.bucket(bucket))
            .clip(shape).background(c.bucketBrush(bucket))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = if (c.isDark) 0.14f else 0.9f), Color.White.copy(alpha = if (c.isDark) 0.03f else 0.15f))), shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Open ${bucketName(bucket)}", onClick = onClick) else Modifier),
    ) {
        // A soft blob of the bucket's own colour drifting in the corner, like liquid behind glass. At night it is
        // only a faint ember, so three boxes side by side never glare.
        Canvas(Modifier.matchParentSize()) {
            val t = if (reduced) 0.3f else drift
            val a = t * 2f * PI.toFloat()
            val centre = Offset(size.width * (0.82f + 0.08f * cos(a)), size.height * (0.18f + 0.1f * sin(a)))
            drawCircle(Brush.radialGradient(listOf(c.bucket(bucket).copy(alpha = if (c.isDark) 0.16f else 0.55f), Color.Transparent), centre, size.width * 0.55f), size.width * 0.55f, centre)
            drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (c.isDark) 0.04f else 0.35f), Color.Transparent), endY = size.height * 0.45f))
        }
        Column(Modifier.matchParentSize().padding(12.dp)) {
            // Name first, then the number: the word carries the meaning, so no icon is needed. At night the name
            // carries the bucket's colour and the number stays plain white.
            Text(bucketName(bucket), style = Ruvo.type.bodyStrong, color = if (c.isDark) c.bucketInk(bucket) else ink, maxLines = 1)
            Text(caption, style = Ruvo.type.caption, color = ink.copy(alpha = if (c.isDark) 0.55f else 0.68f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Amount(amount, style = Ruvo.type.title, color = ink, countUp = true)
            Spacer(Modifier.height(6.dp))
            // Its share of everything you have, so the three boxes compare at a glance.
            FillBar(share, c.bucket(bucket).copy(alpha = 1f), height = 5.dp, track = Color.White.copy(alpha = if (c.isDark) 0.08f else 0.55f))
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
            Text(title, style = Ruvo.type.bodyStrong, color = Ruvo.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Ruvo.type.caption, color = Ruvo.colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(Space.xs))
        trailing()
    }
}

/** A rounded progress bar with an animated fill. */
@Composable
fun FillBar(progress: Float, accent: Color, modifier: Modifier = Modifier, height: Dp = 10.dp, track: Color = Color.Unspecified) {
    val reduced = Ruvo.reducedMotion
    val c = Ruvo.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), if (reduced) spring(stiffness = Spring.StiffnessHigh) else Motion.gentle(), label = "fill")
    val trackColor = if (track != Color.Unspecified) track else c.ink.copy(alpha = if (c.isDark) 0.14f else 0.08f)
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(Radius.chip)).background(trackColor)) {
        Box(Modifier.fillMaxWidth(p).height(height).clip(RoundedCornerShape(Radius.chip)).background(accent))
    }
}

/** One segment of a [SplitBar]. */
data class Segment(val fraction: Float, val color: Color)

/** A single bar split into coloured segments that grow into place. */
@Composable
fun SplitBar(segments: List<Segment>, modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val total = segments.sumOf { it.fraction.toDouble() }.toFloat().coerceAtLeast(1f)
    Row(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(Radius.chip)).background(c.ink.copy(alpha = if (c.isDark) 0.14f else 0.08f)),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        segments.filter { it.fraction > 0f }.forEachIndexed { i, s ->
            val grown = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(Unit) { delay(i * 90L); grown.animateTo(1f, Motion.gentle()) }
            Box(Modifier.weight((s.fraction / total * grown.value).coerceAtLeast(0.0001f)).fillMaxHeight().clip(RoundedCornerShape(Radius.chip)).background(s.color))
        }
        val rest = 1f - segments.sumOf { it.fraction.toDouble() }.toFloat() / total
        if (rest > 0.001f) Spacer(Modifier.weight(rest))
    }
}

/** One pill in a [PillBars] chart. */
data class Pill(val label: String, val value: Float, val highlight: Boolean = false, val spoken: String = "")

/**
 * Rounded pills that grow from the bottom one after another. The tallest is the biggest value.
 * Used for daily spending and month-by-month comparisons.
 */
@Composable
fun PillBars(
    pills: List<Pill>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    color: Color = Ruvo.colors.brand,
    highlightColor: Color = Ruvo.colors.ink,
    /** When set, pills are tappable: the chosen one is solid with its [tooltip] above, the rest are faded. */
    selected: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    tooltip: ((Int) -> String)? = null,
) {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val max = pills.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        pills.forEachIndexed { i, pill ->
            val grow = remember { Animatable(if (reduced) 1f else 0f) }
            LaunchedEffect(pill.value) { delay(i * 45L); grow.animateTo(1f, Motion.soft()) }
            val chosen = selected == i
            val fill = when {
                onSelect == null -> if (pill.highlight) highlightColor else color
                chosen -> color
                // Only the chosen day carries colour; the rest stay a quiet grey so they never look muddy.
                else -> c.ink.copy(alpha = if (c.isDark) 0.2f else 0.14f)
            }
            val press by animateFloatAsState(if (chosen) 1f else 0.97f, Motion.snappy(), label = "pill$i")
            var col = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = pill.spoken.ifEmpty { "${pill.label}: ${pill.value.toInt()}" } }
            if (onSelect != null) col = col.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClickLabel = "Show ${pill.spoken}") { onSelect(i) }
            Column(col, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(Radius.chip)).background(c.ink.copy(alpha = if (c.isDark) 0.08f else 0.05f)))
                    val f = (pill.value / max).coerceIn(0.06f, 1f) * grow.value
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(f.coerceAtLeast(0.001f)).graphicsLayer { scaleY = press; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }
                            .clip(RoundedCornerShape(Radius.chip)).background(fill),
                    )
                    if (chosen && tooltip != null) {
                        // The amount floats above the chosen pill and may be wider than it.
                        Text(
                            tooltip(i),
                            Modifier.align(Alignment.TopCenter).wrapContentWidth(unbounded = true)
                                .clip(RoundedCornerShape(Radius.chip)).background(c.ink).padding(horizontal = 8.dp, vertical = 4.dp),
                            style = Ruvo.type.label, color = if (c.isDark) c.canvas else c.surface, maxLines = 1, softWrap = false,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    pill.label, style = if (chosen) Ruvo.type.label else Ruvo.type.caption,
                    color = if (pill.highlight || chosen) c.ink else c.inkFaint, maxLines = 1,
                )
            }
        }
    }
}

/** Dot plus label, for chart legends. */
@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = Ruvo.type.caption, color = Ruvo.colors.inkSoft)
    }
}

/** The review tile's words, shared by Home and Goals so it reads the same in both places. */
const val REVIEW_TITLE = "Your review"
const val REVIEW_LINE = "This week and this month: what came in, what went out, what you protected."

/** The four looks a [BigTile] can take. Each one always means the same thing, wherever it appears. */
enum class TileStyle {
    /** Sunny yellow: the Marketplace, everywhere. */
    MARKET,
    /** Royal blue: Before you buy. */
    BRAND,
    /** Pearly pastel: your review. */
    IRIS,
    /** White glass: places like Settings. */
    GLASS,
}

/**
 * A big tile that opens a place. The same tile in the same colour is used wherever that place is offered, so the
 * Marketplace is always yellow, Before you buy always blue and the review always pearly.
 */
@Composable
fun BigTile(title: String, line: String, icon: ImageVector, modifier: Modifier = Modifier, style: TileStyle = TileStyle.MARKET, onClick: () -> Unit) {
    val c = Ruvo.colors
    val fg = when (style) {
        TileStyle.MARKET, TileStyle.IRIS -> c.onLight
        TileStyle.BRAND -> c.onHero
        TileStyle.GLASS -> c.ink
    }
    val brush = when (style) {
        TileStyle.MARKET -> c.marketBrush
        TileStyle.IRIS -> c.irisBrush
        TileStyle.BRAND -> c.heroBrush
        TileStyle.GLASS -> null
    }
    val badge = when (style) {
        TileStyle.MARKET, TileStyle.IRIS -> Color.White.copy(alpha = 0.6f)
        TileStyle.BRAND -> Color.White.copy(alpha = 0.16f)
        TileStyle.GLASS -> c.ink.copy(alpha = if (c.isDark) 0.1f else 0.06f)
    }
    RuvoCard(
        modifier.fillMaxWidth(), brush = brush,
        border = if (style == TileStyle.GLASS) Color.Unspecified else null, elevated = style != TileStyle.GLASS,
        glow = if (style == TileStyle.MARKET) Color(0xFFFF8A45) else null,
        onClick = onClick, onClickLabel = "Open $title", padding = PaddingValues(Space.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(badge), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = fg)
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(title, style = Ruvo.type.title, color = fg)
                Text(line, style = Ruvo.type.caption, color = fg.copy(alpha = 0.78f))
            }
            Spacer(Modifier.width(Space.xs))
            Box(Modifier.size(32.dp).clip(CircleShape).background(badge), contentAlignment = Alignment.Center) {
                Icon(RuvoIcons.Forward, null, Modifier.size(16.dp), tint = fg)
            }
        }
    }
}
