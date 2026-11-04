package com.ruvo.feature.guide

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.ruvo.design.Motion
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoIcons
import com.ruvo.design.RuvoSheet
import com.ruvo.design.SegmentedControl
import com.ruvo.design.Space
import com.ruvo.engine.guide.Library

/**
 * Your shelf at a glance: one spine per book, in shelf order. Read books stand tall in amber, unread ones are
 * short outlines, so you can see what is done and what is left without counting.
 */
@Composable
fun ReadingShelf(read: Set<String>, modifier: Modifier = Modifier) {
    val c = Ruvo.colors
    val books = Library.all
    Row(
        modifier.fillMaxWidth().height(34.dp).clearAndSetSemantics {
            contentDescription = "${books.count { it.id in read }} of ${books.size} books read"
        },
        horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom,
    ) {
        books.forEach { b ->
            val done = b.id in read
            val h by animateFloatAsState(if (done) 1f else 0.62f, Motion.bouncy(), label = "spine${b.id}")
            Box(
                Modifier.weight(1f).height(34.dp * h).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .then(
                        if (done) Modifier.background(c.fuel)
                        else Modifier.border(1.dp, c.onHero.copy(alpha = 0.35f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                    ),
            )
        }
    }
}

/** Every book, split into what is still to read and what you have read, grouped by shelf. Tap one to open it. */
@Composable
fun ReadingSheet(read: Set<String>, onOpen: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Ruvo.colors
    val left = Library.all.filter { it.id !in read }
    val done = Library.all.filter { it.id in read }
    var tab by rememberSaveable { mutableIntStateOf(if (left.isEmpty()) 1 else 0) }
    RuvoSheet("Your reading", onDismiss, eyebrow = "${done.size} of ${Library.all.size} read") {
        SegmentedControl(listOf("To read · ${left.size}", "Read · ${done.size}"), tab, { tab = it })
        Spacer(Modifier.height(Space.md))
        val list = if (tab == 0) left else done
        if (list.isEmpty()) {
            Text(
                if (tab == 0) "You've read every book. New ones arrive in New this week."
                else "Nothing read yet. Most books take about 4 minutes; start with one that looks useful.",
                style = Ruvo.type.body, color = c.inkSoft,
            )
        }
        list.groupBy { it.shelf }.forEach { (shelf, books) ->
            Text(shelf.title, style = Ruvo.type.label, color = c.inkSoft)
            Spacer(Modifier.height(Space.xxs))
            books.forEach { b -> BookLine(b, b.id in read) { onDismiss(); onOpen(b.id) } }
            Spacer(Modifier.height(Space.sm))
        }
    }
}

@Composable
private fun BookLine(book: Library.Book, read: Boolean, onClick: () -> Unit) {
    val c = Ruvo.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(Radius.inner))
            .clickable(role = Role.Button, onClickLabel = if (read) "Read ${book.title} again" else "Read ${book.title}", onClick = onClick)
            .padding(vertical = Space.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book, read, width = 34.dp, showLabel = false)
        Spacer(Modifier.width(Space.sm))
        Column(Modifier.weight(1f)) {
            Text(book.title, style = Ruvo.type.bodyStrong, color = c.ink, maxLines = 1)
            Text("${book.minutes} min read", style = Ruvo.type.caption, color = c.inkFaint)
        }
        if (read) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(Radius.chip)).background(c.future.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(RuvoIcons.Check, "Read", Modifier.size(16.dp), tint = c.futureInk)
            }
        } else {
            Text("Read", Modifier.graphicsLayer { alpha = 0.9f }, style = Ruvo.type.label, color = c.brandInk)
        }
    }
}
