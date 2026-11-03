package com.ruvo.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Dao
abstract class RuvoDao {
    @Query("SELECT * FROM txn ORDER BY dateEpochDay DESC, createdAtMillis DESC")
    abstract fun txns(): Flow<List<TxnEntity>>

    @Query("SELECT * FROM txn WHERE id = :id")
    abstract suspend fun txn(id: String): TxnEntity?

    @Query("SELECT * FROM txn WHERE dateEpochDay >= :fromEpochDay")
    abstract suspend fun txnsSince(fromEpochDay: Long): List<TxnEntity>

    @Upsert abstract suspend fun upsertTxn(t: TxnEntity)
    @Upsert abstract suspend fun upsertTxns(t: List<TxnEntity>)
    @Query("DELETE FROM txn WHERE id = :id") abstract suspend fun deleteTxn(id: String)

    @Query("SELECT * FROM commitment ORDER BY name") abstract fun commitments(): Flow<List<CommitmentEntity>>
    @Upsert abstract suspend fun upsertCommitment(c: CommitmentEntity)
    @Upsert abstract suspend fun upsertCommitments(c: List<CommitmentEntity>)
    @Query("DELETE FROM commitment WHERE id = :id") abstract suspend fun deleteCommitment(id: String)

    @Query("SELECT * FROM goal ORDER BY createdAtMillis") abstract fun goals(): Flow<List<GoalEntity>>
    @Query("SELECT * FROM goal WHERE id = :id") abstract suspend fun goal(id: String): GoalEntity?
    @Upsert abstract suspend fun upsertGoal(g: GoalEntity)
    @Upsert abstract suspend fun upsertGoals(g: List<GoalEntity>)
    @Query("DELETE FROM goal WHERE id = :id") abstract suspend fun deleteGoal(id: String)

    @Query("SELECT * FROM split_share") abstract fun shares(): Flow<List<SplitShareEntity>>
    @Insert abstract suspend fun insertShares(s: List<SplitShareEntity>)
    @Query("DELETE FROM split_share WHERE txnId = :txnId") abstract suspend fun deleteSharesFor(txnId: String)
    @Query("UPDATE split_share SET paid = :paid WHERE id = :id") abstract suspend fun setSharePaid(id: Long, paid: Boolean)
    @Query("SELECT * FROM split_share WHERE id = :id") abstract suspend fun share(id: Long): SplitShareEntity?

    @Query("SELECT * FROM merchant_learned") abstract suspend fun learned(): List<MerchantLearnedEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun learn(m: MerchantLearnedEntity)

    @Query("SELECT * FROM snapshot ORDER BY epochDay DESC LIMIT :limit") abstract fun snapshots(limit: Int): Flow<List<SnapshotEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun snapshot(s: SnapshotEntity)

    @Query("DELETE FROM txn") abstract suspend fun clearTxns()
    @Query("DELETE FROM commitment") abstract suspend fun clearCommitments()
    @Query("DELETE FROM goal") abstract suspend fun clearGoals()
    @Query("DELETE FROM split_share") abstract suspend fun clearShares()
    @Query("DELETE FROM snapshot") abstract suspend fun clearSnapshots()
    @Query("DELETE FROM merchant_learned") abstract suspend fun clearLearned()

    @Transaction
    open suspend fun clearAll() {
        clearTxns(); clearCommitments(); clearGoals(); clearShares(); clearSnapshots(); clearLearned()
    }
}

@Database(
    entities = [
        TxnEntity::class, CommitmentEntity::class, GoalEntity::class, SplitShareEntity::class,
        MerchantLearnedEntity::class, SnapshotEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class RuvoDatabase : RoomDatabase() {
    abstract fun dao(): RuvoDao

    companion object {
        /** Opens the encrypted database. The passphrase never leaves the phone (see [KeystorePassphrase]). */
        fun open(context: Context): RuvoDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = KeystorePassphrase.get(context)
            return Room.databaseBuilder(context, RuvoDatabase::class.java, "ruvo.db")
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .fallbackToDestructiveMigration()
                .build()
        }

        /** Unencrypted in-memory database for tests. */
        fun inMemory(context: Context): RuvoDatabase =
            Room.inMemoryDatabaseBuilder(context, RuvoDatabase::class.java).allowMainThreadQueries().build()
    }
}
