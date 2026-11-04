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

    @Test fun editedMonthsReplaceRecordedTotalsAndAreMarked() {
        val txns = listOf(spend("a", 700, 1), Txn("in", rupees(12_000), Direction.CREDIT, today.withDayOfMonth(1)))
        val sep = YearMonth.of(2026, 9)
        val m = Insights.months(txns, today, 3, mapOf(sep to (rupees(10_000) to rupees(9_000))))
        val edited = m.first { it.month == sep }
        assertEquals(true, edited.edited)
        assertEquals(rupees(9_000), edited.spent)
        assertEquals(rupees(1_000), edited.leftOver)
        assertEquals(false, m.last().edited)
        assertEquals(rupees(700), m.last().spent)
    }

    @Test fun flowCountsMoneyInAndOutWithinTheSpanOnly() {
        val txns = listOf(
            spend("a", 300, 1), spend("b", 200, 9),
            Txn("in", rupees(5_000), Direction.CREDIT, today.minusDays(2)),
            Txn("back", rupees(400), Direction.CREDIT, today, kind = TxnKind.SETTLEMENT),
        )
        val f = Insights.flow(txns, today.minusDays(6), today)
        assertEquals(rupees(5_000), f.moneyIn)
        assertEquals(rupees(300), f.moneyOut)
        assertEquals(rupees(4_700), f.net)
        assertEquals(2, f.count)
    }

    @Test fun moodNeverAlarmsAndFollowsTheBuffer() {
        fun r(balance: Long, nextIn: Long) = RunwayEngine.calculate(
            RunwayInput(today, rupees(balance), IncomePlan(Cadence.MONTHLY, rupees(10_000), today.plusDays(nextIn)), transactions = (0L..13L).map { spend("s$it", 200, it) }),
        )
        assertEquals(Insights.MoodLevel.COMFORTABLE, Insights.mood(r(20_000, 10)).level)
        assertEquals(Insights.MoodLevel.PLAN, Insights.mood(r(1_000, 25)).level)
    }

    @Test fun `changes are said in words and never go past 100 percent`() {
        assertEquals("34% more than last week", Insights.changeWords(34, "last week"))
        assertEquals("20% less than last week", Insights.changeWords(-20, "last week"))
        assertEquals("double last week", Insights.changeWords(100, "last week"))
        assertEquals("more than double last week", Insights.changeWords(250, "last week"))
        assertEquals("the same as last week", Insights.changeWords(0, "last week"))
        assertEquals("+34%", Insights.changeTag(34))
        assertEquals("-20%", Insights.changeTag(-20))
        assertEquals("2x or more", Insights.changeTag(250))
    }
}
