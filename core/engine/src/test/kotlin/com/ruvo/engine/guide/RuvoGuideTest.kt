package com.ruvo.engine.guide

import com.ruvo.engine.Cadence
import com.ruvo.engine.Commitment
import com.ruvo.engine.Direction
import com.ruvo.engine.Goal
import com.ruvo.engine.IncomePlan
import com.ruvo.engine.RunwayEngine
import com.ruvo.engine.RunwayInput
import com.ruvo.engine.Txn
import com.ruvo.engine.demo.DemoPersonas
import com.ruvo.engine.rupees
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuvoGuideTest {

    private val today = LocalDate.of(2026, 11, 3)
    private val steady = (0L..13L).map { Txn("s$it", rupees(200), Direction.DEBIT, today.minusDays(it), category = if (it % 2 == 0L) "Food" else "Transport") }
    private val input = RunwayInput(
        today, rupees(4_600), IncomePlan(Cadence.MONTHLY, rupees(12_000), today.plusDays(19)),
        commitments = listOf(Commitment("sp", "Spotify", rupees(149), dueDayOfMonth = 20)),
        goals = listOf(Goal("g", "Goa trip", rupees(8_000), rupees(2_000), today.plusMonths(6), rupees(1_000))),
        transactions = steady,
    )
    private val ctx = RuvoGuide.Context(input, RunwayEngine.calculate(input), owedToYou = rupees(450), name = "Aarav")

    private fun ask(q: String) = RuvoGuide.answer(q, ctx)

    /** The 20 most common questions, each with the topic it must land on and a fact the answer must contain. */
    private val common = listOf(
        Triple("Why this many days?", RuvoGuide.Topic.RUNWAY, "days"),
        Triple("why is my runway 12 days", RuvoGuide.Topic.RUNWAY, "Fuel"),
        Triple("How much can I spend today?", RuvoGuide.Topic.TODAY, "a day"),
        Triple("Can I afford ₹1,500?", RuvoGuide.Topic.AFFORD, "days of your usual spending"),
        Triple("can i buy shoes for 2500", RuvoGuide.Topic.AFFORD, "Ruvo never blocks"),
        Triple("should I spend 2k on a concert", RuvoGuide.Topic.AFFORD, "days"),
        Triple("Where did my money go this week?", RuvoGuide.Topic.SPENDING, "This week you spent"),
        Triple("how much did i spend on food", RuvoGuide.Topic.SPENDING, "Food came to"),
        Triple("When will my money run out?", RuvoGuide.Topic.RUNS_OUT, "lasts until"),
        Triple("when is my next pocket money", RuvoGuide.Topic.NEXT_MONEY, "22 Nov"),
        Triple("I just got my stipend, what now?", RuvoGuide.Topic.MONEY_IN, "Fixed first"),
        Triple("what is fuel", RuvoGuide.Topic.BUCKETS, "free to spend"),
        Triple("what bills are due", RuvoGuide.Topic.DUE, "Spotify"),
        Triple("how can I save more", RuvoGuide.Topic.SAVING, "goals the day it arrives"),
        Triple("how is my goa trip goal", RuvoGuide.Topic.GOALS, "25%"),
        Triple("who owes me money", RuvoGuide.Topic.OWED, "₹450"),
        Triple("should I invest in mutual funds", RuvoGuide.Topic.INVESTING, "do not recommend"),
        Triple("should I get a credit card", RuvoGuide.Topic.CREDIT, "will not recommend"),
        Triple("how am i doing", RuvoGuide.Topic.CHECK_IN, "a day gets you to your next money"),
        Triple("hi", RuvoGuide.Topic.GREETING, "Hello, Aarav"),
    )

    @Test fun `the 20 most common questions get correct answers offline`() {
        val wrong = common.filter { (q, topic, fact) ->
            val r = ask(q)
            r.topic != topic || !r.text.contains(fact)
        }.map { (q, topic, _) -> "$q -> ${ask(q).topic} (wanted $topic): ${ask(q).text}" }
        wrong.forEach { println("  wrong: $it") }
        assertTrue(wrong.isEmpty(), "${wrong.size} of ${common.size} wrong")
    }

    @Test fun `runway answer explains the actual numbers`() {
        val r = ask("why").text
        assertTrue(r.contains("₹4,600"))
        assertTrue(r.contains("₹2,000 is protected"))
        assertTrue(r.contains("₹200 a day"))
    }

    @Test fun `afford answer matches the engine`() {
        val r = ask("can I afford ₹600").text
        assertTrue(r.contains("3 days"), r)
    }

    @Test fun `amounts are read in several styles`() {
        assertEquals(rupees(1_500), RuvoGuide.amountIn("can i afford ₹1,500"))
        assertEquals(rupees(800), RuvoGuide.amountIn("rs 800 for a jacket"))
        assertEquals(rupees(2_000), RuvoGuide.amountIn("spend 2k on shoes"))
        assertEquals(rupees(1_200), RuvoGuide.amountIn("can i buy a jacket for 1200"))
        assertEquals(null, RuvoGuide.amountIn("how am i doing"))
    }

    @Test fun `answers never shame or tell the user they cannot buy`() {
        val all = common.map { ask(it.first).text.lowercase() }
        listOf("you can't", "you cannot", "don't buy", "bad", "irresponsible", "waste").forEach { word ->
            assertFalse(all.any { it.contains(word) }, "found '$word'")
        }
    }

    @Test fun `every persona gets a sensible check in and lesson`() {
        DemoPersonas.all(today).forEach { p ->
            val r = RunwayEngine.calculate(p.input)
            val reply = RuvoGuide.answer("how am i doing", RuvoGuide.Context(p.input, r))
            assertTrue(reply.text.isNotBlank())
            assertTrue(Lessons.forNow(p.input, r).title.isNotBlank())
        }
    }

    @Test fun `perk saving is shown in days`() {
        assertEquals(4, Perks.daysSaved(Perks.all[0], rupees(200)))
        assertEquals(0, Perks.daysSaved(Perks.all[5], rupees(200)))
    }

    private fun listing(checked: LocalDate, ends: LocalDate?) =
        Perks.all[0].copy(checked = checked, ends = ends)

    @Test fun `listing status says whether it is current, ending, stale or over`() {
        val today = LocalDate.of(2026, 11, 4)
        assertEquals(Perks.State.ACTIVE, Perks.status(listing(today.minusDays(3), today.plusDays(60)), today).state)
        assertEquals(Perks.State.ACTIVE, Perks.status(listing(today.minusDays(3), null), today).state)

        val ending = Perks.status(listing(today.minusDays(3), today.plusDays(9)), today)
        assertEquals(Perks.State.ENDING, ending.state)
        assertEquals(9, ending.daysLeft)

        // Checked more than 30 days ago: shown, but flagged so nobody relies on it.
        assertEquals(Perks.State.RECHECK, Perks.status(listing(today.minusDays(31), today.plusDays(60)), today).state)
        assertEquals(Perks.State.ACTIVE, Perks.status(listing(today.minusDays(30), today.plusDays(60)), today).state)

        // Ended trumps everything else.
        val over = Perks.status(listing(today.minusDays(40), today.minusDays(1)), today)
        assertEquals(Perks.State.EXPIRED, over.state)
        assertEquals(40, over.checkedDaysAgo)
    }

    @Test fun `time left runs from the last check to the end date`() {
        val today = LocalDate.of(2026, 11, 4)
        assertEquals(0.5f, Perks.status(listing(today.minusDays(10), today.plusDays(10)), today).timeLeft, 0.001f)
        assertEquals(0f, Perks.status(listing(today.minusDays(10), today.minusDays(1)), today).timeLeft, 0.001f)
        assertEquals(1f, Perks.status(listing(today.minusDays(10), null), today).timeLeft, 0.001f)
    }

    @Test fun `listings put current offers first and ended ones last`() {
        val today = LocalDate.of(2026, 11, 4)
        val order = Perks.listed(today).map { Perks.status(it, today).state }
        assertEquals(order.sortedBy { it.ordinal }, order)
        assertEquals(Perks.all.size, Perks.listed(today).size)
    }
}
