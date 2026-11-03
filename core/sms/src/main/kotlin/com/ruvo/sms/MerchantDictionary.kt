package com.ruvo.sms

/**
 * Turns raw SMS counterparties ("swiggy@icici", "UPI/P2M/628.../ZOMATO LTD", "BOOKMYSHOW") into a clean
 * name and a category. Starts from a built-in list and learns from the user's corrections.
 */
class MerchantDictionary(initialOverrides: Map<String, Entry> = emptyMap()) {

    data class Entry(val name: String, val category: String)

    private val overrides = initialOverrides.toMutableMap()

    /** Corrections the user has made, keyed by the normalised raw counterparty. Persist these. */
    val learned: Map<String, Entry> get() = overrides.toMap()

    fun resolve(raw: String): Entry {
        val key = normalise(raw)
        overrides[key]?.let { return it }
        val compact = key.replace(" ", "")
        for ((needle, entry) in BUILT_IN) {
            val hit = if (needle.startsWith("^")) compact.startsWith(needle.drop(1)) else compact.contains(needle)
            if (hit) return entry
        }
        return Entry(prettify(raw), "Other")
    }

    /** Remember a correction so the same counterparty is named and filed this way from now on. */
    fun learn(raw: String, name: String, category: String) {
        overrides[normalise(raw)] = Entry(name, category)
    }

    companion object {
        /** "swiggy@icici" -> "swiggy", "MR RAJESH SHARMA" -> "mr rajesh sharma". */
        fun normalise(raw: String): String {
            var s = raw.trim().lowercase()
            if ('@' in s) s = s.substringBefore('@')
            s = s.replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
            return s
        }

        /** Title-case a raw counterparty for display, dropping VPA handles, digits and company suffixes. */
        fun prettify(raw: String): String {
            var s = raw.trim()
            if ('@' in s) s = s.substringBefore('@').replace(Regex("[._\\-]"), " ").replace(Regex("\\d+$"), "")
            s = s.replace(Regex("(?i)\\b(pvt|private|ltd|limited|llp|india|technologies|tech|services|payments?)\\b\\.?"), " ")
            s = s.replace(Regex("(?i)^(mr|mrs|ms|dr)\\.?\\s+"), "")
            s = s.replace(Regex("\\s+"), " ").trim()
            if (s.isEmpty()) return raw.trim()
            return s.split(' ').joinToString(" ") { w ->
                if (w.length <= 3 && w.all { it.isUpperCase() } && w != "MR") w
                else w.lowercase().replaceFirstChar { it.uppercase() }
            }
        }

        /** Checked in order: longer, more specific keys first. Lowercase, no spaces; a leading ^ means "starts with". */
        val BUILT_IN: List<Pair<String, Entry>> = listOf(
            "mumbaimetro" to Entry("Mumbai Metro", "Transport"),
            "delhimetro" to Entry("Delhi Metro", "Transport"),
            "dmrc" to Entry("Delhi Metro", "Transport"),
            "bmrcl" to Entry("Namma Metro", "Transport"),
            "irctc" to Entry("IRCTC", "Transport"),
            "uber" to Entry("Uber", "Transport"),
            "olacabs" to Entry("Ola", "Transport"),
            "^ola" to Entry("Ola", "Transport"),
            "rapido" to Entry("Rapido", "Transport"),
            "redbus" to Entry("redBus", "Transport"),
            "fastag" to Entry("FASTag", "Transport"),
            "swiggy" to Entry("Swiggy", "Food"),
            "zomato" to Entry("Zomato", "Food"),
            "dominos" to Entry("Domino's", "Food"),
            "jubilantfood" to Entry("Domino's", "Food"),
            "mcdonald" to Entry("McDonald's", "Food"),
            "starbucks" to Entry("Starbucks", "Food"),
            "thirdwave" to Entry("Third Wave Coffee", "Food"),
            "chaipoint" to Entry("Chai Point", "Food"),
            "chaayos" to Entry("Chaayos", "Food"),
            "canteen" to Entry("Canteen", "Food"),
            "zepto" to Entry("Zepto", "Groceries"),
            "blinkit" to Entry("Blinkit", "Groceries"),
            "grofers" to Entry("Blinkit", "Groceries"),
            "bigbasket" to Entry("BigBasket", "Groceries"),
            "instamart" to Entry("Swiggy Instamart", "Groceries"),
            "dmart" to Entry("DMart", "Groceries"),
            "avenuesuper" to Entry("DMart", "Groceries"),
            "amazon" to Entry("Amazon", "Shopping"),
            "amzn" to Entry("Amazon", "Shopping"),
            "flipkart" to Entry("Flipkart", "Shopping"),
            "myntra" to Entry("Myntra", "Shopping"),
            "ajio" to Entry("AJIO", "Shopping"),
            "meesho" to Entry("Meesho", "Shopping"),
            "nykaa" to Entry("Nykaa", "Shopping"),
            "decathlon" to Entry("Decathlon", "Shopping"),
            "croma" to Entry("Croma", "Shopping"),
            "bookmyshow" to Entry("BookMyShow", "Entertainment"),
            "pvr" to Entry("PVR INOX", "Entertainment"),
            "inox" to Entry("PVR INOX", "Entertainment"),
            "netflix" to Entry("Netflix", "Subscriptions"),
            "spotify" to Entry("Spotify", "Subscriptions"),
            "hotstar" to Entry("JioHotstar", "Subscriptions"),
            "youtube" to Entry("YouTube Premium", "Subscriptions"),
            "primevideo" to Entry("Prime Video", "Subscriptions"),
            "steam" to Entry("Steam", "Entertainment"),
            "jiomart" to Entry("JioMart", "Groceries"),
            "^jio" to Entry("Jio", "Bills"),
            "airtel" to Entry("Airtel", "Bills"),
            "vodafone" to Entry("Vi", "Bills"),
            "bsnl" to Entry("BSNL", "Bills"),
            "tatapower" to Entry("Tata Power", "Bills"),
            "adanielectric" to Entry("Adani Electricity", "Bills"),
            "bescom" to Entry("BESCOM", "Bills"),
            "msedcl" to Entry("Mahavitaran", "Bills"),
            "apollo" to Entry("Apollo Pharmacy", "Health"),
            "pharmeasy" to Entry("PharmEasy", "Health"),
            "netmeds" to Entry("Netmeds", "Health"),
            "1mg" to Entry("Tata 1mg", "Health"),
            "coursera" to Entry("Coursera", "Education"),
            "udemy" to Entry("Udemy", "Education"),
            "unacademy" to Entry("Unacademy", "Education"),
            "university" to Entry("University fees", "Education"),
            "college" to Entry("College", "Education"),
            "xerox" to Entry("Xerox and stationery", "Education"),
            "stationery" to Entry("Xerox and stationery", "Education"),
        )
    }
}
