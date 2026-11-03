package com.ruvo.engine

import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToLong

/** Before you buy: what a price costs in days. Ruvo never blocks a purchase; it only shows consequences. */
object BeforeYouBuy {

    enum class Verdict { COMFORTABLE, TIGHT, SHORT }

    data class Result(
        val price: Paise,
        /** Cost in days of your usual spending, to one decimal place. */
        val costDays: Double,
        val runwayAfter: Int,
        val bufferAfter: Int?,
        val fuelAfter: Paise,
        val verdict: Verdict,
        /** What a day could cost from now on to still make it, if you buy. */
        val safeDailyAfter: Paise,
    )

    fun evaluate(current: RunwayResult, price: Paise): Result {
        require(price >= 0) { "price must not be negative" }
        val daily = current.dailySpend
        val costDays = if (daily <= 0) 0.0 else Math.round(price.toDouble() / daily * 10) / 10.0
        val fuelAfter = current.fuel - price
        val runwayAfter = when {
            fuelAfter <= 0 -> 0
            daily <= 0 -> RunwayEngine.MAX_DAYS
            else -> (fuelAfter / daily).toInt().coerceAtMost(RunwayEngine.MAX_DAYS)
        }
        val bufferAfter = current.daysToNextIncome?.let { runwayAfter - it }
        val verdict = when {
            fuelAfter < 0 -> Verdict.SHORT
            bufferAfter == null -> if (runwayAfter >= 7) Verdict.COMFORTABLE else Verdict.TIGHT
            bufferAfter < 0 -> Verdict.SHORT
            bufferAfter < 3 -> Verdict.TIGHT
            else -> Verdict.COMFORTABLE
        }
        val days = current.daysToNextIncome
        val safeAfter = when {
            fuelAfter <= 0 -> 0L
            days == null || days == 0 -> fuelAfter
            else -> fuelAfter / days
        }
        return Result(price, costDays, runwayAfter, bufferAfter, fuelAfter, verdict, safeAfter)
    }
}

/** Split as a gesture: mark a payment shared, track who owes you, settle by UPI link. Ruvo never moves money. */
object Splits {

    data class Share(val person: String, val amount: Paise, val paid: Boolean = false)

    data class Split(val txnId: String, val total: Paise, val shares: List<Share>) {
        val othersTotal: Paise get() = shares.sumOf { it.amount }
        val yourShare: Paise get() = (total - othersTotal).coerceAtLeast(0)
        val owedToYou: Paise get() = shares.filter { !it.paid }.sumOf { it.amount }
    }

    /**
     * Divides [total] equally between you and [people]. Any paisa left over from rounding stays with you,
     * so the shares always add up to the total exactly.
     */
    fun equal(txnId: String, total: Paise, people: List<String>): Split {
        require(people.isNotEmpty()) { "a split needs at least one other person" }
        val each = total / (people.size + 1)
        return Split(txnId, total, people.map { Share(it, each) })
    }

    fun owedToYou(splits: List<Split>): Paise = splits.sumOf { it.owedToYou }

    /** UPI payment link a friend can tap to pay you back. Amount in rupees with two decimals. */
    fun upiLink(payeeVpa: String, payeeName: String, amount: Paise, note: String): String {
        fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        val rupees = "%d.%02d".format(amount / 100, amount % 100)
        return "upi://pay?pa=${enc(payeeVpa)}&pn=${enc(payeeName)}&am=$rupees&cu=INR&tn=${enc(note)}"
    }
}

/** Money arrives: set aside Fixed first, then suggest an amount for Future. */
object MoneyArrives {

    data class Suggestion(
        val received: Paise,
        val setAsideFixed: Paise,
        val suggestFuture: Paise,
        val leavesFuel: Paise,
    )

    fun suggest(received: Paise, commitments: List<Commitment>, goals: List<Goal>): Suggestion {
        val fixed = commitments.filter { it.recurring }.sumOf { it.amount }.coerceAtMost(received)
        val afterFixed = received - fixed
        val goalNeed = goals.filter { !it.isComplete }.sumOf { it.monthlyContribution }
        // Suggest the goals' monthly need, but never more than 20% of what is left, rounded down to ₹100.
        val cap = (afterFixed * 0.2).roundToLong()
        val raw = if (goalNeed > 0) minOf(goalNeed, cap) else (afterFixed * 0.1).roundToLong()
        val future = (raw / 10_000) * 10_000
        return Suggestion(received, fixed, future, afterFixed - future)
    }
}

/** The day strip on Today: one cell per day from today to the next income (or to the runway end). */
object DayStrip {

    enum class State { SPENT_TODAY, COVERED, SHORT, INCOME }

    data class Day(val date: LocalDate, val state: State, val allowance: Paise, val isToday: Boolean)

    fun build(result: RunwayResult, maxDays: Int = 45): List<Day> {
        val toIncome = result.daysToNextIncome
        val length = when {
            toIncome != null -> toIncome + 1
            result.unlimited -> 30
            else -> result.runwayDays + 1
        }.coerceIn(1, maxDays)
        return (0 until length).map { i ->
            val date = result.today.plusDays(i.toLong())
            val isIncome = toIncome != null && i == toIncome && i > 0
            val state = when {
                isIncome -> State.INCOME
                i == 0 -> State.SPENT_TODAY
                result.unlimited || i < result.runwayDays -> State.COVERED
                else -> State.SHORT
            }
            Day(date, state, result.safeDaily, i == 0)
        }
    }

    /** Fraction of each day that is funded: 1.0 for covered days, partial for the day money runs out. */
    fun fill(result: RunwayResult, dayIndex: Int): Float {
        if (result.unlimited) return 1f
        if (result.fuel <= 0 || result.dailySpend <= 0) return if (result.fuel > 0) 1f else 0f
        val exact = result.fuel.toDouble() / result.dailySpend
        return (exact - dayIndex).coerceIn(0.0, 1.0).toFloat()
    }

    /** Whole days of Fuel, rounded up, for "money runs out on 24 Nov" copy. */
    fun runsOutOn(result: RunwayResult): LocalDate? {
        if (result.unlimited) return null
        val days = if (result.dailySpend <= 0) 0 else ceil(result.fuel.toDouble() / result.dailySpend).toLong()
        return result.today.plusDays(days.coerceAtLeast(0))
    }
}
