package com.ruvo.sms

import java.time.Duration

/**
 * The bank and the UPI app often both send an SMS for the same payment. Two parsed messages are the same
 * payment when amount and direction match and either the references match, or one has no reference and
 * they arrived within ten minutes of each other. Duplicates merge silently.
 */
object Deduper {
    val WINDOW: Duration = Duration.ofMinutes(10)

    fun isDuplicate(a: ParsedTxn, b: ParsedTxn): Boolean {
        if (a.amount != b.amount || a.direction != b.direction) return false
        if (a.reference != null && b.reference != null) return a.reference == b.reference
        val gap = Duration.between(a.receivedAt, b.receivedAt).abs()
        return gap <= WINDOW
    }

    /** Keeps the first of each duplicate group, filling gaps (reference, balance, merchant) from the others. */
    fun merge(items: List<ParsedTxn>): List<ParsedTxn> {
        val out = mutableListOf<ParsedTxn>()
        for (t in items.sortedBy { it.receivedAt }) {
            val i = out.indexOfFirst { isDuplicate(it, t) }
            if (i < 0) {
                out += t
            } else {
                val kept = out[i]
                out[i] = kept.copy(
                    reference = kept.reference ?: t.reference,
                    balanceAfter = kept.balanceAfter ?: t.balanceAfter,
                    merchant = if (kept.category == "Other" && t.category != "Other") t.merchant else kept.merchant,
                    category = if (kept.category == "Other") t.category else kept.category,
                    rawCounterparty = kept.rawCounterparty.ifEmpty { t.rawCounterparty },
                )
            }
        }
        return out
    }
}
