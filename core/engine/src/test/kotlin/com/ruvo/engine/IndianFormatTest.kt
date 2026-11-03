package com.ruvo.engine

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class IndianFormatTest {

    @Test fun `groups in the Indian style`() {
        assertEquals("999", IndianFormat.group(999))
        assertEquals("1,000", IndianFormat.group(1_000))
        assertEquals("12,071", IndianFormat.group(12_071))
        assertEquals("1,25,000", IndianFormat.group(1_25_000))
        assertEquals("12,34,56,789", IndianFormat.group(12_34_56_789))
    }

    @Test fun `formats rupees with sign and optional paise`() {
        assertEquals("₹1,25,000", IndianFormat.rupees(rupees(1_25_000)))
        assertEquals("-₹450", IndianFormat.rupees(rupees(-450)))
        assertEquals("₹450.50", IndianFormat.rupees(45_050, showPaise = true))
        assertEquals("₹0", IndianFormat.rupees(0))
    }

    @Test fun `compact amounts`() {
        assertEquals("₹850", IndianFormat.compact(rupees(850)))
        assertEquals("₹12.5k", IndianFormat.compact(rupees(12_500)))
        assertEquals("₹1.2L", IndianFormat.compact(rupees(1_20_000)))
        assertEquals("₹2L", IndianFormat.compact(rupees(2_00_000)))
    }

    @Test fun `dates in Indian style`() {
        val today = LocalDate.of(2026, 11, 3)
        assertEquals("3 Nov", IndianFormat.date(today, today))
        assertEquals("1 Jan 2027", IndianFormat.date(LocalDate.of(2027, 1, 1), today))
        assertEquals("Today", IndianFormat.relativeDay(today, today))
        assertEquals("Yesterday", IndianFormat.relativeDay(today.minusDays(1), today))
        assertEquals("31 Oct", IndianFormat.relativeDay(today.minusDays(3), today))
    }

    @Test fun `spoken amounts for screen readers`() {
        assertEquals("12,071 rupees", IndianFormat.spoken(rupees(12_071)))
        assertEquals("450 rupees and 50 paise", IndianFormat.spoken(45_050))
        assertEquals("4 days", IndianFormat.days(4))
        assertEquals("1 day", IndianFormat.days(1))
    }
}
