package com.ruvo.engine.guide

/**
 * Money words in plain English, each with an everyday Indian example. Book pages underline any word found here;
 * tapping it shows the meaning. General explanations only: no products recommended, no rates promised.
 */
object Glossary {

    data class Term(val word: String, val meaning: String, val example: String, val also: List<String> = emptyList()) {
        /** Every spelling that should match in a sentence, longest first so "credit score" wins over "credit". */
        val forms: List<String> get() = (listOf(word) + also).sortedByDescending { it.length }
    }

    val all: List<Term> = listOf(
        Term("Fuel", "In Ruvo, the money that is free to spend after bills and goals are set aside.", "₹9,000 in the bank, ₹5,000 rent due and ₹1,000 for a trip leaves ₹3,000 of Fuel."),
        Term("Runway", "In Ruvo, how many days your Fuel lasts at the way you usually spend.", "₹3,000 of Fuel at ₹250 a day is a 12-day runway."),
        Term("Cash flow", "The timing of money coming in and going out.", "A stipend on the 5th and rent on the 1st is a cash-flow gap of four days."),
        Term("Budget", "A plan for what your money will do before you spend it.", "Rent ₹6,000, mess ₹3,000, the rest split between fun and a goal."),
        Term("Emergency fund", "Money kept aside only for surprises, like a phone repair or a trip home.", "Students often start with ₹2,000 to ₹5,000.", listOf("cushion")),
        Term("Interest", "The price of money: what a lender charges you, or what a bank pays you for keeping your money.", "₹10,000 in a savings account at 3% a year earns about ₹300 in a year."),
        Term("Compounding", "Earning interest on interest you already earned, so growth speeds up over time.", "₹1,000 growing 10% a year becomes ₹1,100, then ₹1,210, not ₹1,200.", listOf("compound interest")),
        Term("Inflation", "Prices slowly rising over time, so the same rupee buys a little less each year.", "A ₹20 chai becoming ₹25 a few years later is inflation at work."),
        Term("Savings account", "A bank account for keeping money safely, usually paying a small interest.", "Most students' stipend or pocket money lands here."),
        Term("Fixed deposit", "Money locked with a bank for a set time at an interest rate agreed at the start.", "₹10,000 kept for one year at an agreed rate, returned with interest at the end.", listOf("FD")),
        Term("Recurring deposit", "Saving a fixed amount every month with a bank for a set time, earning agreed interest.", "₹500 every month for 12 months.", listOf("RD")),
        Term("Mutual fund", "A pool of money from many people, managed by professionals and spread across many investments.", "Its value goes up and down; past returns never promise future ones."),
        Term("SIP", "Systematic Investment Plan: putting a fixed amount into an investment every month, automatically.", "₹500 on the 10th of every month."),
        Term("Risk", "How much an investment's value can move up and down, including the chance of losing money.", "A savings account barely moves; shares can move a lot in a week."),
        Term("Diversification", "Spreading money across different things so one bad result does not hurt as much.", "Not keeping every rupee in one place."),
        Term("Credit card", "A card that lets you borrow up to a limit and pay back later. Interest is charged if you do not pay in full by the due date.", "Spend ₹5,000, pay ₹5,000 by the due date, and there is no interest."),
        Term("Credit limit", "The most you can borrow on a credit card.", "A first card might have a ₹20,000 limit."),
        Term("Minimum due", "The smallest amount you can pay on a card bill to avoid a late fee. The rest keeps collecting interest.", "On a ₹12,000 bill the minimum might be about ₹600."),
        Term("EMI", "Equated Monthly Instalment: a loan or big purchase paid back in equal monthly amounts, with interest.", "A ₹12,000 phone over 6 months is about ₹2,090 a month at 16% a year."),
        Term("Buy now, pay later", "A short-term loan at checkout. It feels free, but late fees and interest can follow if you miss a payment.", "Splitting ₹3,000 shoes into three payments.", listOf("BNPL", "pay later")),
        Term("Credit score", "A number from 300 to 900 that shows lenders how reliably you have repaid. Paying on time raises it.", "A score above 750 usually makes loans easier and cheaper.", listOf("CIBIL score", "CIBIL")),
        Term("Loan", "Money borrowed that you repay over time, usually with interest.", "An education loan for college fees."),
        Term("UPI", "Unified Payments Interface: instant bank-to-bank payments from your phone.", "Scanning a QR code at a chai stall."),
        Term("UPI PIN", "The secret number that approves a payment going out. You never need it to receive money.", "If anyone asks for it to \"send you a refund\", it is a scam."),
        Term("OTP", "One-time password: a short code sent to your phone to confirm something. Never share it with anyone.", "A bank will never call you to ask for your OTP."),
        Term("Collect request", "A UPI request asking you to pay. Approving it with your PIN sends money out.", "Scammers send these and call them refunds."),
        Term("Phishing", "Fake messages, links or calls pretending to be a bank or company, to steal details or money.", "An SMS saying your account is blocked, with a link to \"verify\"."),
        Term("PAN", "Permanent Account Number: a 10-character tax ID from the Income Tax Department.", "Needed for a job, a bank account over certain limits, and filing tax."),
        Term("Payslip", "A monthly note from your employer showing what you earned and what was taken out.", "Gross pay minus PF and tax gives your in-hand pay.", listOf("salary slip")),
        Term("CTC", "Cost to Company: everything an employer spends on you in a year, not what lands in your account.", "A ₹4 lakh CTC can mean noticeably less in hand each month."),
        Term("In-hand salary", "What actually reaches your bank account after deductions.", "Also called take-home pay.", listOf("take-home pay", "in-hand pay")),
        Term("PF", "Provident Fund: retirement savings where you and your employer each put in a share of your basic pay.", "Usually 12% of basic pay from each side.", listOf("EPF", "Provident Fund")),
        Term("TDS", "Tax Deducted at Source: tax taken out before money is paid to you.", "Some stipends or freelance payments arrive with a little TDS cut."),
        Term("Income tax return", "A yearly form telling the government what you earned and how much tax applied.", "Filed on the official income tax portal; rules change, so always check the current year's.", listOf("ITR")),
        Term("Gross pay", "What you earn before any deductions.", "₹30,000 gross might be ₹27,000 in hand."),
        Term("Net worth", "What you own minus what you owe.", "₹8,000 in savings and a ₹2,000 loan to a friend you owe is ₹6,000."),
    )

    /** Glossary terms that appear in [text], each once, in reading order. */
    fun inText(text: String): List<Term> {
        val lower = text.lowercase()
        return all.mapNotNull { t ->
            val at = t.forms.map { f -> indexOfWord(lower, f.lowercase()) }.filter { it >= 0 }.minOrNull()
            at?.let { it to t }
        }.sortedBy { it.first }.map { it.second }
    }

    /** Where [word] appears in [text] as a whole word, or -1. */
    fun indexOfWord(text: String, word: String): Int {
        var from = 0
        while (true) {
            val i = text.indexOf(word, from)
            if (i < 0) return -1
            val before = text.getOrNull(i - 1)
            val after = text.getOrNull(i + word.length)
            if ((before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())) return i
            from = i + 1
        }
    }

    fun byWord(word: String): Term? = all.firstOrNull { t -> t.forms.any { it.equals(word, ignoreCase = true) } }
}
