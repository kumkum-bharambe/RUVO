package com.ruvo.engine

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunwayEngineTest {

    private val today = LocalDate.of(2026, 11, 3)

    private fun spend(daysAgo: Long, amount: Long, bucket: Bucket = Bucket.FUEL, others: Long = 0, status: TxnStatus = TxnStatus.CONFIRMED) =
        Txn("s$daysAgo-$amount", rupees(amount), Direction.DEBIT, today.minusDays(daysAgo), bucket = bucket, othersShare = rupees(others), status = status)

    /** 14 days of ₹200 a day. */
    private val steady = (0L..13L).map { spend(it, 200) }

    private fun monthly(next: LocalDate = today.plusDays(19), amount: Long = 12_000) =
        IncomePlan(Cadence.MONTHLY, rupees(amount), next)

    private fun input(
        balance: Long = 8_000,
        income: IncomePlan = monthly(),
        commitments: List<Commitment> = emptyList(),
        goals: List<Goal> = emptyList(),
        tx: List<Txn> = steady,
        basis: SpendBasis = SpendBasis.AUTO,
    ) = RunwayInput(today, rupees(balance), income, commitments, goals, tx, basis)

    // --- Core maths ---

    @Test fun `runway is fuel divided by daily spend`() {
        val r = RunwayEngine.calculate(input())
        assertEquals(rupees(200), r.dailySpend)
        assertEquals(40, r.runwayDays)
        assertEquals(SpendSource.HISTORY, r.spendSource)
    }

    @Test fun `buffer is runway minus days to next income`() {
        val r = RunwayEngine.calculate(input(balance = 4_600))
        assertEquals(23, r.runwayDays)
        assertEquals(19, r.daysToNextIncome)
        assertEquals(4, r.bufferDays)
        assertTrue(r.makesIt)
        assertEquals(4, r.spareDays)
    }

    @Test fun `runway rounds down partial days`() {
        val r = RunwayEngine.calculate(input(balance = 4_799))
        assertEquals(23, r.runwayDays)
    }

    @Test fun `fixed commitments before next income are reserved`() {
        val rent = Commitment("rent", "Rent", rupees(3_000), dueDayOfMonth = 10)
        val r = RunwayEngine.calculate(input(commitments = listOf(rent)))
        assertEquals(rupees(3_000), r.fixedReserved)
        assertEquals(rupees(5_000), r.fuel)
        assertEquals(25, r.runwayDays)
    }

    @Test fun `commitments after next income are not reserved`() {
        val late = Commitment("late", "Late bill", rupees(1_000), dueDayOfMonth = 28)
        val r = RunwayEngine.calculate(input(income = monthly(next = today.plusDays(10)), commitments = listOf(late)))
        assertEquals(0, r.fixedReserved)
    }

    @Test fun `commitment paid early counts as paid`() {
        val rent = Commitment("rent", "Rent", rupees(3_000), dueDayOfMonth = 10, lastPaidOn = today.minusDays(1))
        val r = RunwayEngine.calculate(input(commitments = listOf(rent)))
        assertEquals(0, r.fixedReserved)
    }

    @Test fun `recent unpaid overdue commitment is still reserved`() {
        // Paid last month on 1 Oct, so the 1 Nov bill is known to be unpaid.
        val bill = Commitment("bill", "Electricity", rupees(800), dueDayOfMonth = 1, lastPaidOn = LocalDate.of(2026, 10, 1))
        val r = RunwayEngine.calculate(input(commitments = listOf(bill)))
        assertEquals(rupees(800), r.fixedReserved)
    }

    @Test fun `past due date with no payment history is assumed handled`() {
        val bill = Commitment("bill", "Electricity", rupees(800), dueDayOfMonth = 1)
        val r = RunwayEngine.calculate(input(commitments = listOf(bill)))
        assertEquals(0, r.fixedReserved)
    }

    @Test fun `one-off commitment inside the window is reserved once`() {
        val fees = Commitment("fees", "Exam fees", rupees(1_500), dueDate = today.plusDays(5), recurring = false)
        val r = RunwayEngine.calculate(input(commitments = listOf(fees)))
        assertEquals(rupees(1_500), r.fixedReserved)
        assertEquals(1, r.reservedCommitments.size)
    }

    @Test fun `due day 31 falls on the last day of shorter months`() {
        val c = Commitment("c", "Sub", rupees(100), dueDayOfMonth = 31)
        val nov = RunwayEngine.occurrences(c, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1))
        assertEquals(listOf(LocalDate.of(2026, 11, 30)), nov)
    }

    @Test fun `monthly commitment repeats when the window spans two months`() {
        val c = Commitment("c", "Recharge", rupees(299), dueDayOfMonth = 5)
        val r = RunwayEngine.calculate(input(income = IncomePlan(Cadence.LUMP_SUM, rupees(60_000), today.plusDays(70), 120), commitments = listOf(c)))
        assertEquals(rupees(299) * 3, r.fixedReserved) // 5 Nov, 5 Dec, 5 Jan, all before income on 12 Jan
    }

    @Test fun `goal money is protected and excluded from fuel`() {
        val goal = Goal("g", "Goa", rupees(8_000), rupees(2_000))
        val r = RunwayEngine.calculate(input(goals = listOf(goal)))
        assertEquals(rupees(2_000), r.futureProtected)
        assertEquals(rupees(6_000), r.fuel)
        assertEquals(30, r.runwayDays)
    }

    // --- Daily spend rule ---

    @Test fun `auto uses plan when history is too short`() {
        val r = RunwayEngine.calculate(input(tx = listOf(spend(0, 500), spend(1, 100))))
        assertEquals(SpendSource.PLAN, r.spendSource)
        assertEquals(rupees(400), r.dailySpend) // 12,000 / 30
    }

    @Test fun `auto uses history once there are 7 days and 3 spends`() {
        val tx = listOf(spend(0, 100), spend(3, 100), spend(6, 500))
        val r = RunwayEngine.calculate(input(tx = tx))
        assertEquals(SpendSource.HISTORY, r.spendSource)
        assertEquals(rupees(100), r.dailySpend) // 700 over 7 days
    }

    @Test fun `user can pin the plan even with good history`() {
        val r = RunwayEngine.calculate(input(basis = SpendBasis.PLAN))
        assertEquals(SpendSource.PLAN, r.spendSource)
    }

    @Test fun `user can pin history even when it is short`() {
        val r = RunwayEngine.calculate(input(tx = listOf(spend(0, 300)), basis = SpendBasis.HISTORY))
        assertEquals(SpendSource.HISTORY, r.spendSource)
        assertEquals(rupees(300), r.dailySpend)
    }

    @Test fun `plan subtracts fixed and goal contributions`() {
        val rent = Commitment("rent", "Rent", rupees(3_000), dueDayOfMonth = 25)
        val goal = Goal("g", "Goa", rupees(8_000), 0, monthlyContribution = rupees(1_500))
        val r = RunwayEngine.calculate(input(commitments = listOf(rent), goals = listOf(goal), basis = SpendBasis.PLAN))
        assertEquals(rupees(250), r.dailySpend) // (12,000 - 3,000 - 1,500) / 30
    }

    @Test fun `even spread when there is no plan and no history`() {
        val r = RunwayEngine.calculate(input(income = IncomePlan(Cadence.MONTHLY, 0, today.plusDays(20)), tx = emptyList(), balance = 4_000))
        assertEquals(SpendSource.EVEN_SPREAD, r.spendSource)
        assertEquals(rupees(200), r.dailySpend)
        assertEquals(20, r.runwayDays)
        assertEquals(0, r.bufferDays)
    }

    @Test fun `history only counts your share of a split`() {
        val tx = (0L..13L).map { spend(it, 300, others = 100) }
        val r = RunwayEngine.calculate(input(tx = tx))
        assertEquals(rupees(200), r.dailySpend)
    }

    @Test fun `ignored and fixed-bucket transactions do not count as daily spending`() {
        val tx = steady + spend(0, 5_000, status = TxnStatus.IGNORED) + spend(1, 3_000, bucket = Bucket.FIXED)
        val r = RunwayEngine.calculate(input(tx = tx))
        assertEquals(rupees(200), r.dailySpend)
    }

    @Test fun `transactions waiting to be sorted still count`() {
        val tx = (0L..13L).map { spend(it, 200, status = TxnStatus.TO_SORT) }
        val r = RunwayEngine.calculate(input(tx = tx))
        assertEquals(rupees(200), r.dailySpend)
    }

    @Test fun `spending older than 14 days is ignored`() {
        val tx = steady + spend(20, 10_000)
        val r = RunwayEngine.calculate(input(tx = tx))
        assertEquals(rupees(200), r.dailySpend)
    }

    @Test fun `credits never count as spending`() {
        val tx = steady + Txn("in", rupees(5_000), Direction.CREDIT, today)
        val r = RunwayEngine.calculate(input(tx = tx))
        assertEquals(rupees(200), r.dailySpend)
    }

    // --- Edge cases ---

    @Test fun `zero spending gives an unlimited runway`() {
        val tx = listOf(Txn("in", rupees(100), Direction.CREDIT, today.minusDays(10)))
        val r = RunwayEngine.calculate(input(tx = tx, basis = SpendBasis.HISTORY, income = IncomePlan(Cadence.MONTHLY, 0, today.plusDays(10))))
        // History exists (span 11 days) but no spends -> history daily 0 is null; falls to even spread
        assertEquals(SpendSource.EVEN_SPREAD, r.spendSource)
    }

    @Test fun `history of zero spend is unlimited`() {
        val r = RunwayEngine.calculate(
            input(tx = listOf(spend(20, 100), spend(30, 50)), basis = SpendBasis.HISTORY, income = IncomePlan(Cadence.MONTHLY, 0, today.plusDays(10))),
        )
        assertTrue(r.unlimited)
        assertEquals(RunwayEngine.MAX_DAYS, r.runwayDays)
        assertTrue(r.makesIt)
    }

    @Test fun `negative fuel means zero runway and a shortfall`() {
        val rent = Commitment("rent", "Rent", rupees(9_000), dueDayOfMonth = 10)
        val r = RunwayEngine.calculate(input(balance = 8_000, commitments = listOf(rent)))
        assertEquals(rupees(-1_000), r.fuel)
        assertEquals(0, r.runwayDays)
        assertEquals(-19, r.bufferDays)
        assertFalse(r.makesIt)
        assertEquals(rupees(200) * 19 + rupees(1_000), r.shortfall)
        assertEquals(0, r.safeDaily)
    }

    @Test fun `short of next income shows the gap`() {
        val r = RunwayEngine.calculate(input(balance = 2_000))
        assertEquals(10, r.runwayDays)
        assertEquals(-9, r.bufferDays)
        assertEquals(rupees(1_800), r.shortfall)
        assertEquals(10_526, r.safeDaily) // ₹2,000 / 19 = ₹105.26
    }

    @Test fun `income date already passed with no money is overdue`() {
        val r = RunwayEngine.calculate(input(income = monthly(next = today.minusDays(2))))
        assertTrue(r.incomeOverdue)
        assertEquals(0, r.daysToNextIncome)
        assertEquals(40, r.bufferDays)
    }

    @Test fun `income date passed and money arrived rolls forward a month`() {
        val tx = steady + Txn("in", rupees(12_000), Direction.CREDIT, today.minusDays(2))
        val r = RunwayEngine.calculate(input(income = monthly(next = today.minusDays(2)), tx = tx))
        assertFalse(r.incomeOverdue)
        assertEquals(LocalDate.of(2026, 12, 1), r.nextIncomeDate)
        assertEquals(28, r.daysToNextIncome)
    }

    @Test fun `lump sum rolls forward by its cycle`() {
        val tx = steady + Txn("in", rupees(60_000), Direction.CREDIT, today.minusDays(1))
        val r = RunwayEngine.calculate(input(income = IncomePlan(Cadence.LUMP_SUM, rupees(60_000), today.minusDays(1), 120), tx = tx))
        assertEquals(today.plusDays(119), r.nextIncomeDate)
    }

    @Test fun `income arriving today means zero days to go`() {
        val r = RunwayEngine.calculate(input(income = monthly(next = today)))
        assertEquals(0, r.daysToNextIncome)
        assertFalse(r.incomeOverdue)
    }

    @Test fun `irregular income has a runway but no buffer`() {
        val r = RunwayEngine.calculate(input(income = IncomePlan(Cadence.IRREGULAR, 0, null)))
        assertNull(r.daysToNextIncome)
        assertNull(r.bufferDays)
        assertEquals(40, r.runwayDays)
        assertTrue(r.makesIt)
        assertEquals(rupees(8_000) / 30, r.safeDaily)
    }

    @Test fun `runway is capped at 999 days`() {
        val r = RunwayEngine.calculate(input(balance = 10_00_000))
        assertEquals(999, r.runwayDays)
    }

    @Test fun `zero balance gives zero runway`() {
        val r = RunwayEngine.calculate(input(balance = 0))
        assertEquals(0, r.runwayDays)
        assertEquals(0, r.safeDaily)
    }
}
