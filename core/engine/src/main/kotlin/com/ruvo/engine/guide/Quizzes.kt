package com.ruvo.engine.guide

import com.ruvo.engine.guide.Library.Check

/**
 * Two more questions for each of the original books, so every book ends with a three-question quiz. Each answer
 * explains itself kindly; wrong answers are part of learning. New books carry their extra questions themselves.
 */
internal object Quizzes {
    val extra: Map<String, List<Check>> = mapOf(
        "upi" to listOf(
            Check("Four ₹180 UPI payments in a day add up to…", listOf("₹180", "₹540", "₹720"), 2, "4 × ₹180 is ₹720. Small scans add up quickly."),
            Check("Saying \"need\" or \"want\" before you pay helps because…", listOf("It stops you spending", "It makes you notice the payment", "It lowers the price"), 1, "Noticing is the whole trick. You still choose."),
        ),
        "subs" to listOf(
            Check("Subscriptions usually belong in…", listOf("Fixed", "Fuel", "Future"), 0, "They repeat on a schedule, so they are a promise already made."),
            Check("The easiest way to find every subscription is…", listOf("Guess", "Check UPI autopay and app store lists", "Wait for the bank to tell you"), 1, "Both lists show what renews by itself."),
        ),
        "hostel" to listOf(
            Check("Weekly amounts help most when money arrives…", listOf("Every day", "Once a month or term", "Never"), 1, "One big arrival feels endless; weeks make it feel real."),
            Check("If week one comes in under its amount, the spare money…", listOf("Is gone", "Can roll into later weeks", "Must be spent"), 1, "Carrying it forward is what makes the last week easier."),
        ),
        "503020" to listOf(
            Check("In 50/30/20, the 20 is for…", listOf("Fun", "Rent", "Your future, like goals and a cushion"), 2, "The last part is the money you protect."),
            Check("₹10,000 split 50/30/20 gives needs…", listOf("₹2,000", "₹3,000", "₹5,000"), 2, "Half of ₹10,000 is ₹5,000."),
        ),
        "balance" to listOf(
            Check("₹10,000 balance, ₹4,000 rent due, ₹1,000 for a goal. What can you use?", listOf("₹10,000", "₹6,000", "₹5,000"), 2, "₹10,000 minus ₹4,000 minus ₹1,000 is ₹5,000."),
            Check("Your balance feels high just after money arrives because…", listOf("It includes money already promised", "Banks add extra", "Prices drop"), 0, "Rent and bills are still sitting inside it."),
        ),
        "cashflow" to listOf(
            Check("Rent on the 1st and stipend on the 5th means…", listOf("No problem", "Four days to cover first", "Rent is free"), 1, "Bills that fall before money arrives need covering from what you have."),
            Check("Cash flow is about…", listOf("How much you earn", "When money comes in and goes out", "Your savings rate"), 1, "Timing, not just amounts."),
        ),
        "where" to listOf(
            Check("The best first step to see where money goes is…", listOf("Cut everything", "Look at the last few weeks of spending", "Open a new account"), 1, "You can only change what you can see."),
            Check("If food is your biggest category, that is…", listOf("Bad", "Normal, and useful to know", "A mistake"), 1, "It is information, not a judgement."),
        ),
        "fixed" to listOf(
            Check("Which is a Fixed cost?", listOf("Monthly hostel rent", "A movie on Friday", "Street food"), 0, "Rent is a promise that repeats; the others are choices."),
            Check("Setting Fixed costs aside first means…", listOf("Bills never surprise you", "You spend more", "You save nothing"), 0, "Promises kept first make the rest truly free."),
        ),
        "first" to listOf(
            Check("\"Pay yourself first\" means…", listOf("Treat yourself first", "Move money to your goal the day money arrives", "Spend first, save later"), 1, "Saving first means it actually happens."),
            Check("Even ₹200 a month toward a goal…", listOf("Is pointless", "Builds the habit and adds up", "Must be bigger"), 1, "The habit matters more than the size."),
        ),
        "protect" to listOf(
            Check("Money protected for a goal should…", listOf("Count as spendable", "Stay out of your everyday spending", "Be spent first"), 1, "That is why Ruvo never counts Future in your runway."),
            Check("A goal with a name and a date is…", listOf("Easier to stick to", "Harder to reach", "The same as no goal"), 0, "A clear picture keeps it real."),
        ),
        "buffer" to listOf(
            Check("Spare days are the days your Fuel lasts…", listOf("Before money arrives", "Beyond your next money", "On weekends"), 1, "They are the cushion past your next income."),
            Check("More spare days mean…", listOf("Less stress when plans change", "You must spend more", "Nothing"), 0, "Freedom to handle surprises."),
        ),
        "cushion" to listOf(
            Check("A cushion is for…", listOf("Sales", "Surprises like a repair or a trip home", "Weekends"), 1, "It turns a surprise into an inconvenience."),
            Check("A good first cushion for a student is…", listOf("Any small amount, built slowly", "Three years of costs", "Nothing"), 0, "Start small; ₹2,000 already helps."),
        ),
        "small" to listOf(
            Check("₹150 four times a week is about how much a month?", listOf("₹600", "₹1,500", "₹2,400"), 2, "₹600 a week over four weeks is ₹2,400."),
            Check("The aim with small spends is to…", listOf("Stop them all", "Keep the ones worth it to you", "Feel guilty"), 1, "Notice them, then keep what you value."),
        ),
        "pause" to listOf(
            Check("The one-day pause works because…", listOf("Prices change", "The urge often fades", "Shops close"), 1, "If you still want it tomorrow, it is a real want."),
            Check("After a pause, buying it anyway is…", listOf("A failure", "Fine; you chose on purpose", "Not allowed"), 1, "The pause is about choosing, not refusing."),
        ),
        "purpose" to listOf(
            Check("Spending with purpose means…", listOf("Spending less on everything", "Spending more on what you value, less on what you do not", "Never spending"), 1, "It is about matching money to what matters."),
            Check("Which question helps most before a purchase?", listOf("Is it on sale?", "Will I be glad I spent this next week?", "Do my friends have it?"), 1, "Thinking a week ahead shows what you value."),
        ),
        "irregular" to listOf(
            Check("If income varies, plan your month around…", listOf("Your best month", "Your leanest month", "An average guess"), 1, "If the lean month works, the rest feel easy."),
            Check("Money above your lean amount in a good month can…", listOf("Fill the cushion for lean months", "Must be spent", "Disappear"), 0, "Good months can carry the lean ones."),
        ),
        "split" to listOf(
            Check("You paid ₹1,200 for dinner for four. Your share is…", listOf("₹1,200", "₹300", "₹400"), 1, "₹1,200 split four ways is ₹300 each."),
            Check("The kindest time to send a split request is…", listOf("Weeks later", "The same day, with the amount clear", "Never"), 1, "Fresh and clear avoids awkwardness."),
        ),
        "discounts" to listOf(
            Check("A discount only saves money if…", listOf("You would have bought it anyway", "It is over 50%", "It ends soon"), 0, "Otherwise it is new spending with a sticker on it."),
            Check("\"Ends tonight\" offers are designed to…", listOf("Help you plan", "Rush your decision", "Save you time"), 1, "Urgency is a sales tool; a pause protects you."),
        ),
        "college" to listOf(
            Check("Semester fees due in four months are easiest to meet by…", listOf("Hoping", "Setting a little aside each month now", "Borrowing at the last minute"), 1, "Small monthly amounts make big dates calm."),
            Check("₹8,000 due in 4 months means setting aside about…", listOf("₹1,000 a month", "₹2,000 a month", "₹4,000 a month"), 1, "₹8,000 over four months is ₹2,000 a month."),
        ),
        "invest" to listOf(
            Check("Investing means…", listOf("Money that is guaranteed to grow", "Putting money where it may grow, with some risk", "Saving in a piggy bank"), 1, "Growth is possible, never promised."),
            Check("Investing makes most sense for money you…", listOf("Need next week", "Will not need for years", "Owe a friend"), 1, "Time lets ups and downs even out."),
        ),
        "saveinvest" to listOf(
            Check("Saving is best for…", listOf("Money you need soon or for surprises", "Money for 20 years", "Nothing"), 0, "Saving keeps money safe and ready."),
            Check("Investing is best for…", listOf("Rent next month", "Long-term goals", "Tomorrow's lunch"), 1, "It needs time to ride out the ups and downs."),
        ),
        "risk" to listOf(
            Check("Higher possible returns usually come with…", listOf("Lower risk", "Higher risk", "No risk"), 1, "Risk and possible reward travel together."),
            Check("Anyone promising high, guaranteed returns is…", listOf("Trustworthy", "A warning sign", "Normal"), 1, "No real investment guarantees high returns."),
        ),
        "emergency" to listOf(
            Check("Before investing, it helps to have…", listOf("A cushion for surprises", "A new phone", "A credit card"), 0, "Then a surprise never forces you to sell at a bad time."),
            Check("An emergency fund should be…", listOf("Easy to reach quickly", "Locked for years", "Spent each month"), 0, "It is only useful if you can get it fast."),
        ),
        "beforeinvest" to listOf(
            Check("A good first step before investing is to…", listOf("Learn the basics", "Follow a tip from social media", "Invest borrowed money"), 0, "Understanding comes before money."),
            Check("Investing with borrowed money is…", listOf("Safe", "Risky, because you owe it even if it falls", "Free"), 1, "Losses plus a loan to repay is a hard spot."),
        ),
    )
}
