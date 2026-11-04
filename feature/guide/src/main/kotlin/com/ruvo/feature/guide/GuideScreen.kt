package com.ruvo.feature.guide

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruvo.data.RuvoGraph
import com.ruvo.design.Eyebrow
import com.ruvo.design.FillBar
import com.ruvo.design.GhostButton
import com.ruvo.design.PrimaryButton
import com.ruvo.design.Radius
import com.ruvo.design.Ruvo
import com.ruvo.design.RuvoCard
import com.ruvo.design.RuvoIcons
import com.ruvo.design.SectionTitle
import com.ruvo.design.Space
import com.ruvo.design.Tag
import com.ruvo.design.reveal
import com.ruvo.engine.guide.Library
import com.ruvo.engine.guide.Perks

/** What the Guide tab can open. The app shell owns the overlays. */
class GuideActions(
    val openBook: (String) -> Unit,
    val openBuy: () -> Unit,
    val openChat: (String?) -> Unit,
    val openMarketplace: () -> Unit = {},
)

@Composable
fun GuideScreen(a: GuideActions) {
    val state by RuvoGraph.repository.state.collectAsStateWithLifecycle()
    val c = Ruvo.colors
    val read = state.settings.readBooks
    val total = Library.all.size
    val done = Library.all.count { it.id in read }
    var readingOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).statusBarsPadding().padding(top = Space.md, bottom = Space.dockClearance),
    ) {
        Column(Modifier.padding(horizontal = Space.gutter)) {
            Eyebrow("Guide", Modifier.reveal(0))
            Spacer(Modifier.height(Space.xs))

            // Hero: what the Guide is, and how far you have read.
            RuvoCard(Modifier.fillMaxWidth().reveal(1), brush = c.heroBrush, border = null, elevated = true, padding = PaddingValues(Space.lg)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "Understand your money. One small idea at a time.",
                        Modifier.weight(1f).semantics { heading() },
                        style = Ruvo.type.headline, color = c.onHero,
                    )
                    Spacer(Modifier.width(Space.sm))
                    GuideMark()
                }
                Spacer(Modifier.height(Space.lg))
                // One spine per book: tall amber ones are read, short outlines are still to read.
                ReadingShelf(read)
                Spacer(Modifier.height(Space.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("$done read · ${total - done} to go", style = Ruvo.type.bodyStrong, color = c.onHero)
                        Text("No streaks, no pressure", style = Ruvo.type.caption, color = c.onHero.copy(alpha = 0.6f))
                    }
                    Text(
                        "See the list",
                        Modifier.clip(RoundedCornerShape(Radius.chip)).background(c.onHero.copy(alpha = 0.14f))
                            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "See which books you have read") { readingOpen = true }
                            .padding(horizontal = Space.md, vertical = Space.sm),
                        style = Ruvo.type.label, color = c.onHero,
                    )
                }
            }

            // The same Marketplace tile as in Profile, kept near the top so it is easy to find.
            Spacer(Modifier.height(Space.md))
            com.ruvo.design.BigTile("Marketplace", MARKETPLACE_LINE, RuvoIcons.Store, Modifier.reveal(2), onClick = a.openMarketplace)

            // New this week: the first unread fresh book, highlighted with a slow glow.
            val fresh = Library.shelf(Library.Shelf.NEW)
            val next = fresh.firstOrNull { it.id !in read } ?: fresh.first()
            Spacer(Modifier.height(Space.md))
            NewThisWeek(next, next.id in read, Modifier.reveal(3)) { a.openBook(next.id) }
        }

        // Picked from what actually happened this month.
        val forYou = Library.forYou(state.input, state.runway, read)
        Shelf("Picked for you", "Lessons picked from what you actually did this month.", forYou, read, a.openBook, 3)

        Library.Shelf.entries.forEachIndexed { i, shelf ->
            Shelf(shelf.title, shelf.blurb, Library.shelf(shelf), read, a.openBook, 4 + i)
        }

        Column(Modifier.padding(horizontal = Space.gutter)) {
            // Saved ideas, collected from book pages.
            val ideas = state.settings.savedIdeas.mapNotNull { key ->
                val book = Library.byId(key.substringBefore(":")) ?: return@mapNotNull null
                val page = key.substringAfter(":").toIntOrNull()?.takeIf { it in 1..book.pages.size } ?: return@mapNotNull null
                book to book.pages[page - 1]
            }
            if (ideas.isNotEmpty()) {
                SectionTitle("Your saved ideas")
                ideas.forEachIndexed { i, (book, page) ->
                    RuvoCard(Modifier.fillMaxWidth().padding(bottom = Space.xs).reveal(i), onClick = { a.openBook(book.id) }, onClickLabel = "Open ${book.title}") {
                        Eyebrow("${book.title} · ${page.first}", color = c.fuelInk)
                        Spacer(Modifier.height(Space.xxs))
                        Text(page.second, style = Ruvo.type.body, color = c.ink, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(Modifier.height(Space.lg))
            PrimaryButton("Before you buy", a.openBuy, Modifier.fillMaxWidth(), icon = RuvoIcons.Cart)
            Spacer(Modifier.height(Space.sm))
            GhostButton("Ask Ruvo a question", { a.openChat(null) }, Modifier.fillMaxWidth(), icon = RuvoIcons.Message)
        }
    }
    if (readingOpen) ReadingSheet(read, onOpen = a.openBook, onDismiss = { readingOpen = false })
}

/** The Guide's mark: an open book with a soft halo that breathes very slowly. */
@Composable
private fun GuideMark() {
    val c = Ruvo.colors
    val reduced = Ruvo.reducedMotion
    val t = rememberInfiniteTransition(label = "halo")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3_600, easing = LinearEasing), RepeatMode.Reverse), label = "b")
    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(52.dp).graphicsLayer { val s = if (reduced) 1f else 0.9f + 0.1f * b; scaleX = s; scaleY = s }.clip(CircleShape).background(c.fuel.copy(alpha = 0.18f)))
        Icon(RuvoIcons.Book, null, Modifier.size(26.dp), tint = c.fuelInk)
    }
}

@Composable
private fun NewThisWeek(book: Library.Book, read: Boolean, modifier: Modifier, onOpen: () -> Unit) {
    val c = Ruvo.colors
    Box(modifier.fillMaxWidth()) {
        RuvoCard(Modifier.fillMaxWidth(), brush = c.amberBrush, elevated = true, onClick = onOpen, onClickLabel = "Read ${book.title}", padding = PaddingValues(Space.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BookCover(book, read, width = 64.dp, showLabel = false, modifier = Modifier.graphicsLayer { rotationZ = -5f })
                Spacer(Modifier.width(Space.md))
                Column(Modifier.weight(1f)) {
                    Tag("New this week", color = c.ink, content = if (c.isDark) c.canvas else c.surface)
                    Spacer(Modifier.height(Space.xs))
                    Text(book.title, style = Ruvo.type.title, color = c.onAccent)
                    Text("${book.minutes} min read · tap to open", style = Ruvo.type.caption, color = c.onAccent.copy(alpha = 0.7f))
                }
                Icon(RuvoIcons.Forward, null, Modifier.size(22.dp), tint = c.onAccent)
            }
        }
    }
}

@Composable
private fun Shelf(title: String, blurb: String, books: List<Library.Book>, read: Set<String>, open: (String) -> Unit, index: Int) {
    if (books.isEmpty()) return
    val c = Ruvo.colors
    Column(Modifier.reveal(index)) {
        Column(Modifier.padding(horizontal = Space.gutter)) {
            SectionTitle(title)
            Text(blurb, style = Ruvo.type.caption, color = c.inkSoft)
        }
        Spacer(Modifier.height(Space.sm))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Space.gutter, vertical = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            itemsIndexed(books, key = { _, b -> b.id }) { _, b ->
                BookCover(b, b.id in read, onClick = { open(b.id) })
            }
        }
    }
}
