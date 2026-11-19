package com.ruvo.engine.guide

import com.ruvo.engine.IndianFormat
import com.ruvo.engine.RunwayResult
import java.time.LocalDate

/**
 * What the phone's notifications say: offers and books the student has not seen yet, and the morning update. Pure
 * logic; the app decides when to post them and keeps track of what was seen. Everything ships inside the app, so
 * "new" means new since the student last heard about it (for example after an update), never anything fetched.
 */
object WhatsNew {

    /** Current, recently checked offers the student has not been told about. Ended or stale ones never count. */
    fun offers(seen: Set<String>, today: LocalDate): List<Perks.Perk> =
        Perks.listed(today)
            .filter { Perks.status(it, today).state.let { s -> s == Perks.State.ACTIVE || s == Perks.State.ENDING } }
            .filter { it.id !in seen }

    /** Guide books the student has not been told about. */
    fun books(seen: Set<String>): List<Library.Book> = Library.all.filter { it.id !in seen }

    /** The morning update: the runway in days, today's amount, and any payments waiting to be confirmed. */
    fun daily(r: RunwayResult, toConfirm: Int): Pair<String, String> {
        val title = if (r.unlimited) "Good morning · your money looks set" else "Good morning · ${IndianFormat.days(r.runwayDays)} of runway"
        val next = r.nextIncomeDate
        val today = when {
            r.unlimited -> "Spending is near zero, so your runway has no end."
            next != null && r.safeDaily > 0 && (r.daysToNextIncome ?: 0) > 0 ->
                "Up to ${IndianFormat.rupees(r.safeDaily)} a day gets you to ${IndianFormat.date(next, r.today)}."
            else -> "At your usual pace of ${IndianFormat.rupees(r.dailySpend)} a day."
        }
        val confirm = when (toConfirm) {
            0 -> ""
            1 -> " 1 payment to confirm."
            else -> " $toConfirm payments to confirm."
        }
        return title to today + confirm
    }
}
