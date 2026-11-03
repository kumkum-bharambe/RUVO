package com.ruvo.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where a transaction came from. Shown as a small tag so the user always knows. */
enum class Source { SMS, MANUAL, DEMO }

@Entity(tableName = "txn", indices = [Index("dateEpochDay"), Index("status")])
data class TxnEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    /** "DEBIT" or "CREDIT". */
    val direction: String,
    val dateEpochDay: Long,
    /** SPEND, INCOME, SETTLEMENT or TRANSFER. */
    val kind: String,
    /** FIXED, FUEL or FUTURE. */
    val bucket: String,
    /** TO_SORT, CONFIRMED or IGNORED. */
    val status: String,
    val category: String,
    val merchant: String,
    val othersShare: Long = 0,
    val source: String = Source.MANUAL.name,
    /** Counterparty as the bank wrote it, used only to learn corrections on the phone. */
    val rawCounterparty: String = "",
    val reference: String? = null,
    val note: String = "",
    /** When Ruvo recorded it. Balance moves only for transactions recorded after the balance anchor. */
    val createdAtMillis: Long,
)

@Entity(tableName = "commitment")
data class CommitmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: Long,
    val dueDayOfMonth: Int?,
    val dueDateEpochDay: Long?,
    val recurring: Boolean,
    val lastPaidEpochDay: Long?,
)

@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val target: Long,
    val protectedAmount: Long,
    val targetDateEpochDay: Long?,
    val monthlyContribution: Long,
    /** 0 round jar, 1 tall bottle, 2 wide bowl. Purely visual. */
    val jarShape: Int = 0,
    val createdAtMillis: Long,
)

@Entity(tableName = "split_share", indices = [Index("txnId")])
data class SplitShareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val txnId: String,
    val person: String,
    val amount: Long,
    val paid: Boolean = false,
)

/** A merchant correction the user made, so the same counterparty is named and filed that way from now on. */
@Entity(tableName = "merchant_learned")
data class MerchantLearnedEntity(
    @PrimaryKey val key: String,
    val name: String,
    val category: String,
)

/** Daily runway snapshot for the monthly review chart. */
@Entity(tableName = "snapshot")
data class SnapshotEntity(
    @PrimaryKey val epochDay: Long,
    val runwayDays: Int,
    val bufferDays: Int?,
    val fuel: Long,
)
