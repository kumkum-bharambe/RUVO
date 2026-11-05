package com.ruvo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ruvo.engine.Cadence
import com.ruvo.engine.SpendBasis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "ruvo_settings")

/** Theme names mirror com.ruvo.design.ThemeChoice; kept as strings so data does not depend on design. */
data class UserSettings(
    val onboarded: Boolean = false,
    val ageConfirmed: Boolean = false,
    val name: String = "",
    /** Persona id when showing demo data, null when using the user's own data. */
    val demoPersona: String? = "aarav",
    val theme: String = "SYSTEM",
    val spendBasis: SpendBasis = SpendBasis.AUTO,
    val cadence: Cadence = Cadence.MONTHLY,
    val expectedIncome: Long = 0,
    val nextIncomeEpochDay: Long? = null,
    val cycleDays: Int = 30,
    /** Balance at a point in time; transactions recorded after it move the balance. */
    val anchorAmount: Long = 0,
    val anchorMillis: Long = 0,
    /** Read the real SMS inbox. Off by default; demo SMS work without it. */
    val realSms: Boolean = false,
    /** Accept test SMS from any sender (emulator and demo). */
    val demoSms: Boolean = true,
    val spokenReplies: Boolean = false,
    val upiVpa: String = "",
    val sortDay: Int = 7,
    val sortHour: Int = 19,
    /** Guide books the user has finished. */
    val readBooks: Set<String> = emptySet(),
    /** Ideas saved from a book or an insight, as "bookId:page". */
    val savedIdeas: Set<String> = emptySet(),
    /** Marketplace offers the user saved, by listing id. */
    val savedOffers: Set<String> = emptySet(),
    /** Weekly review answers, as "txnId:WORTH|NOT|UNSURE". */
    val purchaseReviews: Set<String> = emptySet(),
    /** Shows •••• instead of the balance on Money. */
    val hideBalance: Boolean = false,
    /** Calm, occasional notifications. Each can be switched off. */
    val notifyCapture: Boolean = true,
    val notifyWeekly: Boolean = true,
    val notifyGoals: Boolean = true,
    /** History corrections, as "2026-09:receivedPaise:spentPaise". Display only; never move the runway. */
    val monthEdits: Set<String> = emptySet(),
    /** Chat language for dictation and spoken replies, as a BCP-47 tag such as "en-IN" or "hi-IN". */
    val chatLanguage: String = "en-IN",
    /** The on-phone account: who signed up, their mobile number, and a salted hash of their PIN (never the PIN). */
    val accountName: String = "",
    val accountPhone: String = "",
    val pinHash: String = "",
    /** Wrong PINs in a row, and when tries may start again after five. */
    val pinFails: Int = 0,
    val pinPausedUntil: Long = 0,
) {
    val hasAccount: Boolean get() = pinHash.isNotEmpty()

    fun monthEditMap(): Map<java.time.YearMonth, Pair<Long, Long>> = monthEdits.mapNotNull { e ->
        val p = e.split(":")
        runCatching { java.time.YearMonth.parse(p[0]) to (p[1].toLong() to p[2].toLong()) }.getOrNull()
    }.toMap()

    fun reviewOf(txnId: String): String? = purchaseReviews.firstOrNull { it.startsWith("$txnId:") }?.substringAfter(":")
}

class SettingsStore(private val context: Context) {

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val age = booleanPreferencesKey("age_confirmed")
        val name = stringPreferencesKey("name")
        val persona = stringPreferencesKey("demo_persona")
        val theme = stringPreferencesKey("theme")
        val basis = stringPreferencesKey("spend_basis")
        val cadence = stringPreferencesKey("cadence")
        val expected = longPreferencesKey("expected_income")
        val next = longPreferencesKey("next_income_day")
        val cycle = intPreferencesKey("cycle_days")
        val anchor = longPreferencesKey("anchor_amount")
        val anchorAt = longPreferencesKey("anchor_millis")
        val realSms = booleanPreferencesKey("real_sms")
        val demoSms = booleanPreferencesKey("demo_sms")
        val spoken = booleanPreferencesKey("spoken_replies")
        val vpa = stringPreferencesKey("upi_vpa")
        val read = stringSetPreferencesKey("read_books")
        val ideas = stringSetPreferencesKey("saved_ideas")
        val offers = stringSetPreferencesKey("saved_offers")
        val reviews = stringSetPreferencesKey("purchase_reviews")
        val hide = booleanPreferencesKey("hide_balance")
        val nCapture = booleanPreferencesKey("notify_capture")
        val nWeekly = booleanPreferencesKey("notify_weekly")
        val nGoals = booleanPreferencesKey("notify_goals")
        val edits = stringSetPreferencesKey("month_edits")
        val lang = stringPreferencesKey("chat_language")
        val accName = stringPreferencesKey("account_name")
        val accPhone = stringPreferencesKey("account_phone")
        val pin = stringPreferencesKey("pin_hash")
        val pinFails = intPreferencesKey("pin_fails")
        val pinPaused = longPreferencesKey("pin_paused_until")
    }

    val settings: Flow<UserSettings> = context.store.data.map { readFrom(it) }

    suspend fun current(): UserSettings = settings.first()

    suspend fun update(block: (UserSettings) -> UserSettings) {
        context.store.edit { p ->
            val s = block(readFrom(p))
            p[K.onboarded] = s.onboarded
            p[K.age] = s.ageConfirmed
            p[K.name] = s.name
            p[K.persona] = s.demoPersona ?: ""
            p[K.theme] = s.theme
            p[K.basis] = s.spendBasis.name
            p[K.cadence] = s.cadence.name
            p[K.expected] = s.expectedIncome
            if (s.nextIncomeEpochDay != null) p[K.next] = s.nextIncomeEpochDay else p.remove(K.next)
            p[K.cycle] = s.cycleDays
            p[K.anchor] = s.anchorAmount
            p[K.anchorAt] = s.anchorMillis
            p[K.realSms] = s.realSms
            p[K.demoSms] = s.demoSms
            p[K.spoken] = s.spokenReplies
            p[K.vpa] = s.upiVpa
            p[K.read] = s.readBooks
            p[K.ideas] = s.savedIdeas
            p[K.offers] = s.savedOffers
            p[K.reviews] = s.purchaseReviews
            p[K.hide] = s.hideBalance
            p[K.nCapture] = s.notifyCapture
            p[K.nWeekly] = s.notifyWeekly
            p[K.nGoals] = s.notifyGoals
            p[K.edits] = s.monthEdits
            p[K.lang] = s.chatLanguage
            p[K.accName] = s.accountName
            p[K.accPhone] = s.accountPhone
            p[K.pin] = s.pinHash
            p[K.pinFails] = s.pinFails
            p[K.pinPaused] = s.pinPausedUntil
        }
    }

    private fun readFrom(p: Preferences) = UserSettings(
        onboarded = p[K.onboarded] ?: false,
        ageConfirmed = p[K.age] ?: false,
        name = p[K.name] ?: "",
        demoPersona = if (p.contains(K.persona)) p[K.persona]?.ifEmpty { null } else "aarav",
        theme = p[K.theme] ?: "SYSTEM",
        spendBasis = p[K.basis]?.let { runCatching { SpendBasis.valueOf(it) }.getOrNull() } ?: SpendBasis.AUTO,
        cadence = p[K.cadence]?.let { runCatching { Cadence.valueOf(it) }.getOrNull() } ?: Cadence.MONTHLY,
        expectedIncome = p[K.expected] ?: 0,
        nextIncomeEpochDay = p[K.next],
        cycleDays = p[K.cycle] ?: 30,
        anchorAmount = p[K.anchor] ?: 0,
        anchorMillis = p[K.anchorAt] ?: 0,
        realSms = p[K.realSms] ?: false,
        demoSms = p[K.demoSms] ?: true,
        spokenReplies = p[K.spoken] ?: false,
        upiVpa = p[K.vpa] ?: "",
        readBooks = p[K.read] ?: emptySet(),
        savedIdeas = p[K.ideas] ?: emptySet(),
        savedOffers = p[K.offers] ?: emptySet(),
        purchaseReviews = p[K.reviews] ?: emptySet(),
        hideBalance = p[K.hide] ?: false,
        notifyCapture = p[K.nCapture] ?: true,
        notifyWeekly = p[K.nWeekly] ?: true,
        notifyGoals = p[K.nGoals] ?: true,
        monthEdits = p[K.edits] ?: emptySet(),
        chatLanguage = p[K.lang] ?: "en-IN",
        accountName = p[K.accName] ?: "",
        accountPhone = p[K.accPhone] ?: "",
        pinHash = p[K.pin] ?: "",
        pinFails = p[K.pinFails] ?: 0,
        pinPausedUntil = p[K.pinPaused] ?: 0,
    )
}
