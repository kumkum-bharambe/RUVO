package com.ruvo.engine.guide

/**
 * Choose-your-path stories inside Guide books. A short real-life moment, two or three choices, and an ending that
 * says what happened and what to take from it. No ending scolds; the gentlest one still teaches.
 */
object Stories {

    data class Choice(val label: String, val next: String)

    /** One moment in a story. With no choices it is an ending, and [lesson] is what to take away. */
    data class Scene(val id: String, val text: String, val choices: List<Choice> = emptyList(), val lesson: String? = null) {
        val isEnding: Boolean get() = choices.isEmpty()
    }

    data class Story(val id: String, val title: String, val scenes: List<Scene>) {
        val start: Scene get() = scenes.first()
        fun scene(id: String): Scene? = scenes.firstOrNull { it.id == id }
    }

    val all: List<Story> = listOf(
        Story(
            "concert", "The concert ticket",
            listOf(
                Scene(
                    "start",
                    "Your friends are buying tickets for Saturday's concert: ₹1,800. You have ₹4,200 of Fuel, money arrives in 12 days, and you usually spend about ₹250 a day.",
                    listOf(Choice("Buy it right now", "buy"), Choice("Check what it costs in days first", "check"), Choice("Skip it", "skip")),
                ),
                Scene(
                    "buy",
                    "You buy it. Your runway drops from about 16 days to 9. Money arrives in 12, so you are about 3 days short.",
                    listOf(Choice("Spend a little less each day", "trim"), Choice("Borrow from a friend", "borrow")),
                ),
                Scene(
                    "check",
                    "₹1,800 is about 7 days of your usual spending. Buying it leaves you about 3 days short of your next money.",
                    listOf(Choice("Buy it and plan the week", "trim"), Choice("Look for a cheaper seat", "cheaper")),
                ),
                Scene("trim", "You keep to about ₹180 a day for the next ten days.", lesson = "The gap closes and the concert still happens. Deciding on purpose, then adjusting, is a good trade."),
                Scene("borrow", "A friend lends you ₹800 and you pay it back the day your money lands.", lesson = "Borrowing from friends works when you repay exactly when you said. Planning first usually means you never need to."),
                Scene("cheaper", "A ₹1,000 seat further back is still available.", lesson = "Same night, a smaller dent: ₹3,200 at ₹250 a day just carries you to your next money."),
                Scene("skip", "You stay in and watch the videos later.", lesson = "Skipping is fine if it matters less to you. If it matters a lot, planning for it beats missing out every time."),
            ),
        ),
        Story(
            "refund", "The refund call",
            listOf(
                Scene(
                    "start",
                    "A caller says he is from a food delivery app: your ₹649 refund is stuck. He will send a request on UPI, and you just enter your PIN to receive it.",
                    listOf(Choice("Enter the PIN to get the refund", "pin"), Choice("Ask him to send it to your bank instead", "ask"), Choice("Hang up and check the app yourself", "hang")),
                ),
                Scene(
                    "ask",
                    "He gets impatient. The refund will expire in five minutes, he says, so you need to be quick.",
                    listOf(Choice("Hurry and do it", "pin"), Choice("Hang up", "hang")),
                ),
                Scene(
                    "pin",
                    "₹6,490 leaves your account. The \"refund\" was a collect request: a UPI PIN is only ever needed to send money, never to receive it.",
                    listOf(Choice("Call 1930 right away", "report"), Choice("Wait and see if it comes back", "wait")),
                ),
                Scene("report", "You call 1930, report on cybercrime.gov.in and tell your bank within the hour.", lesson = "Reporting fast gives the best chance of stopping the money. Save 1930 in your contacts today."),
                Scene("wait", "A day later the money has not come back.", lesson = "Waiting makes fraud harder to stop. Report within hours at 1930 or cybercrime.gov.in, and tell your bank."),
                Scene("hang", "You open the app yourself. There is no refund issue at all.", lesson = "Real refunds arrive on their own. A rush plus a request for your PIN or OTP is the sign of a scam."),
            ),
        ),
        Story(
            "card", "Your first credit card",
            listOf(
                Scene(
                    "start",
                    "Your first credit card has a ₹20,000 limit. You buy a ₹12,000 phone on it. The bill says: total due ₹12,000, minimum due ₹600.",
                    listOf(Choice("Pay the full ₹12,000", "full"), Choice("Pay the ₹600 minimum", "min"), Choice("Turn it into EMIs", "emi")),
                ),
                Scene(
                    "min",
                    "Next month you still owe almost ₹12,000. Interest of around 3.5% a month was added to what was left.",
                    listOf(Choice("Keep paying the minimum", "minforever"), Choice("Clear it over three months", "three")),
                ),
                Scene("full", "You pay the full amount by the due date.", lesson = "No interest at all. Used this way, a card costs nothing and slowly builds a good credit history."),
                Scene("minforever", "Months pass and the balance barely moves.", lesson = "At the minimum due, ₹12,000 can take years to clear and cost thousands in interest. The minimum only keeps the card in good standing."),
                Scene("three", "You pay about ₹4,300 a month for three months.", lesson = "Under ₹900 of interest in all, far less than the minimum path. Clearing fast is what keeps a card cheap."),
                Scene("emi", "The bank offers 6 EMIs at 16% a year.", lesson = "That is about ₹2,090 a month and roughly ₹550 extra in all, plus any processing fee. Always check the rate and fee first."),
            ),
        ),
        Story(
            "stipend", "Your first stipend",
            listOf(
                Scene(
                    "start",
                    "Your first internship stipend lands: ₹15,000, more than you have ever had at once. Rent of ₹6,000 is due on the 5th.",
                    listOf(Choice("Celebrate with a big shopping trip", "shop"), Choice("Give every rupee a job first", "jobs")),
                ),
                Scene(
                    "shop",
                    "By the 4th you have spent ₹10,000 and rent is due tomorrow.",
                    listOf(Choice("Ask family to help this once", "ask"), Choice("Plan next month's stipend on day one", "jobs")),
                ),
                Scene(
                    "jobs",
                    "You set aside rent and bills, move ₹1,500 to a goal and keep the rest as Fuel for the month.",
                    listOf(Choice("Keep a treat in the plan", "treat")),
                ),
                Scene("treat", "You keep ₹1,000 for a celebration dinner with friends.", lesson = "Celebrating is part of it. A planned treat feels better than a surprise shortfall."),
                Scene("ask", "Your family helps, and you talk about it calmly.", lesson = "Asking for help is okay. Giving the money jobs on the day it arrives keeps rent safe next time."),
            ),
        ),
    )

    fun byId(id: String): Story? = all.firstOrNull { it.id == id }
}
