package com.ruvo.sms

import com.ruvo.engine.Direction
import com.ruvo.engine.rupees
import org.junit.Test
import java.time.LocalDateTime
import kotlin.math.roundToLong
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmsParserTest {

    private val parser = SmsParser()

    private fun parse(sender: String, body: String) = parser.parse(SmsMessage(sender, body, SampleSms.receivedAt))

    /** Each sample counts as correct only when amount, direction, merchant and date all match (or it is rightly ignored). */
    @Test fun `parser is at least 95 percent right on the sample set`() {
        val misses = mutableListOf<String>()
        for (s in SampleSms.all) {
            val p = parse(s.sender, s.body)
            val e = s.expect
            val ok = if (e == null) p == null else p != null &&
                p.amount == (e.rupees * 100).roundToLong() &&
                p.direction == e.direction &&
                p.merchant == e.merchant &&
                p.date == e.date &&
                (e.category == null || p.category == e.category)
            if (!ok) misses += "${s.sender}: expected $e, got ${p?.let { "${it.amount} ${it.direction} '${it.merchant}' ${it.category} ${it.date}" }}"
        }
        val accuracy = 1.0 - misses.size.toDouble() / SampleSms.all.size
        misses.forEach { println("  miss: $it") }
        println("  SMS parser accuracy: ${"%.1f".format(accuracy * 100)}% on ${SampleSms.all.size} samples")
        assertTrue(accuracy >= 0.95, "accuracy ${accuracy * 100}% is below 95%")
    }

    @Test fun `unknown senders are ignored unless demo mode allows them`() {
        val body = "Rs.180.00 debited from a/c **1234 on 06-10-26 to VPA uber.india@axisbank"
        assertNull(parse("+15555215554", body))
        val demo = SmsParser(allowAnySender = true).parse(SmsMessage("+15555215554", body, SampleSms.receivedAt))
        assertNotNull(demo)
        assertEquals("Uber", demo.merchant)
    }

    @Test fun `sender ids with operator prefix and suffix are recognised`() {
        assertEquals("HDFC Bank", parser.bankFor("VM-HDFCBK"))
        assertEquals("SBI", parser.bankFor("AD-SBIUPI-S"))
        assertEquals("ICICI Bank", parser.bankFor("JD-ICICIT-T"))
        assertNull(parser.bankFor("VM-AMAZON"))
    }

    @Test fun `balance is read but never mistaken for the amount`() {
        val p = parse("VM-PNBSMS", "Avl Bal INR 5,120.40. Your A/c XX1234 debited INR 299.00 on 23-09-26 towards UPI/JIO PREPAID")
        assertNotNull(p)
        assertEquals(rupees(299), p.amount)
        assertEquals(512_040, p.balanceAfter)
    }

    @Test fun `reference account and channel are extracted`() {
        val p = parse("VM-SBIUPI", "Rs.180.00 debited from a/c **1234 on 06-10-26 to VPA uber.india@axisbank (UPI Ref No 628012345679).")
        assertNotNull(p)
        assertEquals("628012345679", p.reference)
        assertEquals("1234", p.accountLast4)
        assertEquals(Channel.UPI, p.channel)
        assertEquals("SBI", p.bank)
    }

    @Test fun `message without a date uses the received date`() {
        val p = parse("VM-HDFCBK", "Sent Rs.25.00 from HDFC Bank A/C *1234 to chaiwala@ybl")
        assertNotNull(p)
        assertEquals(SampleSms.receivedAt.toLocalDate(), p.date)
    }

    @Test fun `bank and UPI app SMS for one payment merge into one`() {
        val t = SampleSms.receivedAt
        val bank = parser.parse(SmsMessage("VM-HDFCBK", "Sent Rs.450.00 From HDFC Bank A/C *1234 To SWIGGY On 07/10/26 Ref 628012345678", t))!!
        val app = parser.parse(SmsMessage("VM-IPAYTM", "Paid Rs.450 to swiggy@icici from Paytm Payments Bank a/c 91XXXX1234. UPI Ref: 628012345678.", t.plusMinutes(1)))!!
        val other = parser.parse(SmsMessage("VM-HDFCBK", "Sent Rs.450.00 From HDFC Bank A/C *1234 To ZOMATO On 07/10/26 Ref 628099999999", t.plusMinutes(2)))!!
        assertTrue(Deduper.isDuplicate(bank, app))
        assertFalse(Deduper.isDuplicate(bank, other))
        assertEquals(2, Deduper.merge(listOf(bank, app, other)).size)
    }

    @Test fun `same amount without references merges only inside ten minutes`() {
        val t = SampleSms.receivedAt
        val a = parser.parse(SmsMessage("VM-HDFCBK", "Sent Rs.99.00 From HDFC Bank A/C *1234 To CANTEEN", t))!!
        val b = a.copy(receivedAt = t.plusMinutes(8))
        val c = a.copy(receivedAt = t.plusMinutes(25))
        assertTrue(Deduper.isDuplicate(a, b))
        assertFalse(Deduper.isDuplicate(a, c))
    }

    @Test fun `dictionary learns from corrections`() {
        val dict = MerchantDictionary()
        val learning = SmsParser(dict)
        val body = "Sent Rs.120.00 From HDFC Bank A/C *1234 To SHREE GANESH STORES On 07/10/26"
        assertEquals("Other", learning.parse(SmsMessage("VM-HDFCBK", body, LocalDateTime.now()))!!.category)
        dict.learn("SHREE GANESH STORES", "Ganesh General Store", "Groceries")
        val after = learning.parse(SmsMessage("VM-HDFCBK", body, LocalDateTime.now()))!!
        assertEquals("Ganesh General Store", after.merchant)
        assertEquals("Groceries", after.category)
    }

    @Test fun `vpa handles become readable names`() {
        assertEquals("Priya K", MerchantDictionary.prettify("priya.k@okicici"))
        assertEquals("Client Studio", MerchantDictionary.prettify("client.studio@okaxis"))
        assertEquals("Rajesh Sharma", MerchantDictionary.prettify("MR RAJESH SHARMA"))
    }

    @Test fun `credits and debits are told apart when both words appear`() {
        assertEquals(Direction.DEBIT, parser.direction("acct xx234 debited for rs 1,299.00; amazon pay credited."))
        assertEquals(Direction.CREDIT, parser.direction("rs 1500.00 credited to a/c xxxx1234 by a/c linked to vpa"))
    }
}
