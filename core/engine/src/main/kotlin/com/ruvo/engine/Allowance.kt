package com.ruvo.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * "Make it last until [date]": for one big amount (a scholarship, a semester's money), what Fuel allows per day to reach
 * a chosen date, next to the usual daily spend. Pure maths and words; the screen decides when to show it.
 */
object Allowance {
    /** The card offers itself when the next money is further away than this. */
    const val LONG_GAP_DAYS = 45

    data class Plan(
        val until: LocalDate,
        val days: Int,
        /** Fuel ÷ days to the date. 0 when nothing is free to spend. */
        val daily: Paise,
        /** The usual daily spend from the engine, for comparison. */
        val usual: Paise,
    ) {
        val fits: Boolean get() = daily > 0 && usual <= daily
    }

    /** Shown when the next money is more than [LONG_GAP_DAYS] away, or when the user switched the card on. */
    fun offered(daysToNextIncome: Int?, switchedOn: Boolean): Boolean =
        switchedOn || (daysToNextIncome != null && daysToNextIncome > LONG_GAP_DAYS)

    /** The default date: the next money if known, else 30 days from today. */
    fun defaultUntil(r: RunwayResult): LocalDate =
        r.nextIncomeDate?.takeIf { it.isAfter(r.today) } ?: r.today.plusDays(30)

    fun plan(r: RunwayResult, until: LocalDate): Plan {
        val days = ChronoUnit.DAYS.between(r.today, until).toInt().coerceAtLeast(1)
        val daily = if (r.fuel > 0) r.fuel / days else 0L
        return Plan(until, days, daily, r.dailySpend)
    }

    /** One short, encouraging line. Never blames. */
    fun message(p: Plan): String {
        val date = IndianFormat.date(p.until)
        return when {
            p.daily <= 0 -> "Everything you have is already promised right now. When money comes in, this fills up again."
            p.usual <= 0 -> "${IndianFormat.rupees(p.daily)} a day gets you to $date. You have room to breathe."
            p.fits -> "Your usual ${IndianFormat.rupees(p.usual)} a day fits. Keep this pace and you reach $date with some to spare."
            else -> "That's ${IndianFormat.rupees(p.usual - p.daily)} less than your usual day. A few small swaps a week get you to $date."
        }
    }
}
