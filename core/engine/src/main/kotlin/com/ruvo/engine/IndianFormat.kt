package com.ruvo.engine

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Indian number grouping (₹1,25,000) and Indian date style (3 Nov). */
object IndianFormat {

    /** ₹1,25,000 or ₹1,25,000.50 when there are paise. Negative amounts get a leading minus. */
    fun rupees(paise: Paise, showPaise: Boolean = false): String {
        val negative = paise < 0
        val abs = kotlin.math.abs(paise)
        val whole = group(abs / 100)
        val frac = abs % 100
        val body = if (showPaise) "$whole.%02d".format(frac) else whole
        return (if (negative) "-₹" else "₹") + body
    }

    /** 125000 -> "1,25,000". The last three digits form one group; the rest go in pairs. */
    fun group(n: Long): String {
        val s = n.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        var rest = s.dropLast(3)
        val parts = ArrayDeque<String>()
        while (rest.length > 2) {
            parts.addFirst(rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.addFirst(rest)
        return parts.joinToString(",") + "," + last3
    }

    /** Short spoken-style amount for tight spaces: ₹1.2L, ₹12.5k, ₹850. */
    fun compact(paise: Paise): String {
        val r = paise / 100.0
        return when {
            kotlin.math.abs(r) >= 1_00_000 -> "₹" + trim(r / 1_00_000) + "L"
            kotlin.math.abs(r) >= 1_000 -> "₹" + trim(r / 1_000) + "k"
            else -> "₹" + r.toLong()
        }
    }

    private fun trim(v: Double): String {
        val one = Math.round(v * 10) / 10.0
        return if (one % 1.0 == 0.0) one.toLong().toString() else one.toString()
    }

    /** "3 Nov", or "3 Nov 2027" when the year differs from [today]'s. */
    fun date(d: LocalDate, today: LocalDate? = null): String {
        val month = d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return if (today != null && today.year != d.year) "${d.dayOfMonth} $month ${d.year}" else "${d.dayOfMonth} $month"
    }

    /** "Today", "Yesterday", "Tomorrow", or the short date. */
    fun relativeDay(d: LocalDate, today: LocalDate): String = when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> date(d, today)
    }

    /** "1 day", "4 days". */
    fun days(n: Int): String = if (n == 1 || n == -1) "$n day" else "$n days"

    /** What a screen reader says for an amount: "12,071 rupees". TalkBack reads Indian grouping naturally. */
    fun spoken(paise: Paise): String {
        val r = paise / 100
        val p = kotlin.math.abs(paise % 100)
        val base = (if (paise < 0) "minus " else "") + group(kotlin.math.abs(r)) + " rupees"
        return if (p != 0L) "$base and $p paise" else base
    }
}
