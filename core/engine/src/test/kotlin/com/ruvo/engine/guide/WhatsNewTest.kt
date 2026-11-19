package com.ruvo.engine.guide

import com.ruvo.engine.Cadence
import com.ruvo.engine.IncomePlan
import com.ruvo.engine.RunwayEngine
import com.ruvo.engine.RunwayInput
import com.ruvo.engine.rupees
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhatsNewTest {

    private val today = LocalDate.of(2026, 11, 19)

    @Test fun nothingIsNewOnceEverythingHasBeenSeen() {
        val seenOffers = Perks.all.map { it.id }.toSet()
        val seenBooks = Library.all.map { it.id }.toSet()
        assertTrue(WhatsNew.offers(seenOffers, today).isEmpty())
        assertTrue(WhatsNew.books(seenBooks).isEmpty())
    }

    @Test fun anUnseenCurrentOfferAndBookAreNew() {
        val spotify = Perks.all.first { it.id == "spotify" }
        val seenOffers = Perks.all.map { it.id }.toSet() - spotify.id
        assertEquals(listOf("spotify"), WhatsNew.offers(seenOffers, today).map { it.id })
        val book = Library.all.last()
        assertEquals(listOf(book.id), WhatsNew.books(Library.all.map { it.id }.toSet() - book.id).map { it.id })
    }

    @Test fun endedOffersAreNeverAnnounced() {
        val later = today.plusYears(5)
        val ended = Perks.all.filter { o -> o.ends != null && o.ends!!.isBefore(later) }.map { it.id }.toSet()
        assertTrue(WhatsNew.offers(emptySet(), later).none { it.id in ended })
    }

    @Test fun theDailyLineGivesDaysTodaysAmountAndPaymentsToConfirm() {
        val input = RunwayInput(today, rupees(6_000), IncomePlan(Cadence.MONTHLY, rupees(12_000), today.plusDays(12)))
        val r = RunwayEngine.calculate(input)
        val (title, body) = WhatsNew.daily(r, toConfirm = 3)
        assertTrue(title.contains("days of runway"), title)
        assertTrue(body.contains("a day gets you to"), body)
        assertTrue(body.contains("3 payments to confirm"), body)
        val (_, quiet) = WhatsNew.daily(r, toConfirm = 0)
        assertTrue(!quiet.contains("confirm"), quiet)
    }
}
