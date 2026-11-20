package com.ruvo.engine

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaidThisMonthTest {
    private fun rent(paid: LocalDate?, day: Int = 5) = Commitment("r", "Rent", rupees(6_000), dueDayOfMonth = day, lastPaidOn = paid)

    @Test fun neverPaidIsUnpaid() = assertFalse(rent(null).paidThisMonth(LocalDate.of(2026, 11, 20)))

    @Test fun paidThisMonthIsPaid() = assertTrue(rent(LocalDate.of(2026, 11, 3)).paidThisMonth(LocalDate.of(2026, 11, 20)))

    @Test fun goesBackToUnpaidWhenTheNextMonthStarts() {
        val r = rent(LocalDate.of(2026, 11, 3))
        assertTrue(r.paidThisMonth(LocalDate.of(2026, 11, 30)))
        assertFalse(r.paidThisMonth(LocalDate.of(2026, 12, 1)))
    }

    @Test fun payingAFewDaysEarlyCountsForThatMonth() {
        // Due on the 1st, paid on 25 Nov: December shows Paid.
        assertTrue(rent(LocalDate.of(2026, 11, 25), day = 1).paidThisMonth(LocalDate.of(2026, 12, 2)))
    }

    @Test fun oneOffStaysPaid() {
        val c = Commitment("o", "Deposit", rupees(2_000), dueDate = LocalDate.of(2026, 10, 10), recurring = false, lastPaidOn = LocalDate.of(2026, 10, 9))
        assertTrue(c.paidThisMonth(LocalDate.of(2026, 12, 1)))
    }
}
