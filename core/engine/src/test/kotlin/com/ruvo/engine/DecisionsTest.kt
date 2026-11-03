package com.ruvo.engine

import com.ruvo.engine.demo.DemoPersonas
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DecisionsTest {

    private val today = LocalDate.of(2026, 11, 3)
    private val steady = (0L..13L).map { Txn("s$it", rupees(200), Direction.DEBIT, today.minusDays(it)) }

    private fun result(balance: Long, daysToIncome: Long = 19) = RunwayEngine.calculate(
        RunwayInput(today, rupees(balance), IncomePlan(Cadence.MONTHLY, rupees(12_000), today.plusDays(daysToIncome)), transactions = steady),
    )

    // --- Before you buy ---

    @Test fun `price is shown in days of usual spending`() {
        val b = BeforeYouBuy.evaluate(result(4_600), rupees(600))
        assertEquals(3.0, b.costDays)
        assertEquals(20, b.runwayAfter)
        assertEquals(1, b.bufferAfter)
        assertEquals(BeforeYouBuy.Verdict.TIGHT, b.verdict)
    }

    @Test fun `small purchase stays comfortable`() {
        val b = BeforeYouBuy.evaluate(result(8_000), rupees(250))
        assertEquals(1.3, b.costDays)
        assertEquals(BeforeYouBuy.Verdict.COMFORTABLE, b.verdict)
    }

    @Test fun `purchase that breaks the buffer is short`() {
        val b = BeforeYouBuy.evaluate(result(4_600), rupees(1_500))
        assertEquals(BeforeYouBuy.Verdict.SHORT, b.verdict)
        assertTrue(b.bufferAfter!! < 0)
    }

    @Test fun `purchase bigger than fuel is short with zero runway`() {
        val b = BeforeYouBuy.evaluate(result(1_000), rupees(5_000))
        assertEquals(0, b.runwayAfter)
        assertEquals(0, b.safeDailyAfter)
        assertEquals(BeforeYouBuy.Verdict.SHORT, b.verdict)
    }

    @Test fun `safe daily after purchase spreads what is left`() {
        val b = BeforeYouBuy.evaluate(result(4_600), rupees(800))
        assertEquals(rupees(200), b.safeDailyAfter) // 3,800 / 19
    }

    // --- Splits ---

    @Test fun `equal split leaves rounding paise with you`() {
        val s = Splits.equal("t", rupees(1_000), listOf("Isha", "Kunal"))
        assertEquals(33333, s.shares.first().amount)
        assertEquals(rupees(1_000), s.othersTotal + s.yourShare)
        assertEquals(33334, s.yourShare)
    }

    @Test fun `owed to you counts unpaid shares only`() {
        val s = Splits.Split("t", rupees(900), listOf(Splits.Share("Isha", rupees(300), paid = true), Splits.Share("Kunal", rupees(300))))
        assertEquals(rupees(300), Splits.owedToYou(listOf(s)))
        assertEquals(rupees(300), s.yourShare)
    }

    @Test fun `upi link carries amount and note`() {
        val link = Splits.upiLink("kumkum@okaxis", "Kumkum", rupees(333) + 33, "Groceries split")
        assertEquals("upi://pay?pa=kumkum%40okaxis&pn=Kumkum&am=333.33&cu=INR&tn=Groceries%20split", link)
    }

    @Test fun `a split payment flows through to runway`() {
        // 14 days of ₹300 spends shared with one friend: you spend ₹150 a day, not ₹300.
        val tx = (0L..13L).map { Txn("s$it", rupees(300), Direction.DEBIT, today.minusDays(it), othersShare = rupees(150)) }
        val r = RunwayEngine.calculate(RunwayInput(today, rupees(3_000), IncomePlan(Cadence.MONTHLY, 0, today.plusDays(19)), transactions = tx))
        assertEquals(rupees(150), r.dailySpend)
        assertEquals(20, r.runwayDays)
        // When the friend settles, the money arrives as a settlement credit: balance rises, spending does not change.
        val settled = tx + Txn("pay", rupees(2_100), Direction.CREDIT, today, kind = TxnKind.SETTLEMENT)
        val r2 = RunwayEngine.calculate(RunwayInput(today, rupees(5_100), IncomePlan(Cadence.MONTHLY, 0, today.plusDays(19)), transactions = settled))
        assertEquals(rupees(150), r2.dailySpend)
        assertEquals(34, r2.runwayDays)
    }

    // --- Money arrives ---

    @Test fun `money arriving sets aside fixed first then suggests future`() {
        val c = listOf(Commitment("rent", "Rent", rupees(3_000), 5), Commitment("rc", "Recharge", rupees(299), 20))
        val g = listOf(Goal("g", "Goa", rupees(8_000), 0, monthlyContribution = rupees(1_000)))
        val s = MoneyArrives.suggest(rupees(12_000), c, g)
        assertEquals(rupees(3_299), s.setAsideFixed)
        assertEquals(rupees(1_000), s.suggestFuture)
        assertEquals(rupees(12_000) - rupees(3_299) - rupees(1_000), s.leavesFuel)
    }

    @Test fun `future suggestion is capped at 20 percent`() {
        val g = listOf(Goal("g", "Laptop", rupees(50_000), 0, monthlyContribution = rupees(5_000)))
        val s = MoneyArrives.suggest(rupees(10_000), emptyList(), g)
        assertEquals(rupees(2_000), s.suggestFuture)
    }

    // --- Day strip ---

    @Test fun `day strip runs from today to income day`() {
        val days = DayStrip.build(result(2_000))
        assertEquals(20, days.size)
        assertEquals(DayStrip.State.SPENT_TODAY, days.first().state)
        assertEquals(DayStrip.State.INCOME, days.last().state)
        assertEquals(DayStrip.State.COVERED, days[9].state)
        assertEquals(DayStrip.State.SHORT, days[10].state)
    }

    @Test fun `day strip fill is partial on the day money runs out`() {
        val r = result(2_100) // 10.5 days of fuel
        assertEquals(1f, DayStrip.fill(r, 9))
        assertEquals(0.5f, DayStrip.fill(r, 10))
        assertEquals(0f, DayStrip.fill(r, 11))
        assertEquals(today.plusDays(11), DayStrip.runsOutOn(r))
    }

    // --- Personas ---

    @Test fun `every demo persona calculates without errors`() {
        DemoPersonas.all(today).forEach { p ->
            val r = RunwayEngine.calculate(p.input)
            assertTrue(r.runwayDays >= 0, p.name)
            assertTrue(r.dailySpend > 0, "${p.name} should have a daily spend")
        }
    }

    @Test fun `aarav uses his history and riya has no buffer`() {
        val aarav = RunwayEngine.calculate(DemoPersonas.aarav(today).input)
        assertEquals(SpendSource.HISTORY, aarav.spendSource)
        val riya = RunwayEngine.calculate(DemoPersonas.riya(today).input)
        assertEquals(null, riya.bufferDays)
    }
}
