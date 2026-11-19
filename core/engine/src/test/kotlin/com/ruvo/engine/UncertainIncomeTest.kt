package com.ruvo.engine

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** One-off spends, the "Expected money" list, top-ups, the payday threshold and "Make it last until". */
class UncertainIncomeTest {

    private val today = LocalDate.of(2026, 12, 1)
    private val monthly = IncomePlan(Cadence.MONTHLY, rupees(12_000), today.plusDays(20))

    private fun spend(id: String, rs: Long, daysAgo: Long, oneOff: Boolean = false) =
        Txn(id, rupees(rs), Direction.DEBIT, today.minusDays(daysAgo), oneOff = oneOff)
    private fun income(id: String, rs: Long, daysAgo: Long, topUp: Boolean = false) =
        Txn(id, rupees(rs), Direction.CREDIT, today.minusDays(daysAgo), topUp = topUp)

    // --- One-off spends ---

    @Test fun oneOffSpendsStayOutOfTheDailyAverage() {
        // ₹4,200 over 14 days, ₹2,100 of it a one-off: the usual day is ₹150, not ₹300.
        val txns = (0L..13L).map { spend("d$it", 150, it) } + spend("laptop-bag", 2_100, 3, oneOff = true)
        assertEquals(rupees(4_200), txns.sumOf { it.amount })
        assertEquals(rupees(150), RunwayEngine.historyDaily(txns, today)!!.daily)
    }

    @Test fun withoutOneOffsTheAverageIsUnchanged() {
        val txns = (0L..13L).map { spend("d$it", 150, it) } + spend("bag", 2_100, 3)
        assertEquals(rupees(300), RunwayEngine.historyDaily(txns, today)!!.daily)
    }

    // --- Expected money ---

    private fun input(expected: List<ExpectedMoney>, income: IncomePlan = monthly, txns: List<Txn> = emptyList()) =
        RunwayInput(today, rupees(6_000), income, transactions = txns, expected = expected)

    @Test fun noEntriesBehavesExactlyAsBefore() {
        val before = RunwayEngine.calculate(RunwayInput(today, rupees(6_000), monthly))
        val after = RunwayEngine.calculate(input(emptyList()))
        assertEquals(before, after)
    }

    @Test fun aMaybeEntryOnlyLeavesTheNextMoneyDateUnchanged() {
        val r = RunwayEngine.calculate(input(listOf(ExpectedMoney("m", rupees(2_000), today.plusDays(5), sure = false))))
        assertEquals(today.plusDays(20), r.nextIncomeDate)
        assertEquals(20, r.daysToNextIncome)
    }

    @Test fun theEarliestSureEntryBecomesTheNextMoney() {
        val r = RunwayEngine.calculate(
            input(
                listOf(
                    ExpectedMoney("later", rupees(3_000), today.plusDays(12), sure = true),
                    ExpectedMoney("soon", rupees(1_500), today.plusDays(7), sure = true),
                    ExpectedMoney("maybe", rupees(5_000), today.plusDays(2), sure = false),
                ),
            ),
        )
        assertEquals(today.plusDays(7), r.nextIncomeDate)
        assertEquals(7, r.daysToNextIncome)
    }

    @Test fun aSureEntryGivesIrregularIncomeADate() {
        val irregular = IncomePlan(Cadence.IRREGULAR, 0, null)
        assertNull(RunwayEngine.calculate(input(emptyList(), irregular)).daysToNextIncome)
        val r = RunwayEngine.calculate(input(listOf(ExpectedMoney("gig", rupees(1_500), today.plusDays(6), sure = true)), irregular))
        assertEquals(6, r.daysToNextIncome)
    }

    @Test fun pastSureEntriesDoNotCount() {
        val r = RunwayEngine.calculate(input(listOf(ExpectedMoney("late", rupees(2_000), today.minusDays(1), sure = true))))
        assertEquals(today.plusDays(20), r.nextIncomeDate)
    }

    @Test fun aSureEntryAfterThePlannedDateDoesNotPushItBack() {
        val r = RunwayEngine.calculate(input(listOf(ExpectedMoney("x", rupees(2_000), today.plusDays(40), sure = true))))
        assertEquals(today.plusDays(20), r.nextIncomeDate)
    }

    // --- Top-up from home ---

    @Test fun aTopUpDoesNotRollTheNextMoneyDateForward() {
        // The planned date passed yesterday. A top-up is not the allowance arriving, so the money is still overdue.
        val late = IncomePlan(Cadence.MONTHLY, rupees(12_000), today.minusDays(1))
        val r = RunwayEngine.calculate(input(emptyList(), late, listOf(income("home", 2_000, 0, topUp = true))))
        assertTrue(r.incomeOverdue)
        assertEquals(today.minusDays(1), r.nextIncomeDate)
        val real = RunwayEngine.calculate(input(emptyList(), late, listOf(income("allowance", 12_000, 0))))
        assertFalse(real.incomeOverdue)
    }

    @Test fun aTopUpNeverShowsThePaydayCard() {
        assertNull(Payday.recent(listOf(income("home", 12_000, 0, topUp = true)), today, rupees(12_000)))
    }

    // --- Payday threshold ---

    @Test fun weeklyIncomeOfFifteenHundredTriggersThePaydayCard() {
        val weeks = listOf(income("w1", 1_500, 21), income("w2", 1_500, 14), income("w3", 1_500, 7), income("w4", 1_500, 0))
        // The old rule with ₹12,000 expected needed ₹3,600.
        assertEquals("w4", Payday.recent(weeks, today, rupees(12_000))?.id)
    }

    @Test fun thresholdIsThirtyPercentOfTheMedianOfTheLastThree() {
        val earlier = listOf(income("a", 10_000, 30), income("b", 2_000, 20), income("c", 6_000, 10), income("d", 4_000, 5))
        val candidate = income("now", 1_000, 0)
        // Last three before it: ₹2,000, ₹6,000, ₹4,000 → median ₹4,000 → ₹1,200.
        assertEquals(rupees(1_200), Payday.threshold(earlier + candidate, candidate, rupees(12_000)))
    }

    @Test fun thresholdNeverGoesBelowFiveHundred() {
        val earlier = listOf(income("a", 800, 14), income("b", 900, 7))
        val candidate = income("now", 600, 0)
        assertEquals(rupees(500), Payday.threshold(earlier + candidate, candidate, 0))
    }

    @Test fun withNoHistoryTheOldThresholdApplies() {
        val candidate = income("now", 1_000, 0)
        assertEquals(rupees(3_600), Payday.threshold(listOf(candidate), candidate, rupees(12_000)))
        assertEquals(rupees(1_000), Payday.threshold(listOf(candidate), candidate, 0))
    }

    // --- Make it last until ---

    private fun result(fuelRs: Long, nextInDays: Int?, dailyRs: Long): RunwayResult {
        val plan = IncomePlan(Cadence.LUMP_SUM, rupees(60_000), nextInDays?.let { today.plusDays(it.toLong()) })
        val txns = (0L..13L).map { spend("d$it", dailyRs, it) }
        return RunwayEngine.calculate(RunwayInput(today, rupees(fuelRs), plan, transactions = txns))
    }

    @Test fun cardIsOfferedForLongGapsOrWhenSwitchedOn() {
        assertTrue(Allowance.offered(74, switchedOn = false))
        assertFalse(Allowance.offered(45, switchedOn = false))
        assertFalse(Allowance.offered(null, switchedOn = false))
        assertTrue(Allowance.offered(10, switchedOn = true))
    }

    @Test fun dailyAllowanceIsFuelDividedByDaysToTheDate() {
        val r = result(fuelRs = 30_000, nextInDays = 100, dailyRs = 250)
        val p = Allowance.plan(r, today.plusDays(100))
        assertEquals(100, p.days)
        assertEquals(rupees(300), p.daily)
        assertEquals(rupees(250), p.usual)
        assertTrue(p.fits)
        assertEquals(today.plusDays(100), Allowance.defaultUntil(r))
    }

    @Test fun messagesAreKindInEveryCase() {
        val tight = Allowance.plan(result(30_000, 100, 450), today.plusDays(100))
        assertFalse(tight.fits)
        assertEquals("That's ₹150 less than your usual day. A few small swaps a week get you to 11 Mar.", Allowance.message(tight))
        val fits = Allowance.plan(result(30_000, 100, 250), today.plusDays(100))
        assertTrue(Allowance.message(fits).startsWith("Your usual ₹250 a day fits."))
        val empty = Allowance.plan(result(0, 100, 250), today.plusDays(100))
        assertEquals(0, empty.daily)
        listOf(tight, fits, empty).map(Allowance::message).forEach { m ->
            listOf("overspent", "too much", "careless", "bad").forEach { word -> assertFalse(m.contains(word, ignoreCase = true), m) }
        }
    }

    // --- Usual-day estimate ---

    private fun withEstimate(est: Long?, txns: List<Txn>, basis: SpendBasis = SpendBasis.AUTO) =
        RunwayEngine.calculate(RunwayInput(today, rupees(6_000), monthly, transactions = txns, basis = basis, estimatedDaily = est?.let(::rupees)))

    @Test fun estimateIsUsedWhileThereIsLittleHistory() {
        val r = withEstimate(200, listOf(spend("x", 900, 0)))
        assertEquals(rupees(200), r.dailySpend)
        assertEquals(SpendSource.ESTIMATE, r.spendSource)
        assertEquals(30, r.runwayDays)
    }

    @Test fun oneHeavyWeekMovesTheRunwayOnlyHalfAsMuch() {
        // A trip week: ₹600 a day for 7 days, then ₹150 a day. History alone says ₹375; with a ₹150 estimate, ₹262.
        val txns = (0L..6L).map { spend("trip$it", 600, it) } + (7L..13L).map { spend("d$it", 150, it) }
        assertEquals(rupees(375), withEstimate(null, txns).dailySpend)
        val r = withEstimate(150, txns)
        assertEquals(SpendSource.BLEND, r.spendSource)
        assertEquals(rupees(262) + 50, r.dailySpend)
    }

    @Test fun pinnedHistoryIgnoresTheEstimate() {
        val txns = (0L..13L).map { spend("d$it", 300, it) }
        assertEquals(rupees(300), withEstimate(150, txns, SpendBasis.HISTORY).dailySpend)
    }

    @Test fun noEstimateKeepsTheOldRule() {
        val txns = (0L..13L).map { spend("d$it", 300, it) }
        assertEquals(SpendSource.HISTORY, withEstimate(null, txns).spendSource)
    }
}
