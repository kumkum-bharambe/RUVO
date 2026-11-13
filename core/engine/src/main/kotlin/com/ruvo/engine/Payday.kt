package com.ruvo.engine

import java.time.LocalDate

/**
 * The payday moment: money has just arrived, so Home can say "you made it" and offer to pay yourself first by pouring
 * a little into a goal before spending starts. Pure maths, no screens; the suggestion is only ever an offer.
 */
object Payday {
    /** Money in counts as a payday for this many days after it lands. */
    const val WINDOW_DAYS = 2L
    /** With no expected income (money that varies), anything from this amount counts. */
    private val FLOOR: Paise = rupees(1_000)

    /** The latest money-in from the last [WINDOW_DAYS] days that is big enough to be income, not a refund. */
    fun recent(transactions: List<Txn>, today: LocalDate, expectedIncome: Paise): Txn? {
        val threshold = maxOf(FLOOR, expectedIncome * 3 / 10)
        return transactions
            .filter { it.direction == Direction.CREDIT && it.status != TxnStatus.IGNORED }
            .filter { !it.date.isAfter(today) && !it.date.isBefore(today.minusDays(WINDOW_DAYS)) }
            .filter { it.amount >= threshold }
            .maxWithOrNull(compareBy<Txn> { it.date }.thenBy { it.amount })
    }

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
