package com.ruvo.engine

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.floor

data class RunwayInput(
    val today: LocalDate,
    /** Money the student has right now, across accounts and cash. */
    val balance: Paise,
    val income: IncomePlan,
    val commitments: List<Commitment> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val transactions: List<Txn> = emptyList(),
    val basis: SpendBasis = SpendBasis.AUTO,
)

data class RunwayResult(
    val today: LocalDate,
    val balance: Paise,
    /** Already promised before the next money arrives. */
    val fixedReserved: Paise,
    /** Protected for goals. */
    val futureProtected: Paise,
    /** Free to spend: balance minus Fixed minus Future. Can be negative when over-committed. */
    val fuel: Paise,
    val dailySpend: Paise,
    val spendSource: SpendSource,
    /** Days the Fuel lasts at the daily spend. Capped at [RunwayEngine.MAX_DAYS]. */
    val runwayDays: Int,
    /** True when daily spend is zero, so the runway has no end. */
    val unlimited: Boolean,
    val nextIncomeDate: LocalDate?,
    /** Null for irregular income, where there is no date to make it to. */
    val daysToNextIncome: Int?,
    /** Runway minus days to next income. +4 means you make it with 4 days spare. */
    val bufferDays: Int?,
    /** The expected money date has passed and nothing arrived. */
    val incomeOverdue: Boolean,
    /** How much more money is needed to reach the next income at the current pace. 0 when fine. */
    val shortfall: Paise,
    /** What you could spend each day and still make it to the next money. */
    val safeDaily: Paise,
    val reservedCommitments: List<Commitment>,
) {
    val makesIt: Boolean get() = bufferDays == null || bufferDays >= 0
    val spareDays: Int get() = (bufferDays ?: 0).coerceAtLeast(0)
}

object RunwayEngine {
    const val MAX_DAYS = 999
    const val HISTORY_WINDOW_DAYS = 14
    /** Minimum span of history before the AUTO rule trusts it. */
    const val MIN_HISTORY_DAYS = 7
    const val MIN_HISTORY_TXNS = 3
    /** A commitment paid up to this many days before its due date counts as paid. */
    const val EARLY_PAY_GRACE_DAYS = 10L
    /** Overdue commitments older than this are assumed handled outside Ruvo. */
    const val OVERDUE_LOOKBACK_DAYS = 15L

    fun calculate(input: RunwayInput): RunwayResult {
        val today = input.today
        val (nextIncome, overdue) = effectiveNextIncome(input)
        val daysToNext = nextIncome?.let { ChronoUnit.DAYS.between(today, it).toInt().coerceAtLeast(0) }
        val horizonEnd = nextIncome ?: today.plusDays(input.income.cycleDays.toLong())

        val reserved = reservedCommitments(input.commitments, today, horizonEnd)
        val fixed = reserved.sumOf { it.amount }
        val protected = input.goals.sumOf { it.protectedAmount.coerceAtLeast(0) }
        val fuel = input.balance - fixed - protected

        val horizonDays = (daysToNext ?: input.income.cycleDays).coerceAtLeast(1)
        val (daily, source) = dailySpend(input, fuel, horizonDays)

        val unlimited = daily <= 0
        val runway = when {
            fuel <= 0 -> 0
            unlimited -> MAX_DAYS
            else -> floor(fuel.toDouble() / daily).toInt().coerceAtMost(MAX_DAYS)
        }
        val buffer = daysToNext?.let { runway - it }
        val shortfall = if (daysToNext != null && buffer != null && buffer < 0) {
            (daysToNext.toLong() * daily - fuel).coerceAtLeast(0)
        } else 0L
        val safeDaily = when {
            fuel <= 0 -> 0L
            daysToNext == null -> fuel / input.income.cycleDays.coerceAtLeast(1)
            daysToNext == 0 -> fuel
            else -> fuel / daysToNext
        }

        return RunwayResult(
            today = today,
            balance = input.balance,
            fixedReserved = fixed,
            futureProtected = protected,
            fuel = fuel,
            dailySpend = daily,
            spendSource = source,
            runwayDays = runway,
            unlimited = unlimited,
            nextIncomeDate = nextIncome,
            daysToNextIncome = daysToNext,
            bufferDays = buffer,
            incomeOverdue = overdue,
            shortfall = shortfall,
            safeDaily = safeDaily,
            reservedCommitments = reserved,
        )
    }

    /**
     * Works out when the next money is due. If the planned date has passed:
     * - money that arrived around that date rolls the plan forward by one cycle;
     * - otherwise the income is overdue and we count zero days to it.
     */
    internal fun effectiveNextIncome(input: RunwayInput): Pair<LocalDate?, Boolean> {
        val plan = input.income
        val planned = plan.nextDate ?: return null to false
        if (plan.cadence == Cadence.IRREGULAR) return null to false
        if (!planned.isBefore(input.today)) return planned to false

        val arrived = input.transactions.any {
            it.direction == Direction.CREDIT && it.kind == TxnKind.INCOME &&
                it.status != TxnStatus.IGNORED &&
                !it.date.isBefore(planned.minusDays(3)) && !it.date.isAfter(input.today)
        }
        if (!arrived) return planned to true

        var next = planned
        while (!next.isAfter(input.today)) {
            next = when (plan.cadence) {
                Cadence.MONTHLY -> next.plusMonths(1)
                else -> next.plusDays(plan.cycleDays.toLong())
            }
        }
        return next to false
    }

    /** Commitment occurrences that fall between today and the next income, plus recent unpaid ones. */
    internal fun reservedCommitments(
        commitments: List<Commitment>,
        today: LocalDate,
        horizonEnd: LocalDate,
    ): List<Commitment> {
        val out = mutableListOf<Commitment>()
        for (c in commitments) {
            if (c.amount <= 0) continue
            val occurrences = occurrences(c, today.minusDays(OVERDUE_LOOKBACK_DAYS), horizonEnd)
            for (due in occurrences) {
                val paid = c.lastPaidOn != null && !c.lastPaidOn.isBefore(due.minusDays(EARLY_PAY_GRACE_DAYS))
                // A past due date only stays reserved when we know the payment history and it was missed.
                // With no history (a new user), past occurrences are assumed already handled.
                val pastAndUnknown = due.isBefore(today) && c.lastPaidOn == null
                if (!paid && !pastAndUnknown && due.isBefore(horizonEnd)) out += c.copy(dueDate = due)
            }
        }
        return out.sortedBy { it.dueDate }
    }

    internal fun occurrences(c: Commitment, from: LocalDate, until: LocalDate): List<LocalDate> {
        if (!c.recurring || c.dueDayOfMonth == null) {
            val d = c.dueDate ?: return emptyList()
            return if (!d.isBefore(from) && d.isBefore(until)) listOf(d) else emptyList()
        }
        val result = mutableListOf<LocalDate>()
        var ym = YearMonth.from(from)
        val last = YearMonth.from(until)
        while (!ym.isAfter(last)) {
            val day = c.dueDayOfMonth.coerceIn(1, ym.lengthOfMonth())
            val d = ym.atDay(day)
            if (!d.isBefore(from) && d.isBefore(until)) result += d
            ym = ym.plusMonths(1)
        }
        return result
    }

    /**
     * The daily-spend rule. AUTO uses recent history when there is enough of it (7+ days and 3+ spends),
     * otherwise the plan, otherwise an even spread of Fuel to the next income.
     */
    internal fun dailySpend(input: RunwayInput, fuel: Paise, horizonDays: Int): Pair<Paise, SpendSource> {
        val history = historyDaily(input.transactions, input.today)
        val plan = planDaily(input)
        val even = if (fuel > 0) fuel / horizonDays else 0L

        return when (input.basis) {
            SpendBasis.HISTORY -> when {
                history != null -> history.daily to SpendSource.HISTORY
                plan != null -> plan to SpendSource.PLAN
                else -> even to SpendSource.EVEN_SPREAD
            }
            SpendBasis.PLAN -> when {
                plan != null -> plan to SpendSource.PLAN
                history != null -> history.daily to SpendSource.HISTORY
                else -> even to SpendSource.EVEN_SPREAD
            }
            SpendBasis.AUTO -> when {
                history != null && history.trusted -> history.daily to SpendSource.HISTORY
                plan != null -> plan to SpendSource.PLAN
                history != null -> history.daily to SpendSource.HISTORY
                else -> even to SpendSource.EVEN_SPREAD
            }
        }
    }

    data class History(val daily: Paise, val spanDays: Int, val count: Int) {
        val trusted: Boolean get() = spanDays >= MIN_HISTORY_DAYS && count >= MIN_HISTORY_TXNS
    }

    /** Average daily Fuel spending over the last 14 days, counting only your share of splits. */
    fun historyDaily(transactions: List<Txn>, today: LocalDate): History? {
        val spends = transactions.filter { it.countsAsSpending && it.bucket == Bucket.FUEL && !it.date.isAfter(today) }
        if (spends.isEmpty()) return null
        val windowStart = today.minusDays((HISTORY_WINDOW_DAYS - 1).toLong())
        val firstEver = transactions.filter { !it.date.isAfter(today) }.minOf { it.date }
        val start = if (firstEver.isAfter(windowStart)) firstEver else windowStart
        val span = (ChronoUnit.DAYS.between(start, today) + 1).toInt()
        val inWindow = spends.filter { !it.date.isBefore(start) }
        if (inWindow.isEmpty()) return History(0, span, 0)
        val total = inWindow.sumOf { it.yourShare }
        return History(total / span, span, inWindow.size)
    }

    /** What the plan allows per day: income minus recurring Fixed minus goal contributions, over the cycle. */
    fun planDaily(input: RunwayInput): Paise? {
        val plan = input.income
        if (plan.expectedAmount <= 0) return null
        val monthsPerCycle = plan.cycleDays / 30.0
        val fixedPerCycle = input.commitments.filter { it.recurring }.sumOf { it.amount } * monthsPerCycle
        val goalsPerCycle = input.goals.filter { !it.isComplete }.sumOf { it.monthlyContribution } * monthsPerCycle
        val free = plan.expectedAmount - fixedPerCycle - goalsPerCycle
        if (free <= 0) return 0
        return (free / plan.cycleDays).toLong()
    }
}
