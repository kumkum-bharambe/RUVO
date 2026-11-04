package com.ruvo.data

import com.ruvo.data.db.CommitmentEntity
import com.ruvo.data.db.GoalEntity
import com.ruvo.data.db.MerchantLearnedEntity
import com.ruvo.data.db.RuvoDao
import com.ruvo.data.db.SnapshotEntity
import com.ruvo.data.db.Source
import com.ruvo.data.db.SplitShareEntity
import com.ruvo.data.db.TxnEntity
import com.ruvo.engine.Bucket
import com.ruvo.engine.Cadence
import com.ruvo.engine.Commitment
import com.ruvo.engine.Direction
import com.ruvo.engine.Goal
import com.ruvo.engine.IncomePlan
import com.ruvo.engine.IndianFormat
import com.ruvo.engine.Paise
import com.ruvo.engine.RunwayEngine
import com.ruvo.engine.RunwayInput
import com.ruvo.engine.RunwayResult
import com.ruvo.engine.SpendBasis
import com.ruvo.engine.Splits
import com.ruvo.engine.Txn
import com.ruvo.engine.TxnKind
import com.ruvo.engine.TxnStatus
import com.ruvo.engine.demo.DemoPersonas
import com.ruvo.engine.demo.Persona
import com.ruvo.sms.Deduper
import com.ruvo.sms.MerchantDictionary
import com.ruvo.sms.ParsedTxn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class ActivityItem(
    val txn: Txn,
    val source: Source,
    val shares: List<SplitShareEntity>,
    val note: String,
)

data class OwedItem(val shareId: Long, val person: String, val amount: Paise, val merchant: String, val date: LocalDate)

/** Everything the screens need, recalculated whenever any stored value changes. */
data class AppState(
    val ready: Boolean,
    val settings: UserSettings,
    val today: LocalDate,
    val input: RunwayInput,
    val runway: RunwayResult,
    val activity: List<ActivityItem>,
    val toSort: List<ActivityItem>,
    val goals: List<Goal>,
    val goalShapes: Map<String, Int>,
    val commitments: List<Commitment>,
    val owed: List<OwedItem>,
    val persona: Persona?,
) {
    val owedToYou: Paise get() = owed.sumOf { it.amount }
    val isDemo: Boolean get() = settings.demoPersona != null
    val displayName: String get() = persona?.name ?: settings.name.ifBlank { "there" }

    companion object {
        fun empty(today: LocalDate): AppState {
            val input = RunwayInput(today, 0, IncomePlan(Cadence.MONTHLY, 0, null))
            return AppState(false, UserSettings(), today, input, RunwayEngine.calculate(input), emptyList(), emptyList(), emptyList(), emptyMap(), emptyList(), emptyList(), null)
        }
    }
}

/** Answers from the 90-second first run. */
data class OnboardingAnswers(
    val name: String,
    val cadence: Cadence,
    val balance: Paise,
    val expectedIncome: Paise,
    val nextIncome: LocalDate?,
    val commitments: List<Commitment>,
)

class RuvoRepository(
    private val dao: RuvoDao,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val todayFlow = MutableStateFlow(LocalDate.now(zone))

    /** Call when the app comes to the foreground, so a new day is picked up. */
    fun refreshDay() {
        todayFlow.value = LocalDate.now(zone)
    }

    val state: StateFlow<AppState> = combine(
        settingsStore.settings, dao.txns(), dao.commitments(), dao.goals(), dao.shares(),
    ) { s, txns, commitments, goals, shares -> listOf(s, txns, commitments, goals, shares) }
        .combine(todayFlow) { parts, today ->
            @Suppress("UNCHECKED_CAST")
            build(
                parts[0] as UserSettings, parts[1] as List<TxnEntity>, parts[2] as List<CommitmentEntity>,
                parts[3] as List<GoalEntity>, parts[4] as List<SplitShareEntity>, today,
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AppState.empty(LocalDate.now(zone)))

    private fun build(
        s: UserSettings,
        txnRows: List<TxnEntity>,
        commitmentRows: List<CommitmentEntity>,
        goalRows: List<GoalEntity>,
        shareRows: List<SplitShareEntity>,
        today: LocalDate,
    ): AppState {
        val sharesByTxn = shareRows.groupBy { it.txnId }
        val txns = txnRows.map { it.toEngine(sharesByTxn[it.id]) }
        val balance = s.anchorAmount + txnRows
            .filter { it.createdAtMillis > s.anchorMillis && it.status != TxnStatus.IGNORED.name }
            .sumOf { if (it.direction == Direction.CREDIT.name) it.amount else -it.amount }
        val commitments = commitmentRows.map { it.toEngine() }
        val goals = goalRows.map { it.toEngine() }
        val income = IncomePlan(s.cadence, s.expectedIncome, s.nextIncomeEpochDay?.let(LocalDate::ofEpochDay), s.cycleDays)
        val input = RunwayInput(today, balance, income, commitments, goals, txns, s.spendBasis)
        val result = RunwayEngine.calculate(input)
        val activity = txnRows.zip(txns).map { (row, t) ->
            ActivityItem(t, runCatching { Source.valueOf(row.source) }.getOrDefault(Source.MANUAL), sharesByTxn[row.id].orEmpty(), row.note)
        }
        val owed = shareRows.filter { !it.paid }.mapNotNull { sh ->
            val t = txnRows.firstOrNull { it.id == sh.txnId } ?: return@mapNotNull null
            OwedItem(sh.id, sh.person, sh.amount, t.merchant, LocalDate.ofEpochDay(t.dateEpochDay))
        }
        val persona = s.demoPersona?.let { DemoPersonas.byId(it, today) }
        return AppState(
            ready = true, settings = s, today = today, input = input, runway = result,
            activity = activity, toSort = activity.filter { it.txn.status == TxnStatus.TO_SORT },
            goals = goals, goalShapes = goalRows.associate { it.id to it.jarShape },
            commitments = commitments, owed = owed, persona = persona,
        )
    }

    // --- Capture ---

    suspend fun addManual(
        amount: Paise,
        merchant: String,
        category: String,
        bucket: Bucket = Bucket.FUEL,
        direction: Direction = Direction.DEBIT,
        date: LocalDate = todayFlow.value,
        note: String = "",
    ): String {
        val id = newId()
        dao.upsertTxn(
            TxnEntity(
                id = id, amount = amount, direction = direction.name, dateEpochDay = date.toEpochDay(),
                kind = (if (direction == Direction.CREDIT) TxnKind.INCOME else TxnKind.SPEND).name,
                bucket = bucket.name, status = TxnStatus.CONFIRMED.name, category = category,
                merchant = merchant.ifBlank { category }, source = Source.MANUAL.name, note = note, createdAtMillis = clock(),
            ),
        )
        return id
    }

    /** Stores parsed SMS as "to sort", skipping any that duplicate a recent transaction. Returns the new ones. */
    suspend fun importSms(parsed: List<ParsedTxn>): List<TxnEntity> {
        if (parsed.isEmpty()) return emptyList()
        val learned = dao.learned().associate { it.key to MerchantDictionary.Entry(it.name, it.category) }
        val dict = MerchantDictionary(learned)
        val recent = dao.txnsSince(parsed.minOf { it.date }.minusDays(1).toEpochDay())
        val merged = Deduper.merge(parsed)
        val added = mutableListOf<TxnEntity>()
        for (p in merged) {
            val received = p.receivedAt.atZone(zone).toInstant().toEpochMilli()
            val dup = recent.any { r ->
                r.amount == p.amount && r.direction == p.direction.name &&
                    ((p.reference != null && r.reference == p.reference) ||
                        (kotlin.math.abs(r.createdAtMillis - received) <= Deduper.WINDOW.toMillis()))
            }
            if (dup) continue
            val entry = if (p.rawCounterparty.isNotEmpty()) dict.resolve(p.rawCounterparty) else MerchantDictionary.Entry(p.merchant, p.category)
            val kind = when {
                p.isRefund -> TxnKind.TRANSFER
                p.direction == Direction.CREDIT -> TxnKind.INCOME
                else -> TxnKind.SPEND
            }
            val row = TxnEntity(
                id = newId(), amount = p.amount, direction = p.direction.name, dateEpochDay = p.date.toEpochDay(),
                kind = kind.name, bucket = Bucket.FUEL.name, status = TxnStatus.TO_SORT.name,
                category = if (entry.category == "Other") p.category else entry.category,
                merchant = if (entry.category == "Other") p.merchant else entry.name,
                source = Source.SMS.name, rawCounterparty = p.rawCounterparty, reference = p.reference,
                // The SMS time, not now: messages from before the balance was entered must not move it.
                createdAtMillis = received,
            )
            dao.upsertTxn(row)
            added += row
        }
        return added
    }

    // --- Weekly sort ---

    suspend fun confirm(id: String) = edit(id) { it.copy(status = TxnStatus.CONFIRMED.name) }
    suspend fun ignore(id: String) = edit(id) { it.copy(status = TxnStatus.IGNORED.name) }
    suspend fun restore(id: String) = edit(id) { it.copy(status = TxnStatus.TO_SORT.name) }
    suspend fun setBucket(id: String, bucket: Bucket) = edit(id) { it.copy(bucket = bucket.name) }
    suspend fun delete(id: String) {
        dao.deleteSharesFor(id); dao.deleteTxn(id)
    }

    /** Re-files a transaction and remembers the correction for that counterparty. */
    suspend fun recategorise(id: String, merchant: String, category: String) {
        val t = dao.txn(id) ?: return
        dao.upsertTxn(t.copy(merchant = merchant, category = category))
        val key = MerchantDictionary.normalise(t.rawCounterparty.ifEmpty { t.merchant })
        if (key.isNotEmpty()) dao.learn(MerchantLearnedEntity(key, merchant, category))
    }

    private suspend fun edit(id: String, change: (TxnEntity) -> TxnEntity) {
        dao.txn(id)?.let { dao.upsertTxn(change(it)) }
    }

    // --- Splits ---

    suspend fun split(txnId: String, people: List<String>) {
        val t = dao.txn(txnId) ?: return
        val split = Splits.equal(txnId, t.amount, people)
        dao.deleteSharesFor(txnId)
        dao.insertShares(split.shares.map { SplitShareEntity(txnId = txnId, person = it.person, amount = it.amount) })
        dao.upsertTxn(t.copy(othersShare = split.othersTotal))
    }

    suspend fun unsplit(txnId: String) {
        dao.deleteSharesFor(txnId)
        edit(txnId) { it.copy(othersShare = 0) }
    }

    /** A friend paid you back: the share is settled and the money arrives as a settlement, not income. */
    suspend fun markSharePaid(shareId: Long) {
        val share = dao.share(shareId) ?: return
        dao.setSharePaid(shareId, true)
        addManual(share.amount, "${share.person} paid back", "Settlement", direction = Direction.CREDIT)
            .let { id -> edit(id) { it.copy(kind = TxnKind.SETTLEMENT.name) } }
    }

    fun upiLink(amount: Paise, note: String): String? {
        val vpa = state.value.settings.upiVpa.ifBlank { return null }
        return Splits.upiLink(vpa, state.value.displayName, amount, note)
    }

    // --- Goals ---

    suspend fun addGoal(name: String, target: Paise, targetDate: LocalDate?, monthly: Paise, shape: Int = 0) {
        dao.upsertGoal(GoalEntity(newId(), name, target, 0, targetDate?.toEpochDay(), monthly, shape, clock()))
    }

    /** Moves money from Fuel into a goal. The money stays yours; it just stops counting as spendable. */
    suspend fun pour(goalId: String, amount: Paise) {
        val g = dao.goal(goalId) ?: return
        val fuel = state.value.runway.fuel
        val move = amount.coerceAtMost(fuel.coerceAtLeast(0))
        if (move <= 0) return
        dao.upsertGoal(g.copy(protectedAmount = g.protectedAmount + move))
    }

    suspend fun release(goalId: String, amount: Paise) {
        val g = dao.goal(goalId) ?: return
        dao.upsertGoal(g.copy(protectedAmount = (g.protectedAmount - amount).coerceAtLeast(0)))
    }

    suspend fun deleteGoal(goalId: String) = dao.deleteGoal(goalId)

    // --- Commitments ---

    suspend fun addCommitment(name: String, amount: Paise, dueDay: Int) {
        dao.upsertCommitment(CommitmentEntity(newId(), name, amount, dueDay, null, true, null))
    }

    suspend fun removeCommitment(id: String) = dao.deleteCommitment(id)

    /** Edits a commitment's name, amount and, for monthly ones, the day it falls due. Payment history is kept. */
    suspend fun updateCommitment(c: Commitment, name: String, amount: Paise, dueDay: Int?) {
        dao.upsertCommitment(
            CommitmentEntity(
                c.id, name.trim(), amount, if (c.recurring) dueDay else c.dueDayOfMonth,
                c.dueDate?.toEpochDay(), c.recurring, c.lastPaidOn?.toEpochDay(),
            ),
        )
    }

    /** Records paying a commitment: a Fixed-bucket spend, and the commitment is marked paid. */
    suspend fun payCommitment(c: Commitment) {
        val today = todayFlow.value
        val id = addManual(c.amount, c.name, "Bills", Bucket.FIXED)
        edit(id) { it.copy(kind = TxnKind.SPEND.name) }
        dao.upsertCommitment(CommitmentEntity(c.id, c.name, c.amount, c.dueDayOfMonth, c.dueDate?.toEpochDay(), c.recurring, today.toEpochDay()))
    }

    // --- Settings ---

    suspend fun setTheme(theme: String) = settingsStore.update { it.copy(theme = theme) }
    suspend fun setSpendBasis(b: SpendBasis) = settingsStore.update { it.copy(spendBasis = b) }
    suspend fun setRealSms(on: Boolean) = settingsStore.update { it.copy(realSms = on) }
    suspend fun setDemoSms(on: Boolean) = settingsStore.update { it.copy(demoSms = on) }
    suspend fun setSpokenReplies(on: Boolean) = settingsStore.update { it.copy(spokenReplies = on) }
    suspend fun setUpiVpa(vpa: String) = settingsStore.update { it.copy(upiVpa = vpa.trim()) }
    suspend fun setName(name: String) = settingsStore.update { it.copy(name = name.trim()) }
    suspend fun setHideBalance(on: Boolean) = settingsStore.update { it.copy(hideBalance = on) }
    suspend fun setNotifications(capture: Boolean? = null, weekly: Boolean? = null, goals: Boolean? = null) = settingsStore.update {
        it.copy(notifyCapture = capture ?: it.notifyCapture, notifyWeekly = weekly ?: it.notifyWeekly, notifyGoals = goals ?: it.notifyGoals)
    }

    /** History: "this month I really received X and spent Y". Shown in History only. */
    suspend fun setMonthTotals(month: java.time.YearMonth, received: Paise, spent: Paise) = settingsStore.update { s ->
        s.copy(monthEdits = s.monthEdits.filterNot { it.startsWith("$month:") }.toSet() + "$month:$received:$spent")
    }

    suspend fun clearMonthTotals(month: java.time.YearMonth) = settingsStore.update { s ->
        s.copy(monthEdits = s.monthEdits.filterNot { it.startsWith("$month:") }.toSet())
    }

    suspend fun setChatLanguage(tag: String) = settingsStore.update { it.copy(chatLanguage = tag) }

    // --- Guide and reviews ---

    suspend fun markBookRead(id: String) = settingsStore.update { it.copy(readBooks = it.readBooks + id) }

    /** Saves or unsaves an idea from a book page. */
    suspend fun toggleIdea(key: String) = settingsStore.update {
        it.copy(savedIdeas = if (key in it.savedIdeas) it.savedIdeas - key else it.savedIdeas + key)
    }

    /** Weekly review: was this purchase worth it? WORTH, NOT or UNSURE. */
    suspend fun reviewPurchase(txnId: String, verdict: String) = settingsStore.update { s ->
        s.copy(purchaseReviews = s.purchaseReviews.filterNot { it.startsWith("$txnId:") }.toSet() + "$txnId:$verdict")
    }

    /** Settings → Clear all transactions. The balance stays what it is now. */
    suspend fun clearTransactions() {
        val balance = state.value.input.balance
        dao.clearShares()
        dao.clearTxns()
        settingsStore.update { it.copy(anchorAmount = balance, anchorMillis = clock(), purchaseReviews = emptySet()) }
    }

    /** Settings → Export my data: a plain-text copy of transactions, goals and commitments, made on the phone. */
    fun exportText(): String {
        val s = state.value
        return buildString {
            appendLine("Ruvo export · ${IndianFormat.date(s.today)}")
            appendLine("Balance: ${IndianFormat.rupees(s.input.balance)} · Runway: ${s.runway.runwayDays} days")
            appendLine()
            appendLine("Transactions")
            s.activity.forEach { a ->
                val t = a.txn
                val sign = if (t.direction == Direction.CREDIT) "+" else "-"
                appendLine("${IndianFormat.date(t.date)}\t${t.merchant.ifBlank { t.category }}\t${t.category}\t$sign${IndianFormat.rupees(t.amount)}")
            }
            appendLine()
            appendLine("Goals")
            s.goals.forEach { appendLine("${it.name}\t${IndianFormat.rupees(it.protectedAmount)} of ${IndianFormat.rupees(it.target)}") }
            appendLine()
            appendLine("Commitments")
            s.commitments.forEach { appendLine("${it.name}\t${IndianFormat.rupees(it.amount)}") }
        }
    }

    /** "How much do you have right now?": resets the balance anchor to now. */
    suspend fun setBalance(amount: Paise) = settingsStore.update { it.copy(anchorAmount = amount, anchorMillis = clock()) }

    suspend fun setNextIncome(date: LocalDate?, amount: Paise? = null) = settingsStore.update {
        it.copy(nextIncomeEpochDay = date?.toEpochDay(), expectedIncome = amount ?: it.expectedIncome)
    }

    // --- First run and demo ---

    suspend fun completeOnboarding(a: OnboardingAnswers) {
        dao.clearAll()
        dao.upsertCommitments(a.commitments.map { it.toEntity() })
        val now = clock()
        settingsStore.update {
            it.copy(
                onboarded = true, ageConfirmed = true, name = a.name.trim(), demoPersona = null, cadence = a.cadence,
                expectedIncome = a.expectedIncome, nextIncomeEpochDay = a.nextIncome?.toEpochDay(),
                cycleDays = IncomePlan.defaultCycleDays(a.cadence), anchorAmount = a.balance, anchorMillis = now,
            )
        }
    }

    /** Replaces everything with one of the five sample students. */
    suspend fun loadPersona(id: String) {
        val today = LocalDate.now(zone)
        val p = DemoPersonas.byId(id, today)
        val now = clock()
        dao.clearAll()
        dao.upsertCommitments(p.input.commitments.map { it.toEntity() })
        dao.upsertGoals(p.input.goals.mapIndexed { i, g -> g.toEntity(i % 3, now - 1_000L * (10 - i)) })
        dao.upsertTxns(
            p.input.transactions.map { t ->
                val daysAgo = today.toEpochDay() - t.date.toEpochDay()
                t.toEntity(Source.DEMO, now - (daysAgo + 1) * 86_400_000L)
            },
        )
        // Shared demo spends get a flatmate who still owes their part, so "Owed to you" has something to show.
        val friends = listOf("Isha", "Kunal", "Sana")
        dao.insertShares(
            p.input.transactions.filter { it.othersShare > 0 }.mapIndexed { i, t ->
                SplitShareEntity(txnId = t.id, person = friends[i % friends.size], amount = t.othersShare, paid = i % 3 == 2)
            },
        )
        settingsStore.update {
            it.copy(
                onboarded = true, ageConfirmed = true, demoPersona = p.id, cadence = p.input.income.cadence,
                expectedIncome = p.input.income.expectedAmount, nextIncomeEpochDay = p.input.income.nextDate?.toEpochDay(),
                cycleDays = p.input.income.cycleDays, anchorAmount = p.input.balance, anchorMillis = now,
            )
        }
    }

    /** Settings → Delete everything. Also removes the account, so the next start asks to create one. */
    suspend fun deleteEverything() {
        dao.clearAll()
        settingsStore.update { UserSettings(onboarded = false, demoPersona = "aarav", theme = it.theme) }
        Session.lock()
    }

    // --- Account (on this phone only) ---

    /** Creates the account and signs in. The name also becomes what Ruvo calls you. */
    suspend fun createAccount(name: String, phone: String, pin: String) {
        require(Pins.isValid(pin) && Mobile.isValid(phone) && name.isNotBlank())
        val hash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { Pins.hash(pin) }
        settingsStore.update {
            it.copy(accountName = name.trim(), name = it.name.ifBlank { name.trim() }, accountPhone = Mobile.normalise(phone), pinHash = hash, pinFails = 0, pinPausedUntil = 0)
        }
        Session.unlock()
    }

    /** Checks a PIN. Five wrong in a row pause tries for 30 seconds; a right one clears the count. */
    suspend fun signIn(pin: String, nowMillis: Long = System.currentTimeMillis()): SignIn {
        val s = settingsStore.current()
        if (s.pinPausedUntil > nowMillis) return SignIn.Paused(s.pinPausedUntil)
        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { Pins.matches(pin, s.pinHash) }
        if (ok) {
            settingsStore.update { it.copy(pinFails = 0, pinPausedUntil = 0) }
            Session.unlock()
            return SignIn.Ok
        }
        val fails = s.pinFails + 1
        return if (fails >= MAX_PIN_TRIES) {
            val until = nowMillis + PIN_PAUSE_MILLIS
            settingsStore.update { it.copy(pinFails = 0, pinPausedUntil = until) }
            SignIn.Paused(until)
        } else {
            settingsStore.update { it.copy(pinFails = fails) }
            SignIn.Wrong(MAX_PIN_TRIES - fails)
        }
    }

    /** Changes the PIN after checking the current one. Returns false if the current PIN is wrong. */
    suspend fun changePin(current: String, next: String): Boolean {
        require(Pins.isValid(next))
        val s = settingsStore.current()
        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { Pins.matches(current, s.pinHash) }
        if (!ok) return false
        val hash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { Pins.hash(next) }
        settingsStore.update { it.copy(pinHash = hash, pinFails = 0, pinPausedUntil = 0) }
        return true
    }

    fun signOut() = Session.lock()

    suspend fun recordSnapshot() {
        val s = state.value
        if (!s.ready) return
        dao.snapshot(SnapshotEntity(s.today.toEpochDay(), s.runway.runwayDays, s.runway.bufferDays, s.runway.fuel))
    }

    private fun newId() = UUID.randomUUID().toString()

    private companion object {
        const val MAX_PIN_TRIES = 5
        const val PIN_PAUSE_MILLIS = 30_000L
    }
}

// --- Mapping between stored rows and engine models ---

internal fun TxnEntity.toEngine(shares: List<SplitShareEntity>?): Txn = Txn(
    id = id, amount = amount,
    direction = runCatching { Direction.valueOf(direction) }.getOrDefault(Direction.DEBIT),
    date = LocalDate.ofEpochDay(dateEpochDay),
    kind = runCatching { TxnKind.valueOf(kind) }.getOrDefault(TxnKind.SPEND),
    bucket = runCatching { Bucket.valueOf(bucket) }.getOrDefault(Bucket.FUEL),
    status = runCatching { TxnStatus.valueOf(status) }.getOrDefault(TxnStatus.CONFIRMED),
    category = category, merchant = merchant,
    othersShare = shares?.sumOf { it.amount } ?: othersShare,
)

internal fun Txn.toEntity(source: Source, createdAt: Long) = TxnEntity(
    id = id, amount = amount, direction = direction.name, dateEpochDay = date.toEpochDay(), kind = kind.name,
    bucket = bucket.name, status = status.name, category = category, merchant = merchant, othersShare = othersShare,
    source = source.name, createdAtMillis = createdAt,
)

internal fun CommitmentEntity.toEngine() = Commitment(
    id, name, amount, dueDayOfMonth, dueDateEpochDay?.let(LocalDate::ofEpochDay), recurring, lastPaidEpochDay?.let(LocalDate::ofEpochDay),
)

internal fun Commitment.toEntity() = CommitmentEntity(
    id, name, amount, dueDayOfMonth, dueDate?.toEpochDay(), recurring, lastPaidOn?.toEpochDay(),
)

internal fun GoalEntity.toEngine() = Goal(id, name, target, protectedAmount, targetDateEpochDay?.let(LocalDate::ofEpochDay), monthlyContribution)

internal fun Goal.toEntity(shape: Int, createdAt: Long) = GoalEntity(
    id, name, target, protectedAmount, targetDate?.toEpochDay(), monthlyContribution, shape, createdAt,
)
