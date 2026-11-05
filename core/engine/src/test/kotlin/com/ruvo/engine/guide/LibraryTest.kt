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

    @Test fun neverShamesAndNeverRecommendsAnInvestment() {
        // Kinds of saving and investing may be explained in general (Kumkum, 5 Nov 2026). What stays banned is
        // naming a specific market, coin or product, and anything that reads as advice or a promise.
        val shaming = listOf("waste", "stupid", "irresponsible", "bad with money", "lazy", "shame")
        val advice = listOf("crypto", "bitcoin", "nifty", "sensex", "you should invest in", "best fund", "best stock", "will double your money", "sure-shot", "tip:")
        Library.all.forEach { b ->
            val text = (b.pages.map { it.second } + b.quiz.flatMap { it.options + it.because + it.question } + b.keyPoints + listOfNotNull(b.rule, b.fact))
                .joinToString(" ").lowercase()
            shaming.forEach { assertFalse(it in text, "${b.id} says \"$it\"") }
            advice.forEach { assertFalse(it in text, "${b.id} says \"$it\"") }
        }
    }

    @Test fun everyBookEndsWithAThreeQuestionQuizAndTakeaways() {
        Library.all.forEach { b ->
            assertEquals(3, b.quiz.size, "${b.id} quiz")
            b.quiz.forEach { q ->
                assertTrue(q.options.size in 2..4 && q.correct in q.options.indices && q.because.isNotBlank(), "${b.id}: ${q.question}")
            }
            assertTrue(b.keyPoints.size in 2..4 && b.keyPoints.all { it.isNotBlank() }, "${b.id} takeaways")
        }
    }

    @Test fun newShelvesAreFilledAndExtrasPointAtRealThings() {
        listOf(Library.Shelf.SAFETY, Library.Shelf.CREDIT, Library.Shelf.WORK).forEach { assertEquals(3, Library.shelf(it).size, it.title) }
        Library.all.forEach { b -> b.story?.let { assertNotNull(Stories.byId(it), "${b.id} story $it") } }
        assertTrue(Library.all.count { it.tool != null } >= 5)
        assertTrue(Library.all.count { it.rule != null } >= 20)
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
