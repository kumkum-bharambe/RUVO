package com.ruvo.engine

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Read-only summaries of what already happened: daily spending, the week in review, month by month.
 * The runway maths stays in RunwayEngine; these only count recorded transactions.
 */
object Insights {

    data class Day(val date: LocalDate, val spent: Paise)

    /** Your share of spending for each of the last [days] days, oldest first, ending today. */
    fun dailySpending(txns: List<Txn>, today: LocalDate, days: Int = 7): List<Day> =
        (days - 1 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            Day(d, txns.filter { it.countsAsSpending && it.date == d }.sumOf { it.yourShare })
        }

    data class CategoryChange(val category: String, val thisPeriod: Paise, val lastPeriod: Paise) {
        /** Whole-number percentage change, or null when there is nothing to compare with. */
        val changePercent: Int? get() = if (lastPeriod <= 0) null else Math.round((thisPeriod - lastPeriod) * 100.0 / lastPeriod).toInt()
    }

    data class Week(
        val total: Paise,
        val lastWeek: Paise,
        val categories: List<CategoryChange>,
        /** The single largest spend this week, worth a second look. */
        val biggest: Txn?,
    ) {
        val changePercent: Int? get() = if (lastWeek <= 0) null else Math.round((total - lastWeek) * 100.0 / lastWeek).toInt()
    }

    /** The last 7 days including today, compared with the 7 days before. */
    fun week(txns: List<Txn>, today: LocalDate): Week {
        val spend = txns.filter { it.countsAsSpending }
        fun inWindow(t: Txn, from: Long, to: Long) = ChronoUnit.DAYS.between(t.date, today) in from..to
        val now = spend.filter { inWindow(it, 0, 6) }
        val before = spend.filter { inWindow(it, 7, 13) }
        val cats = (now.map { it.category } + before.map { it.category }).distinct().map { cat ->
            CategoryChange(cat, now.filter { it.category == cat }.sumOf { it.yourShare }, before.filter { it.category == cat }.sumOf { it.yourShare })
        }.filter { it.thisPeriod > 0 }.sortedByDescending { it.thisPeriod }
        return Week(now.sumOf { it.yourShare }, before.sumOf { it.yourShare }, cats, now.maxByOrNull { it.yourShare })
    }

    data class Month(val month: YearMonth, val received: Paise, val spent: Paise) {
        val leftOver: Paise get() = received - spent
    }

    /** Money received and spent in each of the last [count] calendar months, oldest first. */
    fun months(txns: List<Txn>, today: LocalDate, count: Int = 3): List<Month> {
        val current = YearMonth.from(today)
        return (count - 1 downTo 0).map { back ->
            val ym = current.minusMonths(back.toLong())
            val inMonth = txns.filter { YearMonth.from(it.date) == ym && it.status != TxnStatus.IGNORED }
            Month(
                ym,
                received = inMonth.filter { it.direction == Direction.CREDIT && it.kind == TxnKind.INCOME }.sumOf { it.amount },
                spent = inMonth.filter { it.countsAsSpending }.sumOf { it.yourShare },
            )
        }
    }

    /** Spending by category over the last [days] days, largest first. */
    fun byCategory(txns: List<Txn>, today: LocalDate, days: Int = 30): List<Pair<String, Paise>> =
        txns.filter { it.countsAsSpending && ChronoUnit.DAYS.between(it.date, today) in 0 until days }
            .groupBy { it.category }
            .map { (k, v) -> k to v.sumOf { it.yourShare } }
            .sortedByDescending { it.second }

    enum class MoodLevel { COMFORTABLE, WATCH, PLAN }

    data class Mood(val level: MoodLevel, val title: String, val line: String)

    /** A calm one-line read of how the month is going. Never alarming, never shaming. */
    fun mood(r: RunwayResult): Mood {
        val level = when {
            r.unlimited -> MoodLevel.COMFORTABLE
            r.bufferDays != null -> when {
                r.bufferDays >= 5 -> MoodLevel.COMFORTABLE
                r.bufferDays >= 0 -> MoodLevel.WATCH
                else -> MoodLevel.PLAN
            }
            r.runwayDays >= 21 -> MoodLevel.COMFORTABLE
            r.runwayDays >= 10 -> MoodLevel.WATCH
            else -> MoodLevel.PLAN
        }
        return when (level) {
            MoodLevel.COMFORTABLE -> Mood(level, "Feeling comfortable", "Your Fuel comfortably covers the days ahead.")
            MoodLevel.WATCH -> Mood(level, "Keep an eye on it", "You make it, with a thin cushion. Small choices matter this week.")
            MoodLevel.PLAN -> Mood(level, "Let's plan together", "At this pace Fuel runs out before your next money. A small plan fixes that.")
        }
    }
}
