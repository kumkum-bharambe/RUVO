package com.ruvo.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AccountTest {

    @Test fun pinMatchesOnlyItself() {
        val stored = Pins.hash("2580")
        assertTrue(Pins.matches("2580", stored))
        assertFalse(Pins.matches("2581", stored))
        assertFalse(Pins.matches("", stored))
    }

    @Test fun pinIsNeverStoredAndSaltDiffers() {
        val a = Pins.hash("1234")
        val b = Pins.hash("1234")
        assertFalse("1234" in a)
        assertNotEquals(a, b)
        assertTrue(Pins.matches("1234", a) && Pins.matches("1234", b))
    }

    @Test fun brokenHashNeverMatches() {
        assertFalse(Pins.matches("1234", ""))
        assertFalse(Pins.matches("1234", "v1:not-base64:also-not"))
        assertFalse(Pins.matches("1234", "v2:AAAA:AAAA"))
    }

    @Test fun pinMustBeFourDigits() {
        assertTrue(Pins.isValid("0000"))
        assertFalse(Pins.isValid("123"))
        assertFalse(Pins.isValid("12345"))
        assertFalse(Pins.isValid("12a4"))
    }

    @Test fun mobileNumbers() {
        assertTrue(Mobile.isValid("98765 43210"))
        assertTrue(Mobile.isValid("+91 98765 43210"))
        assertTrue(Mobile.isValid("098765 43210"))
        assertFalse(Mobile.isValid("12345 67890"))
        assertFalse(Mobile.isValid("98765"))
        assertEquals("9876543210", Mobile.normalise("+91-98765-43210"))
        assertEquals("98•••••210", Mobile.masked("9876543210"))
    }
}
