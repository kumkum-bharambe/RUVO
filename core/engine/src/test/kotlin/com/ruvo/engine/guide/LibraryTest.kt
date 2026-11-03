package com.ruvo.engine.guide

import com.ruvo.engine.Cadence
import com.ruvo.engine.Direction
import com.ruvo.engine.IncomePlan
import com.ruvo.engine.RunwayEngine
import com.ruvo.engine.RunwayInput
import com.ruvo.engine.Txn
import com.ruvo.engine.rupees
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LibraryTest {

    private val today = LocalDate.of(2026, 11, 3)

    @Test fun everyBookHasSevenFilledPages() {
        Library.all.forEach { b ->
            assertEquals(7, b.pages.size, b.id)
            b.pages.forEach { (label, text) -> assertTrue(text.isNotBlank(), "${b.id}: $label is empty") }
            assertTrue(b.check.options.size in 2..4, "${b.id} check options")
            assertTrue(b.check.correct in b.check.options.indices, "${b.id} correct answer out of range")
            assertTrue(b.check.because.isNotBlank(), "${b.id} explains its answer")
        }
    }

    @Test fun idsAreUniqueAndEveryShelfHasBooks() {
        assertEquals(Library.all.size, Library.all.map { it.id }.toSet().size)
        Library.Shelf.entries.forEach { assertTrue(Library.shelf(it).isNotEmpty(), "${it.title} is empty") }
    }

    @Test fun everyTodayLessonOpensItsBook() {
        Lessons.all.forEach { assertNotNull(Library.byId(it.id), "No book for lesson ${it.id}") }
    }

    @Test fun neverShamesAndNeverNamesAnInvestment() {
        val shaming = listOf("waste", "stupid", "irresponsible", "bad with money", "lazy", "shame")
        val products = listOf("mutual fund", "sip ", "stock", "crypto", "bitcoin", "nifty", "sensex", "fixed deposit", "gold")
        Library.all.forEach { b ->
            val text = (b.pages.map { it.second } + b.check.options + b.check.because).joinToString(" ").lowercase()
            shaming.forEach { assertFalse(it in text, "${b.id} says \"$it\"") }
            if (b.shelf == Library.Shelf.INVEST) products.forEach { assertFalse(it in text, "${b.id} names \"$it\"") }
        }
    }

    @Test fun forYouStartsWithTheTodayLessonAndPrefersUnread() {
        val snacks = (0L..4L).map { Txn("t$it", rupees(120), Direction.DEBIT, today.minusDays(it), category = "Food") }
        val input = RunwayInput(today, rupees(5_000), IncomePlan(Cadence.MONTHLY, rupees(10_000), today.plusDays(20)), transactions = snacks)
        val result = RunwayEngine.calculate(input)
        val picks = Library.forYou(input, result)
        assertEquals(Lessons.forNow(input, result).id, picks.first().id)
        assertTrue(picks.size in 1..3)
        val readFirst = Library.forYou(input, result, read = setOf(picks.first().id))
        assertTrue(readFirst.first().id != picks.first().id, "Unread books come first")
    }
}
