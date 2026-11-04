package com.ruvo.feature.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.ActivityItem
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Chip
import com.ruvo.design.Eyebrow
import com.ruvo.design.GhostButton
import com.ruvo.design.Haptics
import com.ruvo.design.IconCircle
import com.ruvo.design.Motion
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoIcons
import com.ruvo.design.Space
import com.ruvo.design.bucketName
import com.ruvo.engine.Bucket
import com.ruvo.engine.Direction
import com.ruvo.engine.IndianFormat
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class Fling { CONFIRM, IGNORE, SHARE }

/**
 * The weekly sort: the week's detected payments as a card stack.
 * Right = confirm, left = ignore, up = mark shared, tap a bucket chip to re-file. Cards follow your finger
 * and fly off at your swipe speed. Every gesture also has a button and a TalkBack action.
 */
@Composable
fun SortStackScreen(onClose: () -> Unit, onShare: (String) -> Unit) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val stack = state.toSort
    var sorted by remember { mutableIntStateOf(0) }
    val repo = RuvoGraph.repository

    fun act(item: ActivityItem, f: Fling) {
        sorted++
        when (f) {
            Fling.CONFIRM -> { Haptics.tick(view); scope.launch { repo.confirm(item.txn.id) } }
            Fling.IGNORE -> { Haptics.tick(view); scope.launch { repo.ignore(item.txn.id) } }
            Fling.SHARE -> { Haptics.split(view); scope.launch { repo.confirm(item.txn.id) }; onShare(item.txn.id) }
        }
    }

    Column(Modifier.fillMaxSize().background(c.canvas).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(Space.gutter), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Weekly sort")
                Text(if (stack.isEmpty()) "All sorted" else "${stack.size} to go", style = Ruvo.type.headline, color = c.ink)
            }
            IconCircle(RuvoIcons.Close, "Close the sort", onClose)
        }
        if (stack.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(Space.gutter), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your week is sorted.", style = Ruvo.type.display, color = c.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(Space.sm))
                val r = state.runway
                Text(
                    if (sorted > 0) "You went through $sorted ${if (sorted == 1) "payment" else "payments"}. ${runwaySubline(r)}." else runwaySubline(r),
                    style = Ruvo.type.body, color = c.inkSoft, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Space.xl))
                PrimaryButton("Back to Today", onClose)
            }
            return@Column
        }
        Text(
            "Right to confirm · left to ignore · up if it was shared",
            Modifier.fillMaxWidth().padding(horizontal = Space.gutter), style = Ruvo.type.caption, color = c.inkFaint, textAlign = TextAlign.Center,
        )
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(Space.gutter), contentAlignment = Alignment.Center) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            stack.take(3).reversed().forEachIndexed { i, item ->
                val depth = stack.take(3).size - 1 - i
                key(item.txn.id) {
                    SwipeCard(item, depth, widthPx, state.today, onFling = { act(item, it) }) { b -> scope.launch { repo.setBucket(item.txn.id, b) } }
                }
            }
        }
        val top = stack.first()
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            GhostButton("Ignore", { act(top, Fling.IGNORE) }, Modifier.weight(1f))
            GhostButton("Shared", { act(top, Fling.SHARE) }, Modifier.weight(1f), icon = RuvoIcons.Split)
            PrimaryButton("Confirm", { act(top, Fling.CONFIRM) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SwipeCard(
    item: ActivityItem,
    depth: Int,
    widthPx: Float,
    today: java.time.LocalDate,
    onFling: (Fling) -> Unit,
    onBucket: (Bucket) -> Unit,
) {
    val c = Ruvo.colors
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val reduced = Ruvo.reducedMotion
    val t = item.txn
    val credit = t.direction == Direction.CREDIT
    val shape = RoundedCornerShape(Radius.card + 4.dp)
    val drag = offset.value
    val hint = when {
        drag.x > 80f -> "Confirm"
        drag.x < -80f -> "Ignore"
        drag.y < -80f -> "Shared"
        else -> null
    }
    val gestures = if (depth == 0) Modifier.pointerInput(t.id) {
        detectDragGestures(
            onDragEnd = {
                val o = offset.value
                val f = when {
                    o.x > widthPx * 0.28f -> Fling.CONFIRM
                    o.x < -widthPx * 0.28f -> Fling.IGNORE
                    o.y < -widthPx * 0.3f -> Fling.SHARE
                    else -> null
                }
                scope.launch {
                    if (f == null) offset.animateTo(Offset.Zero, Motion.bouncy())
                    else {
                        val target = when (f) {
                            Fling.CONFIRM -> Offset(widthPx * 1.6f, o.y)
                            Fling.IGNORE -> Offset(-widthPx * 1.6f, o.y)
                            Fling.SHARE -> Offset(o.x, -widthPx * 2.2f)
                        }
                        if (!reduced) offset.animateTo(target, Motion.snappy())
                        onFling(f)
                    }
                }
            },
        ) { change, amount ->
            change.consume()
            scope.launch { offset.snapTo(offset.value + amount) }
        }
    } else Modifier

    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer {
                translationX = drag.x
                translationY = drag.y + depth * 18.dp.toPx()
                rotationZ = (drag.x / widthPx) * 12f
                val s = 1f - depth * 0.05f
                scaleX = s; scaleY = s
                alpha = if (depth > 1) 0.6f else 1f
            }
            .then(gestures)
            .clip(shape).background(c.raised).border(1.dp, c.hairline, shape)
            .semantics {
                contentDescription = "${t.merchant}, ${IndianFormat.spoken(t.amount)}, ${IndianFormat.relativeDay(t.date, today)}"
                customActions = listOf(
                    CustomAccessibilityAction("Confirm") { onFling(Fling.CONFIRM); true },
                    CustomAccessibilityAction("Ignore") { onFling(Fling.IGNORE); true },
                    CustomAccessibilityAction("Mark shared") { onFling(Fling.SHARE); true },
                )
            }
            .padding(Space.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(if (credit) "Money in" else "Payment", Modifier.weight(1f))
            if (hint != null) {
                Text(
                    hint,
                    Modifier.clip(RoundedCornerShape(Radius.chip)).background(
                        when (hint) { "Confirm" -> c.fuel; "Ignore" -> c.inkFaint; else -> c.fixed },
                    ).padding(horizontal = Space.sm, vertical = 4.dp),
                    style = Ruvo.type.label, color = c.onAccent,
                )
            }
        }
        Spacer(Modifier.height(Space.lg))
        com.ruvo.design.MerchantLogo(t.merchant, t.category, size = 52.dp)
        Spacer(Modifier.height(Space.sm))
        Text((if (credit) "+" else "") + IndianFormat.rupees(t.amount), style = Ruvo.type.display, color = if (credit) c.fuel else c.ink)
        Text(t.merchant, style = Ruvo.type.headline, color = c.ink)
        Text("${IndianFormat.relativeDay(t.date, today)} · ${t.category}", style = Ruvo.type.body, color = c.inkSoft)
        Spacer(Modifier.height(Space.xl))
        if (!credit) {
            Eyebrow("Which bucket?")
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Bucket.entries.forEach { b -> Chip(bucketName(b), t.bucket == b, { onBucket(b) }, accent = c.bucket(b)) }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Box(Modifier.fillMaxWidth()) {
            Text(
                if (abs(drag.x) + abs(drag.y) < 4f) "Drag the card, or use the buttons below" else " ",
                style = Ruvo.type.caption, color = c.inkFaint,
            )
        }
    }
}
