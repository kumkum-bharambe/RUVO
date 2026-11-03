package com.ruvo.engine

import java.time.LocalDate

/**
 * All money is held in paise (1 rupee = 100 paise) as Long, so no rounding drift ever reaches the screen.
 */
typealias Paise = Long

fun rupees(amount: Long): Paise = amount * 100
fun rupees(amount: Double): Paise = Math.round(amount * 100)

/** How money reaches the student. Five personas sit on top of these three cadences. */
enum class Cadence { MONTHLY, LUMP_SUM, IRREGULAR }

data class IncomePlan(
    val cadence: Cadence,
    /** Usual amount per arrival. 0 when unknown. */
    val expectedAmount: Paise,
    /** Next expected arrival. Null for irregular income. */
    val nextDate: LocalDate?,
    /** Length of one cycle in days: ~30 for monthly, ~120 for a semester lump sum. */
    val cycleDays: Int = defaultCycleDays(cadence),
) {
    companion object {
        fun defaultCycleDays(cadence: Cadence) = when (cadence) {
            Cadence.MONTHLY -> 30
            Cadence.LUMP_SUM -> 120
            Cadence.IRREGULAR -> 30
        }
    }
}

/** Something already promised: rent, mess, recharge, a subscription. Feeds the Fixed bucket. */
data class Commitment(
    val id: String,
    val name: String,
    val amount: Paise,
    /** Day of month it falls due (1..31) for recurring commitments. */
    val dueDayOfMonth: Int? = null,
    /** Exact date for one-off commitments. */
    val dueDate: LocalDate? = null,
    val recurring: Boolean = true,
    /** Date it was last paid, if known. An occurrence paid up to 10 days early counts as paid. */
    val lastPaidOn: LocalDate? = null,
)

enum class Bucket { FIXED, FUEL, FUTURE }

enum class Direction { DEBIT, CREDIT }

enum class TxnStatus { TO_SORT, CONFIRMED, IGNORED }

enum class TxnKind {
    /** Ordinary spending or a payment for a commitment. */
    SPEND,
    /** Pocket money, salary, scholarship, freelance pay. */
    INCOME,
    /** A friend paying back their share of a split. Not income. */
    SETTLEMENT,
    /** Money moved between your own buckets or accounts. Never counts as spending. */
    TRANSFER,
}

data class Txn(
    val id: String,
    val amount: Paise,
    val direction: Direction,
    val date: LocalDate,
    val kind: TxnKind = if (direction == Direction.DEBIT) TxnKind.SPEND else TxnKind.INCOME,
    val bucket: Bucket = Bucket.FUEL,
    val status: TxnStatus = TxnStatus.CONFIRMED,
    val category: String = "Other",
    val merchant: String = "",
    /** For a split: the part other people owe you. Your own spending is amount minus this. */
    val othersShare: Paise = 0,
) {
    val yourShare: Paise get() = (amount - othersShare).coerceAtLeast(0)
    val countsAsSpending: Boolean
        get() = direction == Direction.DEBIT && kind == TxnKind.SPEND && status != TxnStatus.IGNORED
}

data class Goal(
    val id: String,
    val name: String,
    val target: Paise,
    /** Money already set aside for this goal. Excluded from Fuel everywhere. */
    val protectedAmount: Paise,
    val targetDate: LocalDate? = null,
    val monthlyContribution: Paise = 0,
) {
    val progress: Double get() = if (target <= 0) 1.0 else (protectedAmount.toDouble() / target).coerceIn(0.0, 1.0)
    val isComplete: Boolean get() = protectedAmount >= target && target > 0
}

/** Where the daily spending figure comes from. AUTO lets the rule decide; the user can pin either source. */
enum class SpendBasis { AUTO, HISTORY, PLAN }

/** The source the engine actually used, shown to the user in "Why 23 days?". */
enum class SpendSource { HISTORY, PLAN, EVEN_SPREAD }
