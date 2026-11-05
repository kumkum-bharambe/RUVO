package com.ruvo.engine.guide

import com.ruvo.engine.guide.Library.Apply
import com.ruvo.engine.guide.Library.Book
import com.ruvo.engine.guide.Library.Check
import com.ruvo.engine.guide.Library.Shelf

/**
 * The newer shelves (staying safe on UPI, credit and borrowing, first salary and tax) and the deeper investing
 * books, each with the extras real books have: a rule of thumb, a "did you know", key takeaways and a three-question
 * quiz. Facts here are the stable kind; anything that changes yearly (tax slabs, rates) points to the official
 * source instead of a number. Investing is explained in general terms: never a named fund, share or tip.
 */
internal object MoreBooks {

    val all: List<Book> = listOf(
        // Stay safe on UPI
        Book(
            "refunds", Shelf.SAFETY, "Refund calls and collect requests", 5,
            idea = "Money comes to you without a PIN. A UPI PIN is only ever needed to send money, so anyone asking for it to \"give you a refund\" is taking money, not giving it.",
            situation = "Ishaan gets a call about a stuck ₹649 food refund. The caller sends a request and says, \"just enter your PIN to receive it\". It is a collect request for ₹6,490.",
            why = "Scams lean on hurry and good manners. Knowing the one rule about the PIN lets you say no calmly, even to a confident voice.",
            reflect = "Has anyone ever rushed you on a money call or message? What made it feel urgent?",
            tryIt = "Save 1930, the national cyber fraud helpline, in your contacts now, and open your UPI app's list of pending requests once a week.",
            check = Check("To receive money on UPI you need to…", listOf("Enter your UPI PIN", "Do nothing; it just arrives", "Share an OTP"), 1, "Receiving money never needs a PIN or an OTP."),
            more = listOf(
                Check("A caller says your refund expires in five minutes. This is…", listOf("Normal", "A pressure trick", "A bank rule"), 1, "Real refunds do not expire in minutes. Urgency is a scam sign."),
                Check("If money is taken by fraud, the fastest step is to…", listOf("Wait a day", "Call 1930 and tell your bank", "Post about it"), 1, "Reporting within hours gives the best chance of stopping it."),
            ),
            applyLabel = "Ask Ruvo what to do if this ever happens.", apply = Apply.ASK_RUVO,
            rule = "Your UPI PIN only sends money. If anyone asks for it so you can receive money, hang up.",
            fact = "India's cyber fraud helpline is 1930, and complaints can also be filed at cybercrime.gov.in.",
            takeaways = listOf("A PIN is only for sending money.", "Hurry is the scammer's tool; slowing down is yours.", "Report fraud fast: 1930, cybercrime.gov.in and your bank."),
            story = "refund",
        ),
        Book(
            "otp", Shelf.SAFETY, "OTPs, PINs and screen sharing", 4,
            idea = "An OTP is a key to your account for a few minutes. No bank, app or delivery agent ever needs you to read it out or to install an app so they can \"help\".",
            situation = "Nisha's \"bank\" calls about a blocked card and asks her to install a support app. It is a screen-sharing app: the caller can now see every OTP that arrives.",
            why = "Most account takeovers do not break any code. They simply ask, kindly and quickly, for the code.",
            reflect = "Which apps on your phone could see your screen or your messages?",
            tryIt = "Check your phone for screen-sharing or remote-help apps you do not use, and remove them.",
            check = Check("Your bank calls and asks for the OTP you just received. You…", listOf("Read it out", "Never share it and call the bank's own number", "Send it by SMS"), 1, "Banks never ask for OTPs. Hang up and call the number on your card or the bank's site."),
            more = listOf(
                Check("A screen-sharing app lets the other person…", listOf("See your screen, including OTPs", "Only see your photos", "Nothing"), 0, "That is why scammers ask you to install one."),
                Check("A link in an SMS says your account is blocked. You should…", listOf("Tap it quickly", "Open your bank's app yourself instead", "Reply with your details"), 1, "Go to the app or site you already trust, never the link."),
            ),
            applyLabel = "Ask Ruvo how to check a suspicious message.", apply = Apply.ASK_RUVO,
            rule = "No real bank, app or company will ever ask for your OTP, PIN or to install a screen-sharing app.",
            fact = "Phishing means fake messages or calls that pretend to be a bank or company to get your details.",
            takeaways = listOf("OTPs and PINs are never shared, with anyone.", "Do not install apps a caller asks you to.", "Open your bank's own app instead of tapping links."),
        ),
        Book(
            "loanapps", Shelf.SAFETY, "Fake jobs and instant loan apps", 5,
            idea = "If a job asks you to pay first, it is not a job. If a loan app wants your contacts and photos, it is not a safe lender.",
            situation = "Karan is offered ₹800 a day for \"liking videos\". After two small payouts he is asked to deposit ₹5,000 to unlock bigger tasks. The money never comes back.",
            why = "These schemes pay a little first to win your trust, then ask for more. Instant loan apps can harass you using your contact list.",
            reflect = "What would a real employer or a real bank never ask you for?",
            tryIt = "Before any loan app, check that it names the registered bank or finance company behind it, and look that company up yourself.",
            check = Check("A \"job\" needs a ₹2,000 registration fee to start. It is most likely…", listOf("A normal job", "A scam", "A government scheme"), 1, "Real jobs pay you; they do not charge you to start."),
            more = listOf(
                Check("A loan app asks for access to all your contacts and photos. That is…", listOf("Standard", "A red flag", "Needed for a loan"), 1, "Safe lenders do not need your gallery or contact list."),
                Check("Early small payouts from a task scheme are meant to…", listOf("Win your trust before a bigger ask", "Show it is safe", "Pay your taxes"), 0, "That is how the trap is set."),
            ),
            applyLabel = "Ask Ruvo how to tell a real offer from a fake one.", apply = Apply.ASK_RUVO,
            rule = "Real jobs pay you. Anyone who asks you to pay to earn is taking, not giving.",
            fact = "Genuine digital lenders name the bank or regulated finance company behind them, and RBI publishes lists you can check.",
            takeaways = listOf("Never pay to get a job.", "Loan apps should not need your contacts or photos.", "Check who is really behind any lender."),
        ),

        // Credit and borrowing
        Book(
            "card", Shelf.CREDIT, "How a credit card really works", 5,
            idea = "A credit card is a short loan you can make free: pay the total due by the due date and there is no interest at all. Pay less and interest starts on what is left.",
            situation = "Tanvi buys a ₹12,000 phone on her first card and pays only the ₹600 minimum. Next month she still owes almost ₹12,000, because interest was added.",
            why = "Cards are useful and build a credit history. The cost hides in one word: \"minimum\".",
            reflect = "Would you rather pay ₹12,000 once, or ₹600 a month for years?",
            tryIt = "Use the calculator here with any amount and compare paying in full with paying the minimum.",
            check = Check("To pay no interest on a card, you pay…", listOf("The minimum due", "The total due by the due date", "Whenever you like"), 1, "Paying the full bill on time keeps a card interest-free."),
            more = listOf(
                Check("Paying only the minimum means the rest…", listOf("Is forgiven", "Collects interest", "Moves to next year free"), 1, "The minimum only keeps the account in good standing."),
                Check("A card's credit limit is…", listOf("Money you own", "The most you can borrow", "Your salary"), 1, "It is borrowing room, not savings."),
            ),
            applyLabel = "See what a big buy does to your runway first.", apply = Apply.BEFORE_YOU_BUY,
            rule = "Pay the total due, not the minimum, every single month.",
            fact = "Card interest is often around 3 to 4% a month, which works out to roughly 40% or more a year.",
            takeaways = listOf("Total due on time means zero interest.", "The minimum due is a trap if it becomes a habit.", "A limit is borrowing room, not money you have."),
            tool = Tools.Tool.CARD_MINIMUM, story = "card",
        ),
        Book(
            "emi", Shelf.CREDIT, "EMIs and pay later", 5,
            idea = "An EMI or a pay-later plan splits a price into months. It feels smaller, but you should always know the total you will pay, not just the monthly amount.",
            situation = "Aditya splits ₹30,000 earphones and a laptop bag into EMIs across two apps. Each is small. Together they take ₹4,000 from every month for a year.",
            why = "Monthly amounts hide the total, and several small EMIs at once quietly shrink your free money for months.",
            reflect = "How many things are you paying for in instalments right now?",
            tryIt = "Use the calculator to see the full cost of a loan before you choose an EMI.",
            check = Check("Before choosing an EMI, the most useful number is…", listOf("The monthly amount", "The total you will pay, fees included", "How many colours it comes in"), 1, "The total shows the real price."),
            more = listOf(
                Check("\"No-cost EMI\" can still cost you through…", listOf("A processing fee or a lost discount", "Nothing ever", "Free gifts"), 0, "Read the small print for fees."),
                Check("Missing a pay-later instalment can lead to…", listOf("Late fees and a lower credit score", "A reward", "Nothing"), 0, "Pay-later is still a loan."),
            ),
            applyLabel = "Add instalments to your Fixed costs so they never surprise you.", apply = Apply.MONEY,
            rule = "Before any EMI, find the total you will pay, not just the monthly amount.",
            fact = "\"No-cost EMI\" often means the interest is covered by a discount you give up, and a processing fee may still apply.",
            takeaways = listOf("Know the total, not just the monthly amount.", "Several small EMIs add up to one big promise.", "Pay-later is a loan, with fees if you miss a date."),
            tool = Tools.Tool.EMI,
        ),
        Book(
            "score", Shelf.CREDIT, "Your credit score", 4,
            idea = "A credit score is a number from 300 to 900 that tells lenders how reliably you repay. You build it slowly, mostly by paying on time.",
            situation = "Riya has never borrowed, so she has no score yet. A small card she pays in full every month gives her a good score within a year or so.",
            why = "A good score can make an education loan, a phone plan or even a rental easier and cheaper later.",
            reflect = "Which bills would you never want to miss, and how will you remember them?",
            tryIt = "If you have a card or loan, set a reminder two days before each due date.",
            check = Check("The biggest thing that builds a good credit score is…", listOf("Earning a lot", "Paying on time, every time", "Having many cards"), 1, "Repayment history matters most."),
            more = listOf(
                Check("Checking your own credit score…", listOf("Lowers it", "Does not lower it", "Costs a fine"), 1, "Looking at your own score is safe."),
                Check("Using all of your card limit every month is…", listOf("Good for your score", "Better kept low, ideally under a third", "Required"), 1, "Using less of the limit looks steadier to lenders."),
            ),
            applyLabel = "Put your card due date in Fixed costs.", apply = Apply.MONEY,
            rule = "Pay every bill on time and use less than a third of your card limit.",
            fact = "In India credit scores run from 300 to 900, and above about 750 is usually considered good.",
            takeaways = listOf("Scores run 300 to 900; on-time payment builds them.", "Keep card use low compared with the limit.", "Checking your own score is safe."),
        ),

        // First salary and tax
        Book(
            "payslip", Shelf.WORK, "Reading your first payslip", 5,
            idea = "CTC is what a job costs the company. Gross is what you earn before deductions. In-hand is what reaches your bank. Plan with in-hand.",
            situation = "Sneha's offer says ₹4.2 lakh CTC, so she expects ₹35,000 a month. Her first payslip shows about ₹31,000 in hand after PF and other deductions.",
            why = "Planning rent and goals on CTC leaves a gap every month. Reading the payslip once removes the surprise.",
            reflect = "What would you do differently if your monthly pay were ₹4,000 less than you expected?",
            tryIt = "On your next offer or payslip, find three numbers: CTC, gross pay and in-hand pay.",
            check = Check("To plan monthly spending, use…", listOf("CTC", "In-hand pay", "Your friend's salary"), 1, "In-hand pay is what you can actually spend and save."),
            more = listOf(
                Check("PF taken from your pay is…", listOf("Lost money", "Your savings, kept for later", "A fine"), 1, "It is saved for you, with your employer adding a share."),
                Check("Gross pay is…", listOf("Pay before deductions", "Pay after tax", "Your bonus"), 0, "Deductions come out of gross to give in-hand."),
            ),
            applyLabel = "Update your money plan in Ruvo to your in-hand pay.", apply = Apply.MONEY,
            rule = "Compare offers by in-hand pay, not CTC.",
            fact = "PF is usually 12% of basic pay from you, matched by your employer, and it is still your money.",
            takeaways = listOf("CTC, gross and in-hand are three different numbers.", "Plan your life on in-hand pay.", "PF is savings, not a cost."),
        ),
        Book(
            "pan", Shelf.WORK, "PAN, Form 16 and income tax", 5,
            idea = "Income tax is a share of yearly income paid to the government. Many students earn too little to owe any, but it still helps to know your PAN, your Form 16 and when a return is needed.",
            situation = "Arjun's first employer asks for his PAN. In May he gets a Form 16 showing salary and tax deducted, and a senior helps him check whether he should file a return.",
            why = "Tax feels scary only until you see it once. Keeping a few papers in one place makes it calm.",
            reflect = "Where would you keep your payslips and tax papers so you can find them in a year?",
            tryIt = "Make one folder, on your phone or on paper, for PAN, payslips, Form 16 and rent receipts.",
            check = Check("PAN is…", listOf("A bank PIN", "Your permanent tax ID", "A credit card"), 1, "It links your income and tax to you."),
            more = listOf(
                Check("Form 16 is given by…", listOf("Your employer", "Your college", "Your bank app"), 0, "It shows salary paid and tax deducted for the year."),
                Check("Tax rules and limits…", listOf("Never change", "Can change each year, so check the official portal", "Are the same everywhere"), 1, "Always check incometax.gov.in for the current year."),
            ),
            applyLabel = "Ask Ruvo to explain a tax word.", apply = Apply.ASK_RUVO,
            rule = "Keep every payslip, Form 16 and rent receipt in one folder.",
            fact = "Form 16 is the yearly certificate from your employer showing the salary paid and the tax deducted.",
            takeaways = listOf("PAN is your tax ID; keep it safe.", "Form 16 sums up your year's salary and tax.", "Rules change yearly; check the official portal."),
        ),
        Book(
            "freelance", Shelf.WORK, "Stipends, freelance and tax", 4,
            idea = "Freelance and some stipend payments may arrive with a little tax already cut, called TDS. Keeping records and a small amount aside makes tax time easy.",
            situation = "Meher designs posters for ₹5,000 a project. Some clients pay ₹4,500 and note TDS. She keeps every invoice and sets a little aside each time.",
            why = "Irregular income has no employer to sort tax for you. Records turn a scary task into a simple one.",
            reflect = "Do you know who has paid you this year, and how much?",
            tryIt = "Start a simple list: date, client, amount and any TDS for every payment.",
            check = Check("TDS means…", listOf("Tax taken before you are paid", "A type of bank account", "A discount"), 0, "Tax Deducted at Source is cut before the money reaches you."),
            more = listOf(
                Check("From freelance money, a calm habit is to…", listOf("Spend it all", "Set a little aside for tax", "Hide it"), 1, "A small share set aside avoids surprises."),
                Check("Tax already deducted in your name can be seen in…", listOf("Form 26AS or AIS on the income tax portal", "Your gallery", "Your UPI app"), 0, "The portal shows TDS linked to your PAN."),
            ),
            applyLabel = "Plan your month around your leanest income.", apply = Apply.MONEY,
            rule = "From every freelance payment, set a little aside for tax before you spend it.",
            fact = "Form 26AS and the AIS on the income tax portal show any tax already deducted in your name.",
            takeaways = listOf("TDS is tax cut before you are paid.", "Keep a list of every payment.", "Set a little aside each time."),
        ),

        // Investing, deeper
        Book(
            "inflation", Shelf.INVEST, "Inflation: why ₹100 buys less", 4,
            idea = "Prices rise a little most years. That means money kept still slowly buys less, which is why long-term money needs a plan, not just a drawer.",
            situation = "Nikhil's ₹20 cutting chai near college is ₹25 four years later. Nothing changed but time.",
            why = "Inflation is gentle each year and big over many. It is the quiet reason people learn about growing money at all.",
            reflect = "What did something you buy often cost three years ago?",
            tryIt = "Use the calculator to see what ₹10,000 buys in ten years at a few different rates.",
            check = Check("Inflation means…", listOf("Prices slowly rising", "Your bank adding money", "Prices falling"), 0, "The same rupee buys a little less each year."),
            more = listOf(
                Check("At about 6% inflation, prices roughly double in…", listOf("3 years", "12 years", "50 years"), 1, "By the rule of 72, 72 ÷ 6 is about 12 years."),
                Check("Money for a goal 10 years away should…", listOf("Sit still in cash", "Have a plan that thinks about inflation", "Be spent now"), 1, "Long horizons need a plan."),
            ),
            applyLabel = "Ask Ruvo how inflation affects a goal.", apply = Apply.ASK_RUVO,
            rule = "Short-term money stays safe; long-term money needs a plan that thinks about rising prices.",
            fact = "At 6% a year, prices roughly double in about 12 years.",
            takeaways = listOf("Prices rise a little most years.", "Money kept still slowly buys less.", "Long-term goals need a plan, not a drawer."),
            tool = Tools.Tool.INFLATION,
        ),
        Book(
            "compound", Shelf.INVEST, "Compounding, the slow superpower", 5,
            idea = "Compounding is growth on growth: what you earn starts earning too. It is slow at first and surprising later, which is why starting early matters more than starting big.",
            situation = "Two friends save ₹500 a month. One starts at 20, one at 27. Even with the same rate, the early starter ends up far ahead, mostly because of time.",
            why = "Time is the one thing students have plenty of. Compounding turns time into money.",
            reflect = "What small amount could you set aside every month without missing it?",
            tryIt = "Use the calculator: try ₹500 a month for 3, 5 and 10 years and watch the gap grow.",
            check = Check("Compounding means…", listOf("Earning on your earnings too", "Paying fees", "A one-time bonus"), 0, "Growth builds on growth."),
            more = listOf(
                Check("For compounding, what helps most?", listOf("Starting early", "Checking daily", "Big one-time amounts only"), 0, "Time does most of the work."),
                Check("The rule of 72 estimates…", listOf("Years to double", "Your tax", "Your credit score"), 0, "72 divided by a yearly rate gives rough years to double."),
            ),
            applyLabel = "Start a small monthly goal.", apply = Apply.NEW_GOAL,
            rule = "Starting early matters more than starting big.",
            fact = "The rule of 72: divide 72 by a yearly growth rate to see roughly how many years money takes to double.",
            takeaways = listOf("Growth earns growth over time.", "Early and small beats late and big.", "Rough doubling time is 72 divided by the rate."),
            tool = Tools.Tool.GROWTH,
        ),
        Book(
            "options", Shelf.INVEST, "FD, RD, mutual funds and SIPs, explained", 6,
            idea = "These are different places to keep money, from very steady to up-and-down. A fixed deposit and a recurring deposit pay agreed bank interest. A mutual fund pools money and its value moves. A SIP is simply a way to put in a fixed amount every month.",
            situation = "Ananya hears four words at once from relatives. Sorting them by \"how steady\" and \"how long\" makes them clear: steady places for soon, moving ones only for years away.",
            why = "Knowing what each one is protects you from pressure and lets you ask good questions. Choosing is a separate step, and it can wait.",
            reflect = "Which of your money do you need within a year, and which could wait five years or more?",
            tryIt = "For each of your goals, write \"soon\" or \"years away\" next to it.",
            check = Check("A SIP is…", listOf("A kind of bank", "A way to put in a fixed amount regularly", "A guarantee of growth"), 1, "It is a method of investing monthly, not a product with promised results."),
            more = listOf(
                Check("A mutual fund's value…", listOf("Never changes", "Can go up and down", "Is fixed by the bank"), 1, "It moves with what it holds; past results never promise future ones."),
                Check("Money needed in six months is best kept…", listOf("Somewhere steady", "In something that swings a lot", "Nowhere"), 0, "Match the place to the time."),
            ),
            applyLabel = "Ask Ruvo to explain any of these words.", apply = Apply.ASK_RUVO,
            rule = "Match the place to the time: money needed soon stays steady; only long-term money can ride ups and downs.",
            fact = "Bank deposits in India are insured by DICGC up to ₹5 lakh per depositor per bank.",
            takeaways = listOf("FD and RD pay agreed bank interest.", "Mutual fund values move up and down.", "A SIP is a monthly habit, not a promise."),
        ),
    )

    /** Extras for the original books: rules of thumb, facts, calculators and stories. */
    fun enrich(b: Book): Book = when (b.id) {
        "small" -> b.copy(tool = Tools.Tool.SMALL_SPENDS, rule = "Count a habit by the year, not by the day.", fact = "₹100 a day is ₹36,500 a year.")
        "pause" -> b.copy(story = "concert", rule = "If you still want it tomorrow, it is a real want.")
        "first" -> b.copy(story = "stipend", rule = "Move money to your goals on the day money arrives.")
        "subs" -> b.copy(rule = "If you would not sign up again today, cancel it.")
        "balance" -> b.copy(rule = "Spend from Fuel, never from your balance.")
        "503020" -> b.copy(fact = "The 50/30/20 idea was made popular by the 2005 book All Your Worth.")
        "cushion" -> b.copy(rule = "Aim for one month of basic costs, built a little at a time.")
        "emergency" -> b.copy(rule = "Cushion first, investing later.")
        "invest" -> b.copy(tool = Tools.Tool.GROWTH, fact = "No real investment can promise high returns with no risk.")
        "saveinvest" -> b.copy(tool = Tools.Tool.INFLATION, rule = "Save for soon, invest only for years away.")
        "risk" -> b.copy(rule = "Never invest money you will need within a year or two.")
        "split" -> b.copy(rule = "Settle splits within a week, while everyone remembers.")
        "discounts" -> b.copy(rule = "A discount only saves money if you were going to buy it anyway.")
        else -> b
    }
}
