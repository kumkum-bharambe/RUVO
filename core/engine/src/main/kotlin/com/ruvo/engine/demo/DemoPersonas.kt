package com.ruvo.engine.demo

import com.ruvo.engine.Bucket
import com.ruvo.engine.Cadence
import com.ruvo.engine.Commitment
import com.ruvo.engine.Direction
import com.ruvo.engine.Goal
import com.ruvo.engine.IncomePlan
import com.ruvo.engine.RunwayInput
import com.ruvo.engine.Txn
import com.ruvo.engine.TxnKind
import com.ruvo.engine.TxnStatus
import com.ruvo.engine.rupees
import java.time.LocalDate

/**
 * Five sample students for demos. Every date is relative to [today], so the demo always looks current.
 * None of this is real data.
 */
data class Persona(
    val id: String,
    val name: String,
    val age: Int,
    val livesIn: String,
    val story: String,
    val input: RunwayInput,
)

object DemoPersonas {

    fun all(today: LocalDate): List<Persona> = listOf(
        aarav(today), meera(today), kabir(today), riya(today), dev(today),
    )

    fun byId(id: String, today: LocalDate): Persona = all(today).firstOrNull { it.id == id } ?: aarav(today)

    /** Monthly allowance, hostel. Tends to run out in the last week. */
    fun aarav(today: LocalDate): Persona {
        val next = nextDayOfMonth(today, 1)
        val tx = spends(
            today, "a",
            listOf(
                S(0, 180, "Swiggy", "Food"), S(0, 40, "Campus canteen", "Food"),
                S(1, 60, "Mumbai Metro", "Transport"), S(1, 320, "Zomato", "Food"),
                S(2, 149, "Spotify", "Entertainment", Bucket.FIXED), S(3, 90, "Rapido", "Transport"),
                S(4, 250, "Blinkit", "Groceries"), S(5, 120, "Chai point", "Food"),
                S(6, 499, "Myntra", "Shopping"), S(7, 70, "Campus canteen", "Food"),
                S(8, 210, "Swiggy", "Food"), S(9, 60, "Mumbai Metro", "Transport"),
                S(10, 350, "PVR Cinemas", "Entertainment"), S(12, 140, "Xerox and stationery", "Education"),
            ),
        ) + income("a-in", 12_000, next.minusMonths(1), "Pocket money from home")
        return Persona(
            "aarav", "Aarav", 19, "Hostel",
            "Monthly allowance from family. Usually runs low in the last week.",
            RunwayInput(
                today = today,
                balance = rupees(6_840),
                income = IncomePlan(Cadence.MONTHLY, rupees(12_000), next),
                commitments = listOf(
                    Commitment("a-mess", "Hostel mess", rupees(3_200), dueDayOfMonth = 5, lastPaidOn = next.minusMonths(1).plusDays(3)),
                    Commitment("a-rech", "Phone recharge", rupees(299), dueDayOfMonth = 20),
                    Commitment("a-spot", "Spotify", rupees(149), dueDayOfMonth = today.minusDays(2).dayOfMonth, lastPaidOn = today.minusDays(2)),
                ),
                goals = listOf(Goal("a-g1", "Goa trip", rupees(8_000), rupees(2_000), today.plusMonths(4), rupees(1_000))),
                transactions = tx,
            ),
        )
    }

    /** Internship stipend plus a family top-up, shared flat. Rent and who-owes-whom. */
    fun meera(today: LocalDate): Persona {
        val next = nextDayOfMonth(today, 7)
        val tx = spends(
            today, "m",
            listOf(
                S(0, 640, "Zepto", "Groceries", others = 320), S(1, 220, "Uber", "Transport"),
                S(2, 899, "Bigbasket", "Groceries", others = 450), S(3, 160, "Third Wave Coffee", "Food"),
                S(4, 1_200, "Decathlon", "Shopping"), S(6, 380, "Swiggy", "Food"),
                S(7, 90, "Mumbai Metro", "Transport"), S(9, 540, "Airtel broadband", "Bills", Bucket.FIXED, others = 270),
                S(10, 260, "Zomato", "Food"), S(11, 75, "Auto rickshaw", "Transport"),
            ),
        ) + income("m-in", 18_000, next.minusMonths(1), "Internship stipend") +
            income("m-in2", 5_000, next.minusMonths(1).plusDays(2), "Family top-up")
        return Persona(
            "meera", "Meera", 22, "Shared flat",
            "Internship stipend plus a family top-up. Splits rent and groceries with two flatmates.",
            RunwayInput(
                today = today,
                balance = rupees(14_250),
                income = IncomePlan(Cadence.MONTHLY, rupees(23_000), next),
                commitments = listOf(
                    Commitment("m-rent", "Rent share", rupees(9_000), dueDayOfMonth = 10),
                    Commitment("m-elec", "Electricity share", rupees(700), dueDayOfMonth = 15),
                    Commitment("m-net", "Broadband share", rupees(270), dueDayOfMonth = today.minusDays(9).dayOfMonth, lastPaidOn = today.minusDays(9)),
                ),
                goals = listOf(
                    Goal("m-g1", "Laptop", rupees(55_000), rupees(12_500), today.plusMonths(9), rupees(3_000)),
                    Goal("m-g2", "Emergency fund", rupees(10_000), rupees(4_000), null, rupees(1_000)),
                ),
                transactions = tx,
            ),
        )
    }

    /** One scholarship payment per semester. The risk is spending too much early. */
    fun kabir(today: LocalDate): Persona {
        val next = today.plusDays(74)
        val tx = spends(
            today, "k",
            listOf(
                S(0, 420, "Swiggy", "Food"), S(1, 2_400, "Amazon", "Shopping"), S(2, 150, "Ola", "Transport"),
                S(3, 600, "Steam", "Entertainment"), S(5, 310, "Dominos", "Food"), S(6, 1_800, "Croma", "Shopping"),
                S(8, 220, "Campus canteen", "Food"), S(9, 90, "Rapido", "Transport"), S(11, 480, "BookMyShow", "Entertainment"),
            ),
        ) + income("k-in", 60_000, today.minusDays(46), "Merit scholarship")
        return Persona(
            "kabir", "Kabir", 21, "Hostel",
            "A merit scholarship once a semester. Needs one big amount to last four months.",
            RunwayInput(
                today = today,
                balance = rupees(38_600),
                income = IncomePlan(Cadence.LUMP_SUM, rupees(60_000), next, cycleDays = 120),
                commitments = listOf(
                    Commitment("k-hostel", "Hostel and mess", rupees(5_500), dueDayOfMonth = 3),
                    Commitment("k-rech", "Phone recharge", rupees(349), dueDayOfMonth = 12),
                ),
                goals = listOf(Goal("k-g1", "Next semester fees buffer", rupees(15_000), rupees(5_000), next, rupees(2_500))),
                transactions = tx,
            ),
        )
    }

    /** Freelance design gigs and a weekend café shift. Money arrives whenever it arrives. */
    fun riya(today: LocalDate): Persona {
        val tx = spends(
            today, "r",
            listOf(
                S(0, 130, "Chai point", "Food"), S(1, 999, "Figma course", "Education"), S(2, 240, "Swiggy", "Food"),
                S(4, 60, "Bus pass top-up", "Transport"), S(5, 449, "Nykaa", "Shopping"), S(7, 180, "Zomato", "Food"),
                S(8, 2_100, "PG laundry and food", "Bills", Bucket.FIXED), S(10, 95, "Auto rickshaw", "Transport"),
            ),
        ) + income("r-in1", 4_500, today.minusDays(3), "Logo project") +
            income("r-in2", 2_800, today.minusDays(11), "Café shifts")
        return Persona(
            "riya", "Riya", 20, "PG",
            "Freelance gigs and weekend café shifts. No fixed payday.",
            RunwayInput(
                today = today,
                balance = rupees(9_300),
                income = IncomePlan(Cadence.IRREGULAR, 0, null),
                commitments = listOf(Commitment("r-pg", "PG rent", rupees(6_500), dueDayOfMonth = 5)),
                goals = listOf(Goal("r-g1", "Drawing tablet", rupees(7_500), rupees(1_500), today.plusMonths(3), rupees(1_000))),
                transactions = tx,
            ),
        )
    }

    /** Lives with family, small monthly pocket money. Lowest need, still wants control. */
    fun dev(today: LocalDate): Persona {
        val next = nextDayOfMonth(today, 1)
        val tx = spends(
            today, "d",
            listOf(
                S(0, 50, "Vada pav stall", "Food"), S(1, 30, "Local train", "Transport"), S(3, 199, "BGMI UC", "Entertainment"),
                S(5, 120, "Cafe with friends", "Food"), S(8, 30, "Local train", "Transport"),
            ),
        ) + income("d-in", 4_000, next.minusMonths(1), "Pocket money")
        return Persona(
            "dev", "Dev", 18, "With family",
            "Lives at home with a small monthly pocket money. Saving for a phone.",
            RunwayInput(
                today = today,
                balance = rupees(2_650),
                income = IncomePlan(Cadence.MONTHLY, rupees(4_000), next),
                commitments = listOf(Commitment("d-rech", "Phone recharge", rupees(239), dueDayOfMonth = 18)),
                goals = listOf(Goal("d-g1", "New phone", rupees(18_000), rupees(3_200), today.plusMonths(8), rupees(1_000))),
                transactions = tx,
            ),
        )
    }

    private data class S(
        val daysAgo: Int,
        val amount: Int,
        val merchant: String,
        val category: String,
        val bucket: Bucket = Bucket.FUEL,
        val others: Int = 0,
    )

    private fun spends(today: LocalDate, prefix: String, list: List<S>): List<Txn> = list.mapIndexed { i, s ->
        Txn(
            id = "$prefix-$i",
            amount = rupees(s.amount.toLong()),
            direction = Direction.DEBIT,
            date = today.minusDays(s.daysAgo.toLong()),
            kind = TxnKind.SPEND,
            bucket = s.bucket,
            status = if (s.daysAgo <= 1) TxnStatus.TO_SORT else TxnStatus.CONFIRMED,
            category = s.category,
            merchant = s.merchant,
            othersShare = rupees(s.others.toLong()),
        )
    }

    private fun income(id: String, amount: Int, date: LocalDate, label: String) = listOf(
        Txn(id, rupees(amount.toLong()), Direction.CREDIT, date, TxnKind.INCOME, Bucket.FUEL, TxnStatus.CONFIRMED, "Income", label),
    )

    /** The next date with this day of month that is after today. */
    fun nextDayOfMonth(today: LocalDate, day: Int): LocalDate {
        val thisMonth = today.withDayOfMonth(day.coerceAtMost(today.lengthOfMonth()))
        return if (thisMonth.isAfter(today)) thisMonth
        else today.plusMonths(1).let { it.withDayOfMonth(day.coerceAtMost(it.lengthOfMonth())) }
    }
}
