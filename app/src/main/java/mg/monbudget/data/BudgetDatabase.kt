package mg.monbudget.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "envelopes")
data class Envelope(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val monthlyBudget: Long,
    val dailyLimit: Long = 0,
    val weeklyLimit: Long = 0
)

/** Amounts are whole Ariary. A transfer has a source and destination; it is not income or expense. */
@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("envelopeId"), Index("destinationId"), Index("goalId")],
    foreignKeys = [
        ForeignKey(entity = Envelope::class, parentColumns = ["id"], childColumns = ["envelopeId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Envelope::class, parentColumns = ["id"], childColumns = ["destinationId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.RESTRICT)
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val date: String,
    val envelopeId: Long? = null,
    val destinationId: Long? = null,
    val goalId: Long? = null,
    val description: String = "",
    val paymentMethod: String = ""
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val initialAmount: Long = 0,
    val targetDate: String = ""
)

@Entity(tableName = "configuration")
data class Configuration(@PrimaryKey val id: Int = 1, val incomeTarget: Long = 1_000_000)

@Dao
abstract class BudgetDao {
    @Query("SELECT * FROM envelopes ORDER BY name COLLATE NOCASE") abstract fun envelopes(): Flow<List<Envelope>>
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC") abstract fun transactions(): Flow<List<Transaction>>
    @Query("SELECT * FROM goals ORDER BY id") abstract fun goals(): Flow<List<Goal>>
    @Query("SELECT * FROM configuration WHERE id = 1") abstract fun configuration(): Flow<Configuration?>
    @Query("SELECT * FROM configuration WHERE id = 1") abstract suspend fun getConfiguration(): Configuration?
    @Upsert abstract suspend fun saveConfiguration(value: Configuration)
    @Upsert abstract suspend fun saveEnvelope(value: Envelope): Long
    @Upsert abstract suspend fun saveTransaction(value: Transaction): Long
    @Upsert abstract suspend fun saveGoal(value: Goal): Long
    @Delete abstract suspend fun deleteTransaction(value: Transaction)
    @Delete abstract suspend fun deleteEnvelope(value: Envelope)
    @Delete abstract suspend fun deleteGoal(value: Goal)
    @Query("SELECT COUNT(*) FROM transactions WHERE envelopeId = :id OR destinationId = :id") abstract suspend fun envelopeUsage(id: Long): Int
    @Query("SELECT COUNT(*) FROM transactions WHERE goalId = :id") abstract suspend fun goalUsage(id: Long): Int

    @androidx.room.Transaction
    open suspend fun seedIfNeeded() {
        if (getConfiguration() != null) return
        saveConfiguration(Configuration())
        listOf(
            Envelope(name = "Loyer, eau et électricité", monthlyBudget = 300_000),
            Envelope(name = "Nourriture", monthlyBudget = 270_000),
            Envelope(name = "Épargne principale", monthlyBudget = 250_000),
            Envelope(name = "Transport", monthlyBudget = 60_000),
            Envelope(name = "Achats et crédit", monthlyBudget = 50_000),
            Envelope(name = "Loisirs et sorties", monthlyBudget = 50_000),
            Envelope(name = "Imprévus", monthlyBudget = 20_000)
        ).forEach { saveEnvelope(it) }
        saveGoal(Goal(name = "Épargne personnelle", target = 3_000_000))
    }
}

@Database(entities = [Envelope::class, Transaction::class, Goal::class, Configuration::class], version = 1, exportSchema = false)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    companion object {
        fun create(context: Context): BudgetDatabase = Room.databaseBuilder(context, BudgetDatabase::class.java, "budget.db").build()
    }
}
