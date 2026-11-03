package com.ruvo.sms

import com.ruvo.engine.Direction
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Realistic sample bank SMS for demo mode and the parser accuracy test. Formats follow common Indian bank
 * templates; every account number, reference and name is made up.
 */
object SampleSms {

    data class Sample(
        val sender: String,
        val body: String,
        /** Null when the message must be ignored (OTP, offer, reminder, personal). */
        val expect: Expect?,
    )

    data class Expect(
        val rupees: Double,
        val direction: Direction,
        val merchant: String,
        val date: LocalDate,
        val category: String? = null,
    )

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val DEBIT = Direction.DEBIT
    private val CREDIT = Direction.CREDIT

    /** The time each sample is treated as received, for messages that carry no date of their own. */
    val receivedAt: LocalDateTime = LocalDateTime.of(2026, 10, 7, 20, 30)

    val all: List<Sample> = listOf(
        // --- SBI ---
        Sample("AD-SBIUPI-S", "Dear UPI user A/C X1234 debited by 450.0 on date 07Oct26 trf to SWIGGY Refno 628012345678. If not u? call 1800111109. -SBI",
            Expect(450.0, DEBIT, "Swiggy", d(2026, 10, 7), "Food")),
        Sample("VM-SBIUPI", "Rs.180.00 debited from a/c **1234 on 06-10-26 to VPA uber.india@axisbank (UPI Ref No 628012345679). Not you? Call 18001111109 -SBI",
            Expect(180.0, DEBIT, "Uber", d(2026, 10, 6), "Transport")),
        Sample("BZ-SBIINB", "Dear Customer, INR 2,000.00 withdrawn at SBI ATM S1BW000123401 from A/cX1234 on 05Oct26 Transaction Number 6788. Available Balance INR 8,420.50. If not withdrawn by you, call 1800 1234.",
            Expect(2000.0, DEBIT, "ATM cash", d(2026, 10, 5), "Cash")),
        Sample("JM-SBIINB", "Dear Customer, Your A/C XXXXX1234 has been credited with Rs 30,000.00 on 01-10-2026 by NEFT from RAJESH SHARMA. Avl Bal Rs 38,450.00 -SBI",
            Expect(30000.0, CREDIT, "Rajesh Sharma", d(2026, 10, 1), "Income")),
        // --- HDFC ---
        Sample("VM-HDFCBK", "Sent Rs.60.00\nFrom HDFC Bank A/C *1234\nTo MUMBAI METRO ONE PVT LTD\nOn 06/10/26\nRef 628012345680\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808",
            Expect(60.0, DEBIT, "Mumbai Metro", d(2026, 10, 6), "Transport")),
        Sample("AX-HDFCBK-S", "Spent Rs.649.00 On HDFC Bank Card 5678 At BOOKMYSHOW On 2026-10-05:19:22:11 Bal Rs.12,040.00 Not You? Call 18002586161/SMS BLOCK DC 5678 to 7308080808",
            Expect(649.0, DEBIT, "BookMyShow", d(2026, 10, 5), "Entertainment")),
        Sample("JD-HDFCBK", "Received Rs.1500.00 in your HDFC Bank A/c xx1234 from priya.k@okicici on 04-10-26. UPI Ref No 628012345681.",
            Expect(1500.0, CREDIT, "Priya K", d(2026, 10, 4), "Income")),
        Sample("VK-HDFCBK", "Update! INR 499.00 debited from HDFC Bank XX1234 on 02-OCT-26. Info: UPI/NETFLIX. Avl bal:INR 11,391.00",
            Expect(499.0, DEBIT, "Netflix", d(2026, 10, 2), "Subscriptions")),
        // --- ICICI ---
        Sample("JD-ICICIT-T", "ICICI Bank Acct XX234 debited for Rs 1,299.00 on 03-Oct-26; AMAZON PAY credited. UPI:628012345682. Call 18002662 for dispute. SMS BLOCK 234 to 9215676766.",
            Expect(1299.0, DEBIT, "Amazon", d(2026, 10, 3), "Shopping")),
        Sample("VM-ICICIT", "Dear Customer, Acct XX234 is credited with Rs 2,500.00 on 30-Sep-26 from KUNAL MEHTA. UPI:628012345683-ICICI Bank.",
            Expect(2500.0, CREDIT, "Kunal Mehta", d(2026, 9, 30), "Income")),
        Sample("AD-ICICIB", "INR 3,200.00 spent using ICICI Bank Card XX9012 on 28-Sep-26 on DECATHLON SPORTS. Avl Limit: INR 46,800.00. If not you, call 1800 2662/SMS BLOCK 9012 to 9215676766",
            Expect(3200.0, DEBIT, "Decathlon", d(2026, 9, 28), "Shopping")),
        // --- Axis ---
        Sample("AX-AXISBK", "INR 340.00 debited\nA/c no. XX1234\n29-09-26, 20:14:33\nUPI/P2M/628012345684/ZOMATO LTD\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            Expect(340.0, DEBIT, "Zomato", d(2026, 9, 29), "Food")),
        Sample("VM-AXISBK-S", "INR 700.00 credited\nA/c no. XX1234\n27-09-26, 11:02:10 IST\nUPI/P2A/628012345685/ISHA NAIR\n- Axis Bank",
            Expect(700.0, CREDIT, "Isha Nair", d(2026, 9, 27), "Income")),
        Sample("BP-AXISBK", "Spent INR 1,250.00 Axis Bank Card no. XX4321 26-09-26 18:40:11 IST FLIPKART Avl Limit: INR 58,750.00 Not you? SMS BLOCK 4321 to 919951860002",
            Expect(1250.0, DEBIT, "Flipkart", d(2026, 9, 26), "Shopping")),
        // --- Kotak ---
        Sample("VM-KOTAKB", "Sent Rs.90.00 from Kotak Bank AC X1234 to rapido.bike@ybl on 25-09-26.UPI Ref 628012345686. Not you, https://kotak.com/KBANKT/Fraud",
            Expect(90.0, DEBIT, "Rapido", d(2026, 9, 25), "Transport")),
        Sample("AD-KOTAKB-S", "Received Rs.250.00 in your Kotak Bank a/c X1234 from aman.v@okhdfcbank on 24-09-26.UPI Ref:628012345687.",
            Expect(250.0, CREDIT, "Aman V", d(2026, 9, 24), "Income")),
        // --- PNB, BoB, Canara, Union, Indian Bank ---
        Sample("VM-PNBSMS", "Your A/c XX1234 debited INR 299.00 on 23-09-26 towards UPI/JIO PREPAID. Avl Bal INR 5,120.40 -PNB",
            Expect(299.0, DEBIT, "Jio", d(2026, 9, 23), "Bills")),
        Sample("AD-BOBTXN", "Rs.120 transferred from A/c ...1234 to:CHAI POINT. Total Bal:Rs.4,820.00CR. Avlbl Amt:Rs.4,820.00(22-09-2026 09:15:44) - Bank of Baroda",
            Expect(120.0, DEBIT, "Chai Point", d(2026, 9, 22), "Food")),
        Sample("JD-CANBNK", "Rs 1500.00 credited to a/c XXXX1234 on 21-09-26 by a/c linked to VPA client.studio@okaxis (UPI Ref No 628012345688). -Canara Bank",
            Expect(1500.0, CREDIT, "Client Studio", d(2026, 9, 21), "Income")),
        Sample("VM-UNIONB", "A/c *1234 Debited for Rs:250.00 on 20-09-2026 13:21:09 by Mob Bk ref no 628012345689 Avl Bal Rs:6,750.00.If not you, Call 1800222243 -Union Bank of India",
            Expect(250.0, DEBIT, "Payment", d(2026, 9, 20), "Other")),
        Sample("AD-INDBNK", "Your a/c no. XXXXXXXX1234 is debited for Rs.75.00 on 19-09-26 and credited to a/c no. XXXXXXXX9876 (UPI Ref no 628012345690). -Indian Bank",
            Expect(75.0, DEBIT, "Payment", d(2026, 9, 19), "Other")),
        // --- IDFC FIRST, Yes, Federal, IndusInd, AU ---
        Sample("VM-IDFCFB", "Your A/C XXXXXXX1234 has been debited by INR 640.00 on 18-SEP-2026 for UPI txn to ZEPTO. UPI Ref 628012345691. Avl Bal INR 9,210.00. - IDFC FIRST Bank",
            Expect(640.0, DEBIT, "Zepto", d(2026, 9, 18), "Groceries")),
        Sample("AD-YESBNK", "INR 899.00 debited from A/c XX1234 on 17-Sep-26 to BIGBASKET. UPI Ref 628012345692. Avl bal INR 8,300.00. If not you, call 18001200",
            Expect(899.0, DEBIT, "BigBasket", d(2026, 9, 17), "Groceries")),
        Sample("VM-FEDBNK", "Rs 149.00 debited from your A/c XX1234 to spotify@hdfcbank on 16-09-2026. UPI Ref:628012345693. Bal:Rs 7,401.00 -Federal Bank",
            Expect(149.0, DEBIT, "Spotify", d(2026, 9, 16), "Subscriptions")),
        Sample("JD-INDUSB", "Your IndusInd Bank Credit Card ending 4455 has been used for INR 2,100.00 at MYNTRA DESIGNS on 15-09-2026. Avl limit INR 37,900.00",
            Expect(2100.0, DEBIT, "Myntra", d(2026, 9, 15), "Shopping")),
        Sample("AX-AUBANK", "Rs.5,000.00 credited to your A/c XX1234 on 14-09-26 via IMPS from STIPEND CREATIVELABS. Ref 628012345694. Bal Rs 13,200.00 -AU Bank",
            Expect(5000.0, CREDIT, "Stipend Creativelabs", d(2026, 9, 14), "Income")),
        // --- Payments banks ---
        Sample("VM-IPAYTM", "Paid Rs.40 to CAMPUS CANTEEN from Paytm Payments Bank a/c 91XXXX1234 on 13-09-2026. UPI Ref: 628012345695.",
            Expect(40.0, DEBIT, "Canteen", d(2026, 9, 13), "Food")),
        Sample("AD-AIRBNK", "Rs 199.00 paid to AIRTEL PREPAID from Airtel Payments Bank a/c on 12-09-2026. Txn ID 628012345696. Bal Rs 2,301.00",
            Expect(199.0, DEBIT, "Airtel", d(2026, 9, 12), "Bills")),
        // --- Refund ---
        Sample("VM-HDFCBK", "Refund of Rs.1,299.00 from AMAZON has been credited to your HDFC Bank A/c XX1234 on 11-09-26. Avl bal Rs 10,000.00",
            Expect(1299.0, CREDIT, "Amazon", d(2026, 9, 11), "Shopping")),
        // --- Lakh amounts and paise ---
        Sample("JM-SBIINB", "Your A/C XXXXX1234 has been credited with Rs 1,25,000.00 on 10-09-2026 by NEFT from MERIT SCHOLARSHIP TRUST. Avl Bal Rs 1,31,250.75 -SBI",
            Expect(125000.0, CREDIT, "Merit Scholarship Trust", d(2026, 9, 10), "Income")),
        Sample("VM-AXISBK", "INR 63.50 debited\nA/c no. XX1234\n09-09-26, 08:01:13\nUPI/P2M/628012345697/SIDDHI XEROX\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            Expect(63.5, DEBIT, "Xerox and stationery", d(2026, 9, 9), "Education")),

        // --- Must be ignored ---
        Sample("VM-HDFCBK", "123456 is your OTP for txn of Rs.500.00 at AMAZON on HDFC Bank Card XX5678. Valid for 5 mins. Do not share OTP.", null),
        Sample("AD-ICICIB", "Congratulations! You are pre-approved for a Personal Loan of Rs 2,00,000. Click to apply: icici.co/xyz", null),
        Sample("VM-SBIUPI", "Rs 500 will be debited from your A/c XX1234 on 10-10-26 towards e-mandate for NETFLIX.", null),
        Sample("AX-AXISBK", "Your Axis Bank Credit Card XX4321 statement is generated. Minimum amount due Rs 1,200, due on 25-10-26.", null),
        Sample("JD-KOTAKB", "kunal.m@okaxis has requested money from you for Rs 300 via UPI. Approve in your UPI app.", null),
        Sample("+919876543210", "Bro send Rs 200 for the pizza", null),
        Sample("VM-AMAZON", "Your order of Rs 1,299 has been shipped. Track at amzn.in/xyz", null),
        Sample("AD-HDFCBK", "Txn of Rs 2,000.00 on HDFC Bank Card XX5678 at CROMA declined due to insufficient balance.", null),
    )
}
