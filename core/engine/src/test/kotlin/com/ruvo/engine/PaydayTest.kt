package com.ruvo.engine

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaydayTest {

    private val today = LocalDate.of(2026, 12, 1)
    private fun income(id: String, rs: Long, daysAgo: Long, status: TxnStatus = TxnStatus.CONFIRMED) =
        Txn(id, rupees(rs), Direction.CREDIT, today.minusDays(daysAgo), status = status)
    private fun goal(id: String, target: Long, saved: Long, monthly: Long = 0) =
        Goal(id, id, rupees(target), rupees(saved), monthlyContribution = rupees(monthly))

    @Test fun findsMoneyThatArrivedInTheLastTwoDays() {
        val txns = listOf(income("old", 12_000, 5), income("new", 12_000, 1))
        assertEquals("new", Payday.recent(txns, today, rupees(12_000))?.id)
    }

    @Test fun ignoresSmallRefundsAndOldIncome() {
        val txns = listOf(income("refund", 300, 0), income("old", 12_000, 3))
        assertNull(Payday.recent(txns, today, rupees(12_000)))
    }

    @Test fun smallIncomeCountsWhenNoIncomeIsExpected() {
        // Riya's money varies: with no expected amount, ₹1,000 or more counts as money arriving.
        assertEquals("gig", Payday.recent(listOf(income("gig", 1_500, 0)), today, 0)?.id)
        assertNull(Payday.recent(listOf(income("tiny", 900, 0)), today, 0))
    }

    @Test fun ignoredMoneyDoesNotCount() {
        assertNull(Payday.recent(listOf(income("x", 12_000, 0, TxnStatus.IGNORED)), today, rupees(12_000)))
    }

    @Test fun suggestsTheGoalsOwnMonthlyAmount() {
        val s = Payday.suggestPour(rupees(12_000), listOf(goal("Goa trip", 8_000, 2_000, monthly = 1_000)), fuel = rupees(9_000))
        assertEquals("Goa trip", s?.goal?.id)
        assertEquals(rupees(1_000), s?.amount)
    }

    @Test fun withoutAMonthlyAmountSuggestsATenthOfTheIncomeInHundreds() {
        val s = Payday.suggestPour(rupees(12_345), listOf(goal("Laptop", 50_000, 0)), fuel = rupees(9_000))
        assertEquals(rupees(1_200), s?.amount)
    }

    @Test fun neverSuggestsMoreThanTheGoalNeedsOrTheFuelThereIs() {
        assertEquals(rupees(300), Payday.suggestPour(rupees(12_000), listOf(goal("A", 8_000, 7_700, monthly = 1_000)), rupees(9_000))?.amount)
        assertEquals(rupees(400), Payday.suggestPour(rupees(12_000), listOf(goal("B", 8_000, 0, monthly = 1_000)), rupees(450))?.amount)
    }

    @Test fun noSuggestionWithoutAnOpenGoalOrFuel() {
        assertNull(Payday.suggestPour(rupees(12_000), emptyList(), rupees(9_000)))
        assertNull(Payday.suggestPour(rupees(12_000), listOf(goal("Done", 1_000, 1_000)), rupees(9_000)))
        assertNull(Payday.suggestPour(rupees(12_000), listOf(goal("A", 8_000, 0, monthly = 1_000)), rupees(50)))
    }
}
