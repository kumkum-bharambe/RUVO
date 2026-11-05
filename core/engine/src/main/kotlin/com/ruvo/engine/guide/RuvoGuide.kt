package com.ruvo.engine.guide

import com.ruvo.engine.BeforeYouBuy
import com.ruvo.engine.Bucket
import com.ruvo.engine.DayStrip
import com.ruvo.engine.Goal
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Paise
import com.ruvo.engine.RunwayInput
import com.ruvo.engine.RunwayResult
import com.ruvo.engine.SpendSource
import com.ruvo.engine.Txn
import com.ruvo.engine.MoneyArrives
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Ruvo's scripted voice: warm, grown-up, never preachy. Every number comes from the engine; this file only
 * chooses words. These answers work offline, so a demo never fails.
 */
object RuvoGuide {

    data class Context(
        val input: RunwayInput,
        val result: RunwayResult,
        val owedToYou: Paise = 0,
        val name: String = "",
    )

    data class Reply(
        val text: String,
        /** Short follow-up questions shown as chips under the answer. */
        val suggestions: List<String> = emptyList(),
        /** What the reply is about, so the UI can offer the right action (open Before you buy, Goals...). */
        val topic: Topic = Topic.GENERAL,
    )

    enum class Topic { RUNWAY, AFFORD, TODAY, RUNS_OUT, NEXT_MONEY, SPENDING, BUCKETS, SAVING, GOALS, OWED, DUE, CHECK_IN, MONEY_IN, INVESTING, CREDIT, SUBSCRIPTIONS, SORT, GREETING, GENERAL }

    /** The questions offered as chips when the chat opens. */
    val starterQuestions = listOf(
        "Why this many days?",
        "How much can I spend today?",
        "Can I afford ₹1,500?",
        "Where did my money go this week?",
    )

    fun answer(question: String, c: Context): Reply {
        val q = question.lowercase().trim()
        val amount = amountIn(q)
        return when {
            q.isEmpty() || q.matches(Regex("^(hi|hello|hey|namaste|yo)\\b.*")) -> greeting(c)
            amount != null && Regex("afford|buy|spend|purchase|get|can i").containsMatchIn(q) -> afford(amount, c)
            Regex("invest|stock|mutual fund|sip|crypto|shares|trading").containsMatchIn(q) -> investing()
            Regex("loan|credit card|emi|borrow|bnpl|pay later").containsMatchIn(q) -> credit()
            Regex("subscription|netflix|spotify|ott").containsMatchIn(q) -> subscriptions(c)
            Regex("owe|owed|pay me back|split|flatmate|friend").containsMatchIn(q) -> owed(c)
            Regex("goal|saving for|trip|laptop|jar|target").containsMatchIn(q) -> goals(c)
            Regex("emergency").containsMatchIn(q) -> emergency(c)
            Regex("today|per day|a day|daily|safe to spend").containsMatchIn(q) -> today(c)
            Regex("run out|runs out|last till|last until|when will .* (end|finish)").containsMatchIn(q) -> runsOut(c)
            Regex("next money|income|pocket money|stipend|salary|scholarship|late|arrive").containsMatchIn(q) &&
                Regex("got|received|came|arrived|just").containsMatchIn(q) -> moneyIn(c)
            Regex("next money|income|pocket money|stipend|salary|scholarship|late").containsMatchIn(q) -> nextMoney(c)
            Regex("where|went|spent|spending|food|transport|shopping|categor").containsMatchIn(q) -> spending(q, c)
            Regex("fuel|fixed|future|bucket").containsMatchIn(q) -> buckets(c)
            Regex("due|rent|bill|mess|recharge|upcoming").containsMatchIn(q) -> due(c)
            Regex("save|saving|cut|reduce|tips|stretch|better").containsMatchIn(q) -> saving(c)
            Regex("sort|weekly").containsMatchIn(q) -> sort()
            Regex("how am i|doing|okay|ok\\b|fine|on track|worried|stress").containsMatchIn(q) -> checkIn(c)
            Regex("why|days|runway|buffer|spare|number|explain").containsMatchIn(q) -> runway(c)
            else -> Reply(
                "I can help with your runway, what you can spend, what a purchase costs in days, your goals and " +
                    "who owes you. Try asking it the way you would ask a friend.",
                starterQuestions, Topic.GENERAL,
            )
        }
    }

    // --- Answers ---

    private fun greeting(c: Context): Reply {
        val r = c.result
        val name = c.name.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
        val line = when {
            r.unlimited -> "Your spending has been near zero lately, so there is no end to your runway right now."
            r.bufferDays == null -> "You have ${IndianFormat.days(r.runwayDays)} of runway at your usual pace."
            r.makesIt -> "You have ${IndianFormat.days(r.runwayDays)} of runway and you make it to your next money with ${IndianFormat.days(r.spareDays)} spare."
            else -> "You have ${IndianFormat.days(r.runwayDays)} of runway, which is ${IndianFormat.days(-(r.bufferDays ?: 0))} short of your next money. That is fixable."
        }
        return Reply("Hello$name. $line What would you like to look at?", starterQuestions, Topic.GREETING)
    }

    fun runway(c: Context): Reply {
        val r = c.result
        val source = when (r.spendSource) {
            SpendSource.HISTORY -> "your spending over the last two weeks"
            SpendSource.PLAN -> "your plan, since there is not much history yet"
            SpendSource.EVEN_SPREAD -> "an even spread to your next money, since there is no plan or history yet"
        }
        val parts = mutableListOf<String>()
        parts += "You have ${IndianFormat.rupees(r.balance)} in total."
        if (r.fixedReserved > 0) parts += "${IndianFormat.rupees(r.fixedReserved)} is already promised to bills before your next money."
        if (r.futureProtected > 0) parts += "${IndianFormat.rupees(r.futureProtected)} is protected for your goals."
        parts += "That leaves ${IndianFormat.rupees(r.fuel.coerceAtLeast(0))} of Fuel."
        if (r.unlimited) {
            parts += "Your recent spending is close to zero, so the runway has no end for now."
        } else {
            parts += "At about ${IndianFormat.rupees(r.dailySpend)} a day, based on $source, that lasts ${IndianFormat.days(r.runwayDays)}."
        }
        r.daysToNextIncome?.let { d ->
            parts += if (r.makesIt) "Your next money is ${IndianFormat.days(d)} away, so you have ${IndianFormat.days(r.spareDays)} spare."
            else "Your next money is ${IndianFormat.days(d)} away, so you are ${IndianFormat.days(-(r.bufferDays ?: 0))} short at this pace."
        }
        return Reply(parts.joinToString(" "), listOf("How much can I spend today?", "What counts as Fixed?"), Topic.RUNWAY)
    }

    fun afford(price: Paise, c: Context): Reply {
        val b = BeforeYouBuy.evaluate(c.result, price)
        val cost = "That costs about ${days1(b.costDays)} of your usual spending."
        val text = when (b.verdict) {
            BeforeYouBuy.Verdict.COMFORTABLE -> {
                val spare = b.bufferAfter?.let { " and still make it with ${IndianFormat.days(it)} spare" } ?: ""
                "$cost You could buy it$spare. Your call, of course."
            }
            BeforeYouBuy.Verdict.TIGHT -> {
                val spare = b.bufferAfter?.let { "with ${IndianFormat.days(it)} spare" } ?: "with a thin cushion"
                "$cost You would still make it, $spare, so the rest of the month gets tight. If it can wait until your next money, it will feel lighter."
            }
            BeforeYouBuy.Verdict.SHORT -> {
                val gap = b.bufferAfter?.let { " and leave you about ${IndianFormat.days(-it)} short of your next money" } ?: ""
                val daily = if (b.safeDailyAfter > 0) " If you buy it, ${IndianFormat.rupees(b.safeDailyAfter)} a day gets you through." else ""
                "$cost It would use more than your cushion$gap.$daily It is still your decision; Ruvo never blocks a purchase."
            }
        }
        return Reply(text, listOf("What if I wait a week?", "How much can I spend today?"), Topic.AFFORD)
    }

    fun today(c: Context): Reply {
        val r = c.result
        val text = when {
            r.fuel <= 0 -> "Your Fuel is used up for now, so today is best kept to essentials. ${shortfallLine(r)}"
            r.daysToNextIncome == null ->
                "Spread over the next month, about ${IndianFormat.rupees(r.safeDaily)} a day keeps you steady. Your usual is ${IndianFormat.rupees(r.dailySpend)}."
            r.makesIt ->
                "Up to ${IndianFormat.rupees(r.safeDaily)} a day gets you to your next money. You usually spend ${IndianFormat.rupees(r.dailySpend)}, so you have some room."
            else ->
                "${IndianFormat.rupees(r.safeDaily)} a day gets you to your next money. Your usual is ${IndianFormat.rupees(r.dailySpend)}, so a slightly lighter week closes the gap."
        }
        return Reply(text, listOf("Where did my money go this week?", "Why this many days?"), Topic.TODAY)
    }

    private fun runsOut(c: Context): Reply {
        val r = c.result
        val date = DayStrip.runsOutOn(r)
        val text = when {
            r.unlimited || date == null -> "At your current pace your money does not run out. Enjoy the calm."
            r.makesIt -> "At your usual pace your Fuel lasts until about ${IndianFormat.date(date, r.today)}, which is after your next money arrives."
            else -> "At your usual pace your Fuel lasts until about ${IndianFormat.date(date, r.today)}. ${shortfallLine(r)}"
        }
        return Reply(text, listOf("How much can I spend today?"), Topic.RUNS_OUT)
    }

    private fun nextMoney(c: Context): Reply {
        val r = c.result
        val d = r.nextIncomeDate
        val text = when {
            r.incomeOverdue -> "Your expected money has not shown up yet. Until it does, Ruvo counts zero days to it, so your runway is the honest number to watch. You can update the date in Settings."
            d == null -> "Your income comes in when it comes, so Ruvo shows how long your money lasts rather than a date to reach. Planning by your leanest month keeps you safe."
            else -> "Your next money is due on ${IndianFormat.date(d, r.today)}, ${IndianFormat.days(r.daysToNextIncome ?: 0)} from today."
        }
        return Reply(text, listOf("Why this many days?"), Topic.NEXT_MONEY)
    }

    private fun moneyIn(c: Context): Reply {
        val s = MoneyArrives.suggest(c.input.income.expectedAmount, c.input.commitments, c.input.goals)
        val text = if (s.received <= 0) {
            "Lovely. Add it with the + button so your runway updates, and Ruvo will suggest how to set it up."
        } else {
            "Nice. A calm way to set it up: put ${IndianFormat.rupees(s.setAsideFixed)} aside for Fixed first, move " +
                "${IndianFormat.rupees(s.suggestFuture)} to your goals, and the remaining ${IndianFormat.rupees(s.leavesFuel)} is Fuel."
        }
        return Reply(text, listOf("How are my goals doing?"), Topic.MONEY_IN)
    }

    private fun spending(q: String, c: Context): Reply {
        val today = c.result.today
        val week = c.input.transactions.filter { it.countsAsSpending && it.bucket == Bucket.FUEL && ChronoUnit.DAYS.between(it.date, today) in 0..6 }
        if (week.isEmpty()) return Reply("Nothing has been spent from Fuel in the last seven days. Quiet weeks count too.", topic = Topic.SPENDING)
        val byCat = week.groupBy { it.category }.mapValues { (_, l) -> l.sumOf(Txn::yourShare) }.entries.sortedByDescending { it.value }
        val total = week.sumOf(Txn::yourShare)
        val asked = listOf("food", "transport", "shopping", "groceries", "entertainment", "education").firstOrNull { q.contains(it) }
        if (asked != null) {
            val match = byCat.firstOrNull { it.key.lowercase() == asked }
            val count = week.count { it.category.lowercase() == asked }
            val text = if (match == null) "No ${asked} spending in the last seven days."
            else "${asked.replaceFirstChar { it.uppercase() }} came to ${IndianFormat.rupees(match.value)} over $count ${if (count == 1) "payment" else "payments"} this week, out of ${IndianFormat.rupees(total)} in total."
            return Reply(text, listOf("How can I stretch my money?"), Topic.SPENDING)
        }
        val top = byCat.first()
        val small = week.count { it.yourShare <= 20_000 }
        val smallLine = if (small >= 4) " $small of those were under ₹200; small spends are easy to miss and they add up." else ""
        val second = byCat.getOrNull(1)?.let { ", then ${it.key} at ${IndianFormat.rupees(it.value)}" } ?: ""
        return Reply(
            "This week you spent ${IndianFormat.rupees(total)} from Fuel. ${top.key} was the biggest at ${IndianFormat.rupees(top.value)}$second.$smallLine",
            listOf("How can I stretch my money?", "How much can I spend today?"), Topic.SPENDING,
        )
    }

    private fun buckets(c: Context): Reply {
        val r = c.result
        return Reply(
            "Fixed is money already promised before your next money, like rent or a recharge: ${IndianFormat.rupees(r.fixedReserved)} right now. " +
                "Future is protected for your goals: ${IndianFormat.rupees(r.futureProtected)}. Fuel is everything else, free to spend: " +
                "${IndianFormat.rupees(r.fuel.coerceAtLeast(0))}. Your runway only counts Fuel, so promises and goals stay safe.",
            listOf("Why this many days?"), Topic.BUCKETS,
        )
    }

    private fun due(c: Context): Reply {
        val list = c.result.reservedCommitments
        if (list.isEmpty()) return Reply("Nothing is due before your next money. Everything promised is already covered.", topic = Topic.DUE)
        val lines = list.take(4).joinToString("; ") { "${it.name} ${IndianFormat.rupees(it.amount)} on ${it.dueDate?.let { d -> IndianFormat.date(d, c.result.today) } ?: "soon"}" }
        return Reply("Before your next money: $lines. Ruvo has already set this aside in Fixed, so your runway does not count it.", topic = Topic.DUE)
    }

    private fun saving(c: Context): Reply {
        val r = c.result
        val tip = if (r.dailySpend > 0) {
            val fifty = 5_000L
            val gained = if (r.dailySpend > fifty) (r.fuel.toDouble() / (r.dailySpend - fifty) - r.fuel.toDouble() / r.dailySpend).toInt() else 0
            if (gained > 0) " Spending ₹50 less a day would add about ${IndianFormat.days(gained)} to your runway." else ""
        } else ""
        return Reply(
            "Three things work for most students. Move money to your goals the day it arrives, so saving is not left for last. " +
                "Do the two-minute weekly sort, because seeing the week is half the change. And pause before buys over a day's spend: " +
                "Before you buy shows the cost in days.$tip",
            listOf("Can I afford ₹1,500?", "How are my goals doing?"), Topic.SAVING,
        )
    }

    private fun goals(c: Context): Reply {
        val goals = c.input.goals
        if (goals.isEmpty()) return Reply("You have no goals yet. Even a small one, like a trip or an emergency cushion, makes saving feel real.", topic = Topic.GOALS)
        val lines = goals.take(3).joinToString(" ") { g -> goalLine(g, c.result.today) }
        return Reply(lines, listOf("How can I stretch my money?"), Topic.GOALS)
    }

    private fun goalLine(g: Goal, today: LocalDate): String {
        val pct = (g.progress * 100).toInt()
        val left = (g.target - g.protectedAmount).coerceAtLeast(0)
        if (g.isComplete) return "${g.name} is fully funded. Well done."
        val eta = if (g.monthlyContribution > 0) {
            val months = Math.ceil(left.toDouble() / g.monthlyContribution).toLong()
            val date = today.plusMonths(months)
            val onTrack = g.targetDate?.let { !date.isAfter(it) }
            val track = when (onTrack) {
                true -> ", on track"
                false -> ", a little behind the date you set"
                null -> ""
            }
            " At ${IndianFormat.rupees(g.monthlyContribution)} a month you reach it around ${IndianFormat.date(date, today)}$track."
        } else ""
        return "${g.name} is $pct% there, ${IndianFormat.rupees(left)} to go.$eta"
    }

    private fun emergency(c: Context): Reply {
        val monthly = c.result.dailySpend * 30
        return Reply(
            "An emergency cushion is money you never plan to spend, for a broken phone or a surprise trip home. A good first target is one month of your usual spending, about ${IndianFormat.rupees(monthly)} for you. Build it slowly; even ₹500 a month counts.",
            topic = Topic.SAVING,
        )
    }

    private fun owed(c: Context): Reply {
        val text = if (c.owedToYou > 0) {
            "Friends owe you ${IndianFormat.rupees(c.owedToYou)} in total. You can send each of them a UPI payment link from Money, under Owed to you. Ruvo never moves money itself."
        } else {
            "Nobody owes you anything right now. To split a payment, open it in Money and tap Shared."
        }
        return Reply(text, topic = Topic.OWED)
    }

    private fun subscriptions(c: Context): Reply {
        val subs = c.input.commitments.filter { it.recurring && Regex("(?i)spotify|netflix|prime|hotstar|youtube|subscription").containsMatchIn(it.name) }
        val total = subs.sumOf { it.amount }
        val text = if (subs.isEmpty()) "I do not see any subscriptions in your Fixed list. If you have some, add them so your runway stays honest."
        else "Your subscriptions come to ${IndianFormat.rupees(total)} a month: ${subs.joinToString(", ") { it.name }}. Many have student plans; worth a quick check."
        return Reply(text, topic = Topic.SUBSCRIPTIONS)
    }

    private fun investing() = Reply(
        "I can explain the basics, but I do not recommend specific funds, stocks or apps. The usual order is: an emergency cushion first, " +
            "then saving for near goals, and investing only money you will not need for years. If you want to learn more, a SEBI-registered adviser or the SEBI investor education site is a good start.",
        topic = Topic.INVESTING,
    )

    private fun credit() = Reply(
        "Pay-later and EMIs make a purchase feel smaller than it is, because the cost arrives after your runway has moved on. " +
            "I will not recommend a loan or a card. If you are thinking about one, Before you buy shows what the full price costs in days today.",
        topic = Topic.CREDIT,
    )

    private fun sort() = Reply(
        "The weekly sort is a two-minute check of the payments Ruvo picked up. Swipe right to confirm, left to ignore, up to mark it shared. It keeps your daily spend, and so your runway, accurate.",
        topic = Topic.SORT,
    )

    private fun checkIn(c: Context): Reply {
        val r = c.result
        val text = when {
            r.unlimited -> "You are in a very calm spot. Your spending has been low, and nothing is pressing."
            r.makesIt && r.spareDays >= 5 -> "You are doing well: ${IndianFormat.days(r.spareDays)} spare before your next money. A good moment to move a little to a goal."
            r.makesIt -> "You are okay. You make it to your next money with ${IndianFormat.days(r.spareDays)} spare, so keep an eye on bigger buys."
            r.bufferDays == null -> "Your runway is ${IndianFormat.days(r.runwayDays)} at your usual pace. With irregular income, the steady move is to spend like your leanest month."
            else -> "It is a tighter month, and that happens to everyone. ${shortfallLine(r)} One lighter week usually does it."
        }
        return Reply(text, listOf("How much can I spend today?", "How can I stretch my money?"), Topic.CHECK_IN)
    }

    private fun shortfallLine(r: RunwayResult): String =
        if (r.daysToNextIncome != null && r.daysToNextIncome > 0 && r.fuel > 0) "${IndianFormat.rupees(r.safeDaily)} a day gets you to your next money."
        else if (r.shortfall > 0) "You are about ${IndianFormat.rupees(r.shortfall)} short of your next money at your usual pace."
        else ""

    private fun days1(d: Double): String = if (d == Math.floor(d)) IndianFormat.days(d.toInt()) else "$d days"

    /** Finds an amount like "₹1,500", "rs 800", "1500 rupees", "2k". */
    fun amountIn(q: String): Paise? {
        Regex("(?:₹|rs\\.?|inr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(k)?").find(q)?.let { return toPaise(it.groupValues[1], it.groupValues[2]) }
        Regex("\\b([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(k|rupees|rs)\\b").find(q)?.let { return toPaise(it.groupValues[1], if (it.groupValues[2] == "k") "k" else "") }
        Regex("\\b(?:afford|buy|spend|for)\\s+(?:a\\s+|an\\s+)?(?:[a-z ]{0,20}?)([0-9]{2,7})\\b").find(q)?.let { return toPaise(it.groupValues[1], "") }
        return null
    }

    private fun toPaise(n: String, k: String): Paise {
        val v = n.replace(",", "").toDoubleOrNull() ?: return 0
        return Math.round((if (k == "k") v * 1000 else v) * 100)
    }
}

/** The 8 contextual micro-lessons. Each appears when its trigger pattern happens, never as homework. */
object Lessons {
    data class Lesson(val id: String, val title: String, val body: String, val trigger: String)

    val all = listOf(
        Lesson("small", "Small spends add up", "Four ₹150 snacks a week is ₹2,400 a month. You do not have to stop; just notice the ones that were not worth it.", "4+ spends under ₹200 in a week"),
        Lesson("first", "Pay yourself first", "Move money to your goal the day it arrives. What is left is what you live on, and saving stops being an afterthought.", "Money arrives"),
        Lesson("fixed", "Promises come first", "Rent and recharges are already spent, even before they leave. Ruvo sets them aside so Fuel shows what is truly free.", "First commitment added"),
        Lesson("buffer", "Spare days are freedom", "Days spare before your next money are your cushion for surprises. Protect two or three and the month feels calm.", "Buffer under 3 days"),
        Lesson("irregular", "Plan by your leanest month", "With freelance or part-time pay, plan as if every month were your quietest one. Good months then become savings.", "Irregular income chosen"),
        Lesson("split", "Splits without the awkwardness", "Send the UPI link the same day. People pay fastest while the dinner is still fresh in their memory.", "First shared payment"),
        Lesson("emergency", "Cushion before investing", "Investments can fall just when you need cash. An emergency fund stays put. Build that first.", "Asks about investing"),
        Lesson("pause", "The one-day pause", "For anything costing more than a day of spending, wait a day. If you still want it tomorrow, it is probably worth it.", "Before you buy over 1 day"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Picks the lesson that fits the current numbers best, for the card on Today. */
    fun forNow(input: RunwayInput, result: RunwayResult): Lesson {
        val week = input.transactions.filter { it.countsAsSpending && ChronoUnit.DAYS.between(it.date, result.today) in 0..6 }
        return when {
            week.count { it.yourShare <= 20_000 } >= 4 -> all[0]
            result.bufferDays != null && result.bufferDays in 0..2 -> all[3]
            result.bufferDays == null -> all[4]
            input.transactions.any { it.othersShare > 0 } -> all[5]
            input.goals.isNotEmpty() -> all[1]
            else -> all[7]
        }
    }
}

/**
 * The Marketplace: real student offers from real brands (Kumkum, 5 Nov 2026), each checked against public reports
 * or the brand's own page on the date shown. Every listing says where it comes from, when Ruvo last checked it and
 * when it ends, so nobody mistakes a stale offer for a live one. Prices change, so the app always asks people to
 * confirm on the brand's own app before paying. Ruvo is not paid for any listing and shows no ads.
 */
object Perks {
    data class Perk(
        val id: String,
        val brand: String,
        val offer: String,
        /** What it saves a month compared with the usual price, when that is known. 0 when it varies. */
        val monthlySaving: Paise,
        /** Save, Earn, Learn or Student offers. */
        val kind: String,
        /** Who published it, and what kind of publisher that is. */
        val source: String = "",
        val sourceType: String = "",
        /** When Ruvo last checked the listing, and when the offer ends (null if it has no end). */
        val checked: LocalDate = LocalDate.of(2026, 11, 5),
        val ends: LocalDate? = null,
        /** The one step to take it up. */
        val how: String = "",
        /** A short badge when the saving is not a simple monthly amount, e.g. "Free" or "App deals". */
        val badge: String? = null,
    )

    private val checkedOn = LocalDate.of(2026, 11, 5)

    val all = listOf(
        Perk(
            "spotify", "Spotify", "Premium Student at ₹69 a month, instead of ₹139 for Standard", 7_000, "Student offers",
            "Spotify India price change, reported May 2026", "Company pricing", checkedOn, null,
            "In Spotify, choose Premium Student and verify that you are enrolled at a recognised college.",
        ),
        Perk(
            "youtube", "YouTube Premium", "Student plan at ₹89 a month, instead of ₹149 for Individual", 6_000, "Student offers",
            "YouTube Premium India pricing", "Company pricing", checkedOn, null,
            "In the YouTube app, pick the Student plan and verify that you are a full-time student. Eligible for up to four years.",
        ),
        Perk(
            "swiggy", "Swiggy", "Student Rewards: discounted Swiggy One and One Lite memberships and food deals for verified students", 0, "Student offers",
            "Swiggy Student Rewards programme", "Company programme", checkedOn, null,
            "In the Swiggy app, search \"Identity card\" and upload a photo of your valid student ID to unlock the student rates.",
            badge = "Student ID",
        ),
        Perk(
            "swiggylite", "Swiggy One Lite", "₹99 for 3 months: 10 free food deliveries on orders above ₹149 and 10 on Instamart above ₹199", 0, "Save",
            "Swiggy One Lite launch details", "Company announcement", checkedOn, null,
            "Buy One Lite in the Swiggy app. It is worth it only if you would have ordered anyway.",
            badge = "₹99 / 3 mo",
        ),
        Perk(
            "mcdonalds", "McDonald's", "No student discount in India, but app-only deals such as McSaver meals in the Offers tab", 0, "Save",
            "McDonald's India app offers", "Company app", checkedOn, null,
            "Open the Offers tab in the McDonald's India app before you order; deals change often.",
            badge = "App deals",
        ),
        Perk(
            "github", "GitHub Student Developer Pack", "Free GitHub Pro and free partner tools and credits while you are a student", 0, "Learn",
            "education.github.com/pack", "Company website", checkedOn, null,
            "Apply at education.github.com/pack with your college email or a student ID.",
            badge = "Free",
        ),
        Perk(
            "figma", "Figma for Education", "Free Professional plan for verified students", 0, "Learn",
            "figma.com/education", "Company website", checkedOn, null,
            "Apply at figma.com/education; approval usually takes a few days.",
            badge = "Free",
        ),
        Perk(
            "internshala", "Internshala", "Internships and part-time work for students, many of them paid", 0, "Earn",
            "internshala.com", "Job platform", checkedOn, null,
            "Make a free profile and apply to a few roles that match your course.",
            badge = "Earn",
        ),
    )

    /** "Saves about 3 days a year" at the current daily spend: the yearly saving in days of usual spending. */
    fun daysPerYear(p: Perk, dailySpend: Paise): Int = if (dailySpend <= 0) 0 else (p.monthlySaving * 12 / dailySpend).toInt()

    /** "Saves you 2 days" at the current daily spend. */
    fun daysSaved(p: Perk, dailySpend: Paise): Int = if (dailySpend <= 0) 0 else (p.monthlySaving / dailySpend).toInt()

    /** In list order: what is current first, then ending soon, then needing a re-check, then over. */
    enum class State { ACTIVE, ENDING, RECHECK, EXPIRED }

    data class Status(val state: State, val daysLeft: Int?, val checkedDaysAgo: Int, val timeLeft: Float)

    /** Over once the end date has passed; ending within 14 days; needing a re-check if checked over 30 days ago. */
    fun status(p: Perk, today: LocalDate): Status {
        val ago = java.time.temporal.ChronoUnit.DAYS.between(p.checked, today).toInt()
        val left = p.ends?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it).toInt() }
        val total = p.ends?.let { java.time.temporal.ChronoUnit.DAYS.between(p.checked, it).toInt() }?.coerceAtLeast(1)
        val timeLeft = when {
            left == null || total == null -> 1f
            left < 0 -> 0f
            else -> (left.toFloat() / total).coerceIn(0f, 1f)
        }
        val state = when {
            left != null && left < 0 -> State.EXPIRED
            ago > 30 -> State.RECHECK
            left != null && left <= 14 -> State.ENDING
            else -> State.ACTIVE
        }
        return Status(state, left, ago, timeLeft)
    }

    /** All listings, current ones first, each group soonest-ending first. */
    fun listed(today: LocalDate): List<Perk> =
        all.sortedWith(compareBy<Perk>({ status(it, today).state.ordinal }, { status(it, today).daysLeft ?: Int.MAX_VALUE }))
}
