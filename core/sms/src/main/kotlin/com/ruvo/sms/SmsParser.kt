package com.ruvo.sms

import com.ruvo.engine.Direction
import com.ruvo.engine.Paise
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class SmsMessage(val sender: String, val body: String, val receivedAt: LocalDateTime)

enum class Channel { UPI, CARD, NEFT, IMPS, ATM, OTHER }

data class ParsedTxn(
    val amount: Paise,
    val direction: Direction,
    /** Counterparty as the bank wrote it. Kept for learning from corrections, never uploaded. */
    val rawCounterparty: String,
    val merchant: String,
    val category: String,
    val date: LocalDate,
    val reference: String?,
    val accountLast4: String?,
    val bank: String?,
    val channel: Channel,
    val balanceAfter: Paise?,
    val receivedAt: LocalDateTime,
    /** Money coming back for an earlier purchase. Not income and not spending. */
    val isRefund: Boolean = false,
)

/**
 * Reads Indian bank and UPI transaction SMS on the phone. Only messages from known bank sender IDs are read;
 * OTPs, offers, reminders and personal messages are ignored and never stored.
 */
class SmsParser(
    private val dictionary: MerchantDictionary = MerchantDictionary(),
    /** Demo and emulator testing only: accept any sender, since test SMS arrive from phone numbers. */
    private val allowAnySender: Boolean = false,
) {

    fun parse(msg: SmsMessage): ParsedTxn? {
        val bank = bankFor(msg.sender)
        if (bank == null && !allowAnySender) return null
        val body = msg.body.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        val lower = body.lowercase()
        if (IGNORE.any { it.containsMatchIn(lower) }) return null

        val direction = direction(lower) ?: return null
        val amount = amount(body) ?: return null
        if (amount <= 0) return null

        val raw = counterparty(body, direction)
        val entry = when {
            raw != null -> dictionary.resolve(raw)
            direction == Direction.CREDIT -> MerchantDictionary.Entry("Money received", "Income")
            else -> MerchantDictionary.Entry("Payment", "Other")
        }
        val category = if (direction == Direction.CREDIT && entry.category == "Other") "Income" else entry.category
        val channel = channel(lower)
        return ParsedTxn(
            amount = amount,
            direction = direction,
            rawCounterparty = raw ?: "",
            merchant = if (channel == Channel.ATM) "ATM cash" else entry.name,
            category = if (channel == Channel.ATM) "Cash" else category,
            date = date(body) ?: msg.receivedAt.toLocalDate(),
            reference = reference(body),
            accountLast4 = account(body),
            bank = bank ?: bankFromBody(lower),
            channel = channel,
            balanceAfter = balance(body),
            receivedAt = msg.receivedAt,
            isRefund = direction == Direction.CREDIT && Regex("\\brefund").containsMatchIn(lower),
        )
    }

    // --- Sender ---

    /** Sender IDs look like "VM-HDFCBK", "AD-SBIUPI-S" or "JD-ICICIT-T". */
    fun bankFor(sender: String): String? {
        val parts = sender.uppercase().split('-')
        val root = when {
            parts.size >= 2 && parts[0].length == 2 -> parts[1]
            else -> parts[0]
        }
        return SENDERS.entries.firstOrNull { (prefix, _) -> root.startsWith(prefix) }?.value
    }

    private fun bankFromBody(lower: String): String? =
        BODY_BANKS.entries.firstOrNull { lower.contains(it.key) }?.value

    // --- Fields ---

    internal fun direction(lower: String): Direction? {
        val debit = DEBIT_WORDS.mapNotNull { Regex(it).find(lower)?.range?.first }.minOrNull()
        val credit = CREDIT_WORDS.mapNotNull { Regex(it).find(lower)?.range?.first }.minOrNull()
        return when {
            debit == null && credit == null -> null
            credit == null -> Direction.DEBIT
            debit == null -> Direction.CREDIT
            debit <= credit -> Direction.DEBIT
            else -> Direction.CREDIT
        }
    }

    internal fun amount(body: String): Paise? {
        BARE_AMOUNT.find(body)?.let { if (!AMOUNT.containsMatchIn(body.substring(0, it.range.first))) return toPaise(it.groupValues[1]) }
        for (m in AMOUNT.findAll(body)) {
            val before = body.substring(maxOf(0, m.range.first - 18), m.range.first).lowercase()
            if (before.contains("bal") || before.contains("limit") || before.contains("avl")) continue
            return toPaise(m.groupValues[1])
        }
        return null
    }

    internal fun balance(body: String): Paise? = BALANCE.find(body)?.let { toPaise(it.groupValues[1]) }

    internal fun counterparty(body: String, direction: Direction): String? {
        val patterns = if (direction == Direction.DEBIT) DEBIT_PARTY else CREDIT_PARTY
        for (p in patterns) {
            val m = p.find(body) ?: continue
            val v = m.groupValues[1].trim().trimEnd('.', ',', ';', '-', ':').trim()
            val lowerV = v.lowercase()
            if (v.length >= 2 && !JUNK_PARTY.containsMatchIn(lowerV) && !LONG_DIGITS.containsMatchIn(v) && !lowerV.contains("ref no")) return v
        }
        return null
    }

    internal fun reference(body: String): String? =
        REFERENCE.firstNotNullOfOrNull { it.find(body)?.groupValues?.get(1) }

    internal fun account(body: String): String? = ACCOUNT.find(body)?.groupValues?.get(1)?.takeLast(4)

    internal fun channel(lower: String): Channel = when {
        lower.contains("upi") || Regex("[a-z0-9.\\-_]+@[a-z]{2,}").containsMatchIn(lower) -> Channel.UPI
        lower.contains("atm") || lower.contains("cash withdrawal") || lower.contains("withdrawn") -> Channel.ATM
        lower.contains("card") -> Channel.CARD
        lower.contains("neft") -> Channel.NEFT
        lower.contains("imps") -> Channel.IMPS
        else -> Channel.OTHER
    }

    internal fun date(body: String): LocalDate? {
        for ((regex, formats) in DATES) {
            val m = regex.find(body) ?: continue
            val text = m.groupValues[1].replace(Regex("\\s+"), " ")
            for (f in formats) {
                try {
                    return LocalDate.parse(normaliseMonth(text), DateTimeFormatter.ofPattern(f, Locale.ENGLISH))
                } catch (_: DateTimeParseException) {
                }
            }
        }
        return null
    }

    private fun normaliseMonth(s: String): String =
        Regex("[A-Za-z]{3,9}").replace(s) { mr ->
            val w = mr.value.take(3).lowercase()
            w.replaceFirstChar { it.uppercase() }
        }

    private fun toPaise(text: String): Paise {
        val clean = text.replace(",", "")
        val parts = clean.split('.')
        val whole = parts[0].toLongOrNull() ?: 0
        val frac = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLongOrNull() ?: 0
        return whole * 100 + frac
    }

    companion object {
        /** Root of the sender ID after the two-letter operator prefix, mapped to a bank name. */
        val SENDERS: Map<String, String> = linkedMapOf(
            "HDFCBK" to "HDFC Bank", "HDFCBN" to "HDFC Bank",
            "SBIUPI" to "SBI", "SBIINB" to "SBI", "SBICRD" to "SBI Card", "ATMSBI" to "SBI", "CBSSBI" to "SBI", "SBIPSG" to "SBI",
            "ICICIT" to "ICICI Bank", "ICICIB" to "ICICI Bank",
            "AXISBK" to "Axis Bank", "AXISMR" to "Axis Bank",
            "KOTAKB" to "Kotak Bank", "KOTAK" to "Kotak Bank",
            "PNBSMS" to "PNB", "PUNBNK" to "PNB",
            "BOBTXN" to "Bank of Baroda", "BOBSMS" to "Bank of Baroda",
            "CANBNK" to "Canara Bank",
            "IDFCFB" to "IDFC FIRST Bank",
            "YESBNK" to "Yes Bank",
            "INDBNK" to "Indian Bank",
            "UNIONB" to "Union Bank", "UBININ" to "Union Bank",
            "IPAYTM" to "Paytm Payments Bank", "PAYTMB" to "Paytm Payments Bank",
            "AIRBNK" to "Airtel Payments Bank",
            "FEDBNK" to "Federal Bank",
            "IPBMSG" to "India Post Payments Bank",
            "INDUSB" to "IndusInd Bank",
            "AUBANK" to "AU Small Finance Bank",
        )

        private val BODY_BANKS = linkedMapOf(
            "hdfc" to "HDFC Bank", "icici" to "ICICI Bank", "axis" to "Axis Bank", "kotak" to "Kotak Bank",
            "sbi" to "SBI", "canara" to "Canara Bank", "pnb" to "PNB", "baroda" to "Bank of Baroda",
        )

        /** Messages that mention money but are not completed transactions. */
        private val IGNORE = listOf(
            Regex("\\botp\\b"), Regex("one.?time.?password"), Regex("verification code"), Regex("\\bis your\\b.*\\bcode\\b"),
            Regex("pre-?approved"), Regex("\\bloan\\b.*\\b(offer|eligible|avail)"), Regex("\\b(win|won|congratulations)\\b"),
            Regex("will be (debited|deducted|charged)"), Regex("\\bdue (on|by|date)\\b"), Regex("e-?mandate"),
            Regex("collect request"), Regex("has requested"), Regex("requested money"), Regex("payment request"),
            Regex("\\b(declined|failed|unsuccessful|reversed? due)\\b"), Regex("minimum amount due"), Regex("statement (is|for)"),
            Regex("\\bget\\b.*\\bcashback\\b"), Regex("\\bupto\\b.*\\boff\\b"), Regex("\\bclick\\b.*\\b(apply|claim)\\b"),
        )

        private val DEBIT_WORDS = listOf(
            "\\bdebited\\b", "\\bspent\\b", "\\bsent\\b", "\\bpaid\\b", "\\bwithdrawn\\b", "\\bpurchase\\b", "\\bdeducted\\b",
            "\\btransferred\\b", "\\bused for\\b", "\\bused at\\b", "\\btrf to\\b", "\\bdebit(ed)? for\\b", "\\bdr\\b", "\\bwithdrawal\\b", "\\bdebit\\b",
        )
        private val CREDIT_WORDS = listOf(
            "\\bcredited to\\b", "\\bcredited in\\b", "\\bcredited with\\b", "\\breceived\\b", "\\bdeposited\\b",
            "\\bcr\\b", "\\brefund(ed)?\\b", "\\bcredit(ed)? of\\b", "^(rs\\.?|inr) ?[0-9,.]+ credited", "\\bhas been credited\\b",
            "is credited",
        )

        private val AMOUNT = Regex("(?i)(?:rs[.:]?|inr|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)")
        /** Some SBI UPI alerts carry no currency word: "debited by 450.0". */
        private val BARE_AMOUNT = Regex("(?i)\\b(?:debited|credited)\\s+(?:by|with|for)\\s+([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\b")
        private val BALANCE = Regex("(?i)(?:avl\\.?|avbl\\.?|available|clear|closing)\\s*(?:bal(?:ance)?|bal\\.?)[^0-9]{0,12}?(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")

        private const val NAME = "([A-Za-z0-9][A-Za-z0-9 &'._\\-]{1,40}?)"
        private const val VPA = "([A-Za-z0-9._\\-]{2,}@[A-Za-z]{2,})"
        private const val STOP = "(?=\\s+on\\b|\\s+ref\\b|\\s+upi\\b|\\s+via\\b|\\s+using\\b|\\s+avl\\b|\\s+info\\b|\\s+txn\\b|\\s+not you|\\.\\s|\\.?$|,|;|\\(|\\s+-\\s*[A-Z]|\\s+dated|\\s+from\\b|\\s+has\\b|\\s+is\\b|\\s+was\\b|\\s+for\\b|\\s+refno\\b)"

        private val DEBIT_PARTY = listOf(
            Regex("(?i)\\bto\\s+(?:vpa\\s+)?$VPA"),
            Regex("(?i)UPI/P2[MA]/\\d+/([A-Za-z0-9 &'.]+?)(?:/|\\s+not you|\\s+-|$)"),
            Regex("(?i)\\b(?:towards|info:?)\\s*UPI/([A-Za-z0-9 &'.]+?)(?=\\.|$|\\s+avl)"),
            Regex("(?i)\\bat\\s+$NAME$STOP"),
            Regex("(?i)\\d{2}:\\d{2}:\\d{2}\\s+(?:IST\\s+)?([A-Za-z][A-Za-z0-9 &'.]{2,40}?)\\s+avl"),
            Regex("(?i)\\d\\s+on\\s+([A-Za-z][A-Za-z0-9 &'.]{2,40}?)(?=\\.\\s|\\.?$|\\s+avl)"),
            Regex("(?i)\\btrf to\\s+$NAME$STOP"),
            Regex("(?i);\\s*([A-Za-z0-9 &'.]+?)\\s+credited"),
            Regex("(?i)\\b(?:to|towards):?\\s*(?!your\\b|a/c\\b|ac\\b|account\\b)$NAME$STOP"),
            Regex("(?i)\\bInfo:?\\s*(?:UPI/)?([A-Za-z0-9 &'.]{3,30}?)(?:/|\\.|$)"),
        )
        private val CREDIT_PARTY = listOf(
            Regex("(?i)\\b(?:from|by)\\s+(?:vpa\\s+)?$VPA"),
            Regex("(?i)linked to (?:vpa\\s+)?$VPA"),
            Regex("(?i)UPI/P2[MA]/\\d+/([A-Za-z0-9 &'.]+?)(?:/|\\s+not you|\\s+-|$)"),
            Regex("(?i)\\b(?:neft|imps|rtgs|transfer)\\s+from\\s+$NAME$STOP"),
            Regex("(?i)\\bfrom\\s+(?!your\\b|a/c\\b|ac\\b|account\\b)$NAME$STOP"),
            Regex("(?i)\\bby\\s+(?!a/c\\b|ac\\b|neft\\b|imps\\b|upi\\b)$NAME$STOP"),
        )
        private val JUNK_PARTY = Regex("^(a/?c|ac|acct|account|your|xx|x+\\d+|\\d+|bank|card|upi|ref|on|by)\\b")
        private val LONG_DIGITS = Regex("\\d{6,}")

        private val REFERENCE = listOf(
            Regex("(?i)UPI/P2[MA]/(\\d{9,14})"),
            Regex("(?i)(?:upi\\s*ref(?:\\s*no)?|ref(?:erence)?\\s*(?:no|num(?:ber)?)?|rrn|utr|txn\\s*id|upi)[\\s:.#-]*([0-9]{9,14})"),
        )
        private val ACCOUNT = Regex("(?i)(?:a/?c|acct|account|card)(?:\\s*no\\.?)?(?:\\s*ending(?:\\s*with)?)?\\s*[*xX.]*\\s*([0-9]{3,6})")

        private val DATES: List<Pair<Regex, List<String>>> = listOf(
            Regex("(\\d{4}-\\d{2}-\\d{2})") to listOf("yyyy-MM-dd"),
            Regex("(\\d{1,2}-[A-Za-z]{3}-\\d{2,4})") to listOf("d-MMM-yy", "d-MMM-yyyy"),
            Regex("(\\d{1,2} [A-Za-z]{3,9},? \\d{4})") to listOf("d MMM yyyy", "d MMM, yyyy"),
            Regex("(\\d{1,2}[A-Za-z]{3}\\d{2,4})") to listOf("dMMMyy", "dMMMyyyy"),
            Regex("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})\\b") to listOf("d-M-yyyy", "d/M/yyyy"),
            Regex("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2})\\b") to listOf("d-M-yy", "d/M/yy"),
        )
    }
}
