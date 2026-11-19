package com.ruvo.engine

import java.time.LocalDate

/**
 * The payday moment: money has just arrived, so Home can say "you made it" and offer to pay yourself first by pouring
 * a little into a goal before spending starts. Pure maths, no screens; the suggestion is only ever an offer.
 */
object Payday {
    /** Money in counts as a payday for this many days after it lands. */
    const val WINDOW_DAYS = 2L
    /** With no income history and no expected income (money that varies), anything from this amount counts. */
    private val FLOOR: Paise = rupees(1_000)
    /** With income history, money from this amount can count, so ₹1,500 a week is still a payday. */
    private val HISTORY_FLOOR: Paise = rupees(500)

    /** The latest money-in from the last [WINDOW_DAYS] days that is big enough to be income, not a refund. */
    fun recent(transactions: List<Txn>, today: LocalDate, expectedIncome: Paise): Txn? {
        val incomes = transactions.filter { it.isIncomeDeposit }
        return incomes
            .filter { !it.date.isAfter(today) && !it.date.isBefore(today.minusDays(WINDOW_DAYS)) }
            .filter { it.amount >= threshold(incomes, it, expectedIncome) }
            .maxWithOrNull(compareBy<Txn> { it.date }.thenBy { it.amount })
    }

    /**
     * How big money-in must be to count as income: 30% of the median of the last three income deposits before it,
     * never under ₹500. With no earlier income, the old rule: 30% of the expected income, never under ₹1,000.
     */
    fun threshold(incomes: List<Txn>, candidate: Txn, expectedIncome: Paise): Paise {
        val earlier = incomes
            .filter { it.id != candidate.id && !it.date.isAfter(candidate.date) }
            .sortedByDescending { it.date }
            .take(3)
            .map { it.amount }
        if (earlier.isEmpty()) return maxOf(FLOOR, expectedIncome * 3 / 10)
        return maxOf(HISTORY_FLOOR, median(earlier) * 3 / 10)
    }

    private fun median(values: List<Paise>): Paise {
        val s = values.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }

    /** Real income: not a refund, a friend settling up, an ignored message or a top-up from home. */
    private val Txn.isIncomeDeposit: Boolean
        get() = direction == Direction.CREDIT && kind == TxnKind.INCOME && status != TxnStatus.IGNORED && !topUp

    /**
     * True when the money that just arrived was the expected payday: the next money is now close to a full cycle
     * away (within a week of it). Money that lands mid-cycle (a refund, a gift, a top-up) is just "money in".
     * With no next date (money that varies) every arrival counts.
     */
    fun isPayday(daysToNextIncome: Int?, cycleDays: Int): Boolean =
        daysToNextIncome == null || daysToNextIncome >= cycleDays - 7

    data class Pour(val goal: Goal, val amount: Paise)

    /**
     * What to offer to pour into a goal: the open goal with a monthly amount first (else the first open goal), its own
     * monthly amount (else a tenth of the income, in whole hundreds), never more than the goal still needs or the
     * Fuel there is. Null when there is nothing sensible to offer (no open goal, or under ₹100).
     */
    fun suggestPour(income: Paise, goals: List<Goal>, fuel: Paise): Pour? {
        val open = goals.filter { !it.isComplete }
        val goal = open.firstOrNull { it.monthlyContribution > 0 } ?: open.firstOrNull() ?: return null
        val wanted = if (goal.monthlyContribution > 0) goal.monthlyContribution else income / 10 / rupees(100) * rupees(100)
        val amount = minOf(wanted, goal.target - goal.protectedAmount, fuel) / rupees(100) * rupees(100)
        return if (amount >= rupees(100)) Pour(goal, amount) else null
    }
}
