package com.ruvo.engine.guide

import com.ruvo.engine.Cadence
import com.ruvo.engine.RunwayInput
import com.ruvo.engine.RunwayResult
import java.time.temporal.ChronoUnit

/**
 * The Guide's reading shelf. Each book is a five-minute read in seven short pages:
 * the idea, a real student situation, why it matters, a question to reflect on, something to try this week,
 * a one-question check, and a way to put it to work in Ruvo.
 *
 * Plain ideas only: no stock tips, no named products, no promised returns, no streaks or guilt.
 * The eight Today lessons keep their ids here, so a lesson card opens straight into its book.
 */
object Library {

    enum class Shelf(val title: String, val blurb: String) {
        NEW("New this week", "Fresh reads, picked for students this week."),
        BASICS("Money basics", "What you have, what is free, and where it goes."),
        SAVE("Save better", "Ways to keep money for what matters."),
        SPEND("Spend better", "Spend on what you value, without the guilt."),
        STUDENT("Student money", "Hostels, stipends, splits and semester fees."),
        INVEST("Investing basics", "Ideas to know before you ever invest. No pressure, no deadline."),
    }

    /** What the last page offers to do in the app. */
    enum class Apply { BEFORE_YOU_BUY, NEW_GOAL, GOALS, SORT, MONEY, ASK_RUVO }

    data class Check(val question: String, val options: List<String>, val correct: Int, val because: String)

    data class Book(
        val id: String,
        val shelf: Shelf,
        val title: String,
        val minutes: Int,
        val idea: String,
        val situation: String,
        val why: String,
        val reflect: String,
        val tryIt: String,
        val check: Check,
        val applyLabel: String,
        val apply: Apply,
    ) {
        /** The seven pages in reading order, as (label, text). The check is shown as its question. */
        val pages: List<Pair<String, String>>
            get() = listOf(
                "The idea" to idea,
                "A real student situation" to situation,
                "Why it matters" to why,
                "Reflect" to reflect,
                "Try it this week" to tryIt,
                "Check your understanding" to check.question,
                "Put it to work in Ruvo" to applyLabel,
            )
    }

    val all: List<Book> = listOf(
        // New this week
        Book(
            "upi", Shelf.NEW, "Needs vs wants on UPI", 4,
            idea = "UPI makes paying feel like nothing happened. Sorting each payment as a need or a want brings the feeling back, without making you stop.",
            situation = "Meera scans for chai, a cab, a phone case and a late-night snack in one day. Each was under ₹300. Together they were ₹740, more than her day's Fuel.",
            why = "Needs keep your days running. Wants make them nicer. Both are fine; knowing which is which tells you what to trim first on a tight week.",
            reflect = "Think of your last five UPI payments. Which ones would you happily pay for again?",
            tryIt = "For three days, say \"need\" or \"want\" in your head before you scan. Do not change anything else.",
            check = Check("A friend's birthday dinner is…", listOf("Always a need", "A want, and a perfectly good one", "Something to avoid"), 1, "It is a want you value. Wants are allowed; they just come after needs."),
            applyLabel = "Sort this week's payments in two minutes.", apply = Apply.SORT,
        ),
        Book(
            "subs", Shelf.NEW, "Audit your subscriptions", 4,
            idea = "Small monthly charges hide well. Once a term, list every subscription and keep only the ones you actually used.",
            situation = "Arjun pays for music, two video apps, cloud storage and a fitness app he opened twice. That is ₹1,100 a month, about four days of his Fuel.",
            why = "Subscriptions sit in Fixed, so they shrink your free money every month whether you use them or not.",
            reflect = "Which subscription would you not notice if it stopped tomorrow?",
            tryIt = "Open your UPI autopay list and your app store subscriptions. Pause one you have not used in a month.",
            check = Check("A ₹149 monthly app costs about how much a year?", listOf("₹149", "₹900", "₹1,788"), 2, "Twelve months of ₹149 is ₹1,788. Small monthly amounts add up over a year."),
            applyLabel = "See what is in your Fixed bucket.", apply = Apply.MONEY,
        ),
        Book(
            "hostel", Shelf.NEW, "A hostel month in four weeks", 5,
            idea = "Split the month into four weeks and give each week its own Fuel. A slow first week no longer has to pay for a rushed last week.",
            situation = "Kavya's pocket money lands on the 1st. By the 20th she is counting coins. With a weekly amount, the last week feels like the first.",
            why = "Money that arrives in one go feels endless at the start. Weekly amounts turn one big number into four you can actually feel.",
            reflect = "In which week of the month do you usually feel the squeeze?",
            tryIt = "Divide this month's Fuel by the weeks left. Check that number each Sunday.",
            check = Check("If Fuel is ₹6,000 with 3 weeks left, a fair weekly amount is…", listOf("₹1,500", "₹2,000", "₹3,000"), 1, "₹6,000 over three weeks is ₹2,000 a week."),
            applyLabel = "Check what a purchase would do first.", apply = Apply.BEFORE_YOU_BUY,
        ),
        Book(
            "503020", Shelf.NEW, "The 50/30/20 idea, student edition", 5,
            idea = "A simple split: about half for needs, about a third for wants, and the rest for your future. Treat it as a starting shape, not a rule.",
            situation = "Rohan gets ₹12,000. Rent and mess are ₹7,000, more than half. So his version becomes 60/30/10, and that is fine for now.",
            why = "Any split beats no split. Naming the three parts is what makes money feel calmer, not hitting exact numbers.",
            reflect = "What would your own split look like this month?",
            tryIt = "Look at your Fixed, Fuel and Future buckets. Write down the rough percentage of each.",
            check = Check("If needs take 65% of your money, the 50/30/20 idea…", listOf("Has failed", "Can be adjusted to fit your life", "Means you must move out"), 1, "It is a guide. Adjust the shape to your real costs."),
            applyLabel = "Start a goal for your Future part.", apply = Apply.NEW_GOAL,
        ),

        // Money basics
        Book(
            "balance", Shelf.BASICS, "Balance vs what you can use", 4,
            idea = "Your balance is what is in the account. What you can use is what is left after promises and goals.",
            situation = "Sana sees ₹14,000 and feels rich. But ₹6,000 is rent next week and ₹2,000 is for her trip. She really has ₹6,000.",
            why = "Spending from your balance feels fine until the rent day. Spending from Fuel never surprises you.",
            reflect = "How often do you check your balance and feel more relaxed than you should?",
            tryIt = "Next time you check your bank app, open Ruvo too and compare the two numbers.",
            check = Check("Fuel is…", listOf("Your bank balance", "Balance minus Fixed and Future", "What you spent this month"), 1, "Fuel is what is free after promises and protected goals."),
            applyLabel = "See how your money is split.", apply = Apply.MONEY,
        ),
        Book(
            "cashflow", Shelf.BASICS, "Understanding cash flow", 4,
            idea = "Money arrives in bursts but leaves every day. Cash flow is the gap between those two rhythms.",
            situation = "Dev's stipend comes on the 5th, but rent is due on the 1st. For four days each month he runs on nothing.",
            why = "Knowing when money arrives matters as much as how much. Your runway is really a cash-flow forecast.",
            reflect = "What date does your money arrive, and which bills fall before it?",
            tryIt = "Write the dates of your next money and your next two bills side by side.",
            check = Check("A good runway means…", listOf("Fuel lasts until the next money arrives", "You never spend", "Your balance is high"), 0, "Runway is about lasting until money arrives again."),
            applyLabel = "Ask Ruvo when your money runs out.", apply = Apply.ASK_RUVO,
        ),
        Book(
            "where", Shelf.BASICS, "Where your money goes", 4,
            idea = "A vague worry about money shrinks once you see it as a few category totals.",
            situation = "Ishaan felt he spent \"everything on nothing\". Sorting one week showed ₹1,900 on food delivery. Now it was one number he could choose about.",
            why = "You cannot adjust what you cannot see. Categories turn feelings into facts.",
            reflect = "Which category do you think is your biggest? Check whether you are right.",
            tryIt = "Sort this week's payments, then look at the category totals in Money.",
            check = Check("The point of categories is to…", listOf("Feel guilty", "See where to make choices", "Spend less on everything"), 1, "Categories show where a small change would matter most."),
            applyLabel = "Sort this week's payments.", apply = Apply.SORT,
        ),
        Book(
            "fixed", Shelf.BASICS, "Promises come first", 4,
            idea = "Rent and recharges are already spent, even before they leave. Ruvo sets them aside so Fuel shows what is truly free.",
            situation = "Neha's ₹499 recharge and ₹4,500 PG rent are due this week. Ruvo has already moved them into Fixed, so her Fuel never counted them.",
            why = "Counting promised money as free is the most common reason a month ends early.",
            reflect = "Which payment surprises you most often, even though it comes every month?",
            tryIt = "Add one commitment you have not told Ruvo about yet.",
            check = Check("A recharge due in 5 days is…", listOf("Fuel until it is paid", "Fixed from today", "Future"), 1, "Promised money belongs in Fixed from the moment you know about it."),
            applyLabel = "Check your Fixed bucket.", apply = Apply.MONEY,
        ),

        // Save better
        Book(
            "first", Shelf.SAVE, "Pay yourself first", 4,
            idea = "Move money to your goal the day it arrives. What is left is what you live on, and saving stops being an afterthought.",
            situation = "Aarav used to save \"whatever is left\". Nothing was left. Now ₹1,000 goes to his laptop goal on the 1st, and he lives on the rest.",
            why = "Saving last depends on willpower all month. Saving first takes one decision.",
            reflect = "What would you save for if saving were automatic?",
            tryIt = "On your next money day, move a small, fixed amount to a goal before anything else.",
            check = Check("Paying yourself first means…", listOf("Treating yourself on payday", "Moving money to goals as soon as it arrives", "Paying friends back first"), 1, "Your future self gets paid before your spending starts."),
            applyLabel = "Pour into a goal.", apply = Apply.GOALS,
        ),
        Book(
            "protect", Shelf.SAVE, "Protect your goals", 4,
            idea = "Money with a name and a purpose is much harder to spend by accident.",
            situation = "Priya kept her trip money in the same account as everything else. It slowly became snacks. Named and protected, it stayed put.",
            why = "Ruvo never counts Future money as Fuel, so a protected goal cannot quietly shrink your runway.",
            reflect = "Is there money you are saving in your head but not anywhere real?",
            tryIt = "Give that money a goal with a name and a date.",
            check = Check("Money in a Ruvo goal…", listOf("Counts as Fuel", "Is never counted as spendable", "Is locked forever"), 1, "It stays out of Fuel. You can still release it any time; it is your call."),
            applyLabel = "Start a new goal.", apply = Apply.NEW_GOAL,
        ),
        Book(
            "buffer", Shelf.SAVE, "Spare days are freedom", 4,
            idea = "Days spare before your next money are your cushion for surprises. Protect two or three and the month feels calm.",
            situation = "Zoya had 1 spare day. A cracked phone screen wiped it out. Next month she aimed for 4 spare days, and the next surprise was only an annoyance.",
            why = "Spare days absorb the unexpected. Without them, every surprise becomes a crisis.",
            reflect = "How many spare days would make you feel relaxed?",
            tryIt = "This week, try to add one spare day by skipping one want you would not miss.",
            check = Check("+4 days spare means…", listOf("Money arrives in 4 days", "You make it to your next money with 4 days to spare", "You overspent by 4 days"), 1, "Buffer is runway minus days until the next money."),
            applyLabel = "Ask Ruvo what changes your runway.", apply = Apply.ASK_RUVO,
        ),
        Book(
            "cushion", Shelf.SAVE, "Build a small cushion", 4,
            idea = "A small emergency buffer turns a surprise into an inconvenience. Start with one week of spending.",
            situation = "When Kabir's laptop charger died before exams, his ₹2,000 cushion paid for it. No borrowing, no stress.",
            why = "Surprises are certain; only their timing is not. A cushion means you never have to borrow for them.",
            reflect = "What was the last surprise cost you had to scramble for?",
            tryIt = "Create an emergency goal for one week of your daily spending.",
            check = Check("A first emergency cushion should cover about…", listOf("One week of spending", "One year of rent", "Nothing, until you earn more"), 0, "Start small. One week is enough to begin."),
            applyLabel = "Start an emergency goal.", apply = Apply.NEW_GOAL,
        ),

        // Spend better
        Book(
            "small", Shelf.SPEND, "Small spends add up", 4,
            idea = "Four ₹150 snacks a week is ₹2,400 a month. You do not have to stop; just notice the ones that were not worth it.",
            situation = "Rhea's evening Blinkit order became a habit. Keeping the ones she enjoyed and skipping the bored ones saved ₹900 a month.",
            why = "Small spends are easy to miss and easy to change. They are where most extra days hide.",
            reflect = "Which small spend do you enjoy most? Which one happens out of habit?",
            tryIt = "This week, mark small spends as worth it or not in your weekly review.",
            check = Check("₹100 a day for 30 days is…", listOf("₹1,000", "₹3,000", "₹300"), 1, "₹100 × 30 = ₹3,000. Daily amounts multiply quickly."),
            applyLabel = "Try Before you buy on your next snack.", apply = Apply.BEFORE_YOU_BUY,
        ),
        Book(
            "pause", Shelf.SPEND, "The one-day pause", 4,
            idea = "For anything costing more than a day of spending, wait a day. If you still want it tomorrow, it is probably worth it.",
            situation = "Tanvi nearly bought ₹3,500 earbuds at midnight. The next morning she still wanted them, and bought them without a shred of guilt.",
            why = "A pause separates real wants from passing moods. It never blocks you; it just gives tomorrow a vote.",
            reflect = "What did you last buy that you stopped using within a week?",
            tryIt = "Add the next big item to Before you buy and decide tomorrow.",
            check = Check("The one-day pause is meant to…", listOf("Stop all purchases", "Give you time to be sure", "Make shopping harder"), 1, "It is about being sure, not about saying no."),
            applyLabel = "Open Before you buy.", apply = Apply.BEFORE_YOU_BUY,
        ),
        Book(
            "purpose", Shelf.SPEND, "Spend with purpose", 4,
            idea = "Spending on what you value feels better than spending on autopilot, even when the amounts are the same.",
            situation = "Aditya cut random delivery orders but kept his Sunday cricket and cafe with friends. He spent a little less and enjoyed it a lot more.",
            why = "Money is a tool for the life you want. Purposeful spending is the opposite of guilt.",
            reflect = "Name three things worth every rupee to you.",
            tryIt = "In this week's review, notice which payments matched those three things.",
            check = Check("Spending with purpose means…", listOf("Never buying wants", "Choosing wants you truly value", "Only buying needs"), 1, "Wants you value are exactly what Fuel is for."),
            applyLabel = "Open this week's review.", apply = Apply.MONEY,
        ),

        // Student money
        Book(
            "irregular", Shelf.STUDENT, "Plan by your leanest month", 4,
            idea = "With freelance or part-time pay, plan as if every month were your quietest one. Good months then become savings.",
            situation = "Farhan's design gigs paid ₹4,000 one month and ₹15,000 the next. Planning around ₹4,000 turned the good months into a cushion.",
            why = "Averages lie when income jumps around. The leanest month is the honest number.",
            reflect = "What was your smallest month this year?",
            tryIt = "Use your smallest recent month as the income for this month's plan.",
            check = Check("With uneven income, plan using…", listOf("Your best month", "Your average", "Your leanest month"), 2, "The leanest month keeps you safe in a slow month."),
            applyLabel = "Ask Ruvo about irregular income.", apply = Apply.ASK_RUVO,
        ),
        Book(
            "split", Shelf.STUDENT, "Splits without the awkwardness", 4,
            idea = "Send the UPI link the same day. People pay fastest while the dinner is still fresh in their memory.",
            situation = "Ananya paid ₹1,800 for the group pizza. She sent links that night and was paid back by morning.",
            why = "Money owed to you is not Fuel until it arrives. Quick reminders keep your runway honest.",
            reflect = "Is anyone holding money of yours right now?",
            tryIt = "Split one shared payment in Ruvo and send the link today.",
            check = Check("Money friends owe you…", listOf("Counts as Fuel right away", "Becomes Fuel when they pay", "Is lost"), 1, "It moves into Fuel the moment they pay you back."),
            applyLabel = "See what is owed to you.", apply = Apply.MONEY,
        ),
        Book(
            "discounts", Shelf.STUDENT, "Student discounts, honestly", 4,
            idea = "A discount only saves money on something you were going to buy anyway.",
            situation = "Ritika signed up for a 50% off course she never opened. The \"saving\" cost her ₹1,200.",
            why = "Offers create wants. Honest discounts lower costs you already have.",
            reflect = "Have you ever bought something because it was on sale?",
            tryIt = "Before using an offer this week, ask: would I pay for this at full price?",
            check = Check("A discount on something you did not need…", listOf("Saves money", "Still costs money", "Is free"), 1, "Spending less on something unneeded is still spending."),
            applyLabel = "Check a purchase before you buy.", apply = Apply.BEFORE_YOU_BUY,
        ),
        Book(
            "college", Shelf.STUDENT, "Plan around college costs", 4,
            idea = "Fees, exam forms and project costs are predictable. Give them a date and set money aside early.",
            situation = "Every semester Yash was surprised by a ₹3,000 lab fee. As a goal due in March, it became ₹750 a month.",
            why = "Predictable costs are only emergencies when you forget them.",
            reflect = "Which college cost caught you off guard last semester?",
            tryIt = "Make a goal for your next known college cost with its due date.",
            check = Check("A ₹3,000 fee in 4 months is about how much a month?", listOf("₹750", "₹3,000", "₹300"), 0, "₹3,000 over four months is ₹750 a month."),
            applyLabel = "Start a goal for it.", apply = Apply.NEW_GOAL,
        ),

        // Investing basics
        Book(
            "invest", Shelf.INVEST, "What investing is", 5,
            idea = "Investing means putting money into assets that may grow over time, with no guarantee that they will.",
            situation = "Saloni heard a friend doubled his money. She learned he had also lost half of it the year before.",
            why = "Growth and risk come together. Understanding both is the first step, long before choosing anything.",
            reflect = "If an investment fell by a third, how would you feel?",
            tryIt = "Read one beginner explainer on investing from a regulator or your bank, not a social media tip.",
            check = Check("Investing…", listOf("Always grows your money", "May grow money, with no guarantee", "Is the same as saving"), 1, "There is always a chance values fall."),
            applyLabel = "Ask Ruvo to explain investing simply.", apply = Apply.ASK_RUVO,
        ),
        Book(
            "saveinvest", Shelf.INVEST, "Saving vs investing", 4,
            idea = "Saving is for goals within a few years. Investing is for goals much further away.",
            situation = "Kunal wanted a trip next March. He saved for it rather than investing, because he could not risk it being worth less in March.",
            why = "Money you need soon should not be exposed to ups and downs.",
            reflect = "Which of your goals are soon, and which are years away?",
            tryIt = "Label each of your goals as soon or later.",
            check = Check("Money for a trip in 5 months is best…", listOf("Saved", "Invested", "Spent now"), 0, "Short-term money should be safe and ready."),
            applyLabel = "Look at your goals.", apply = Apply.GOALS,
        ),
        Book(
            "risk", Shelf.INVEST, "Understanding risk", 4,
            idea = "Risk is the chance that the value falls. Higher possible gains usually come with higher risk.",
            situation = "Two friends: one kept money safe and earned a little, one chased big gains and lost some. Neither was foolish; they took different risks.",
            why = "Knowing how much risk you can live with matters more than any tip.",
            reflect = "Would you rather have steady small growth, or a chance of more with a chance of loss?",
            tryIt = "Notice any \"guaranteed high returns\" message this week and treat it as a warning sign.",
            check = Check("\"Guaranteed high returns\" usually means…", listOf("A great deal", "A warning sign", "A government scheme"), 1, "High returns are never guaranteed."),
            applyLabel = "Ask Ruvo about risk.", apply = Apply.ASK_RUVO,
        ),
        Book(
            "emergency", Shelf.INVEST, "Cushion before investing", 4,
            idea = "Investments can fall just when you need cash. An emergency fund stays put. Build that first.",
            situation = "Mihir invested his savings, then needed ₹5,000 for a family trip home. He had to sell at a loss.",
            why = "A cushion means you never have to sell an investment at a bad time.",
            reflect = "Could you cover a ₹5,000 surprise today without borrowing?",
            tryIt = "Check whether you have an emergency goal yet.",
            check = Check("What comes first?", listOf("Investing", "An emergency cushion", "A credit card"), 1, "A cushion first, so investments can wait out bad times."),
            applyLabel = "Start an emergency goal.", apply = Apply.NEW_GOAL,
        ),
        Book(
            "beforeinvest", Shelf.INVEST, "Before a student invests", 5,
            idea = "Learn the basics, keep a cushion, and never feel pressured. There is no deadline.",
            situation = "A senior kept telling Pooja she was \"late\" to invest at 19. She waited, built her cushion, and learned first. She was not late.",
            why = "Pressure leads to rushed choices. Your pace is the right pace.",
            reflect = "Who or what is making you feel you should invest now?",
            tryIt = "Write three questions you would want answered before investing anything.",
            check = Check("If you feel pressured to invest quickly, you should…", listOf("Act fast", "Slow down", "Borrow to invest"), 1, "Good decisions are never rushed."),
            applyLabel = "Ask Ruvo your questions.", apply = Apply.ASK_RUVO,
        ),
    )

    fun byId(id: String): Book? = all.firstOrNull { it.id == id }

    fun shelf(s: Shelf): List<Book> = all.filter { it.shelf == s }

    /**
     * Up to three books picked from what actually happened: the Today lesson first, then ones that fit
     * the numbers. Unread books are preferred.
     */
    fun forYou(input: RunwayInput, result: RunwayResult, read: Set<String> = emptySet()): List<Book> {
        val week = input.transactions.filter { it.countsAsSpending && ChronoUnit.DAYS.between(it.date, result.today) in 0..6 }
        val picks = buildList {
            add(Lessons.forNow(input, result).id)
            if (week.count { it.yourShare <= 30_000 } >= 3) add("upi")
            if (input.commitments.any { it.name.contains("sub", true) || it.name.contains("spotify", true) || it.name.contains("netflix", true) }) add("subs")
            if (input.goals.isEmpty()) add("protect") else add("first")
            if (result.bufferDays != null && result.bufferDays < 3) add("buffer")
            if (input.income.cadence == Cadence.IRREGULAR) add("irregular")
            if (input.income.cadence == Cadence.LUMP_SUM) add("hostel")
            add("balance")
        }.distinct()
        val books = picks.mapNotNull { byId(it) }
        return (books.filter { it.id !in read } + books.filter { it.id in read }).take(3)
    }
}
