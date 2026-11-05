package com.ruvo.engine.guide

import com.ruvo.engine.Paise
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * The little calculators inside Guide books. Each one turns an idea into the student's own numbers, so "small
 * spends add up" becomes "₹300 a week is 11 days of your runway a year". Every result is an estimate for
 * learning, never advice, and the books say so. Money stays in paise; rates are display maths only.
 */
object Tools {

    /** Which calculator a book carries, with the words its page uses. */
    enum class Tool(val title: String, val line: String) {
        SMALL_SPENDS("Small spends, big year", "Slide a weekly habit and see what it adds up to."),
        GROWTH("Watch saving grow", "A small monthly amount, a few years, a little interest."),
        INFLATION("What ₹ will buy later", "Prices rise a little every year. See how much."),
        EMI("What a loan really costs", "Spread a price over months and see the extra you pay."),
        CARD_MINIMUM("The minimum-due trap", "Pay only the minimum on a card bill and see how long it lasts."),
    }

    /** A weekly habit over a year: the total, and how many days of your usual spending that is. */
    data class Habit(val perYear: Paise, val days: Int)

    fun smallSpends(perWeek: Paise, dailySpend: Paise): Habit {
        val year = perWeek * 52
        val days = if (dailySpend <= 0) 0 else (year / dailySpend).toInt()
        return Habit(year, days)
    }

    /** Saving [monthly] for [years] at [ratePct] a year, added monthly. What you put in, and what it becomes. */
    data class Growth(val putIn: Paise, val endsAt: Paise) {
        val earned: Paise get() = endsAt - putIn
    }

    fun growth(monthly: Paise, years: Int, ratePct: Double): Growth {
        val months = years * 12
        val r = ratePct / 100.0 / 12.0
        val end = if (r == 0.0) (monthly * months).toDouble() else monthly * ((1 + r).pow(months) - 1) / r * (1 + r)
        return Growth(monthly * months, end.roundToLong())
    }

    /** What [amount] will buy in [years], in today's money, if prices rise [ratePct] a year. */
    fun inflation(amount: Paise, years: Int, ratePct: Double): Paise =
        (amount / (1 + ratePct / 100.0).pow(years)).roundToLong()

    /** A loan of [principal] at [annualRatePct] over [months]: the monthly EMI and the total extra paid. */
    data class Loan(val emi: Paise, val totalPaid: Paise) {
        fun extra(principal: Paise): Paise = totalPaid - principal
    }

    fun emi(principal: Paise, annualRatePct: Double, months: Int): Loan {
        require(months > 0)
        val r = annualRatePct / 100.0 / 12.0
        val emi = if (r == 0.0) principal.toDouble() / months else principal * r * (1 + r).pow(months) / ((1 + r).pow(months) - 1)
        val e = emi.roundToLong()
        return Loan(e, e * months)
    }

    /** Paying only the minimum due on a card balance: how many months until it is cleared, and the interest paid. */
    data class Payoff(val months: Int, val interest: Paise, val cleared: Boolean)

    /**
     * Each month the balance gains [monthlyRatePct] interest, then the minimum is paid: [minPct] of the balance,
     * but never less than [floor]. No new spending. Stops after 30 years if it never clears.
     */
    fun cardMinimum(balance: Paise, monthlyRatePct: Double = 3.5, minPct: Double = 5.0, floor: Paise = 200_00): Payoff {
        var left = balance.toDouble()
        var interest = 0.0
        var months = 0
        while (left > 0.5 && months < 360) {
            val i = left * monthlyRatePct / 100.0
            interest += i
            left += i
            val pay = maxOf(left * minPct / 100.0, floor.toDouble()).coerceAtMost(left)
            left -= pay
            months++
        }
        return Payoff(months, interest.roundToLong(), left <= 0.5)
    }
}
