package com.ruvo.engine

import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InsightsTest {

    private val today = LocalDate.of(2026, 11, 3)
    private fun spend(id: String, rs: Long, daysAgo: Long, cat: String = "Food", others: Long = 0) =
        Txn(id, rupees(rs), Direction.DEBIT, today.minusDays(daysAgo), category = cat, othersShare = rupees(others))

    @Test fun dailySpendingCountsYourShareOnlyAndEndsToday() {
        val txns = listOf(spend("a", 200, 0), spend("b", 600, 0, others = 300), spend("c", 50, 2), Txn("i", rupees(5_000), Direction.CREDIT, today))
        val days = Insights.dailySpending(txns, today, 7)
        assertEquals(7, days.size)
        assertEquals(today, days.last().date)
        assertEquals(rupees(500), days.last().spent)
        assertEquals(rupees(50), days[4].spent)
        assertEquals(0, days[0].spent)
    }

    @Test fun weekComparesWithTheWeekBefore() {
        val txns = listOf(spend("a", 300, 1), spend("b", 100, 3, "Transport"), spend("c", 200, 8), spend("d", 9_999, 20))
        val w = Insights.week(txns, today)
        assertEquals(rupees(400), w.total)
        assertEquals(rupees(200), w.lastWeek)
        assertEquals(100, w.changePercent)
        assertEquals("Food", w.categories.first().category)
        assertEquals(50, w.categories.first().changePercent)
        assertNull(w.categories.first { it.category == "Transport" }.changePercent)
        assertEquals("a", w.biggest?.id)
    }

    @Test fun ignoredPaymentsNeverCount() {
        val txns = listOf(spend("a", 300, 1).copy(status = TxnStatus.IGNORED))
        assertEquals(0, Insights.week(txns, today).total)
        assertEquals(0, Insights.months(txns, today).last().spent)
    }

    @Test fun monthsSeparateReceivedFromSpentAndSkipSettlements() {
        val txns = listOf(
            Txn("in", rupees(12_000), Direction.CREDIT, today.withDayOfMonth(1)),
            Txn("back", rupees(400), Direction.CREDIT, today, kind = TxnKind.SETTLEMENT),
            spend("a", 700, 1),
            spend("old", 1_000, 20),
        )
        val m = Insights.months(txns, today, 3)
        assertEquals(YearMonth.of(2026, 11), m.last().month)
        assertEquals(rupees(12_000), m.last().received)
        assertEquals(rupees(700), m.last().spent)
        assertEquals(rupees(1_000), m[1].spent)
    }

    @Test fun moodNeverAlarmsAndFollowsTheBuffer() {
        fun r(balance: Long, nextIn: Long) = RunwayEngine.calculate(
            RunwayInput(today, rupees(balance), IncomePlan(Cadence.MONTHLY, rupees(10_000), today.plusDays(nextIn)), transactions = (0L..13L).map { spend("s$it", 200, it) }),
        )
        assertEquals(Insights.MoodLevel.COMFORTABLE, Insights.mood(r(20_000, 10)).level)
        assertEquals(Insights.MoodLevel.PLAN, Insights.mood(r(1_000, 25)).level)
    }
}
