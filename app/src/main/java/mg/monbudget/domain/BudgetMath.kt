package mg.monbudget.domain

import mg.monbudget.data.Envelope
import mg.monbudget.data.Goal
import mg.monbudget.data.Transaction
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object Types {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
    const val REFUND = "REFUND"
    const val SAVING = "SAVING"
    const val TRANSFER = "TRANSFER"
    val all = listOf(INCOME, EXPENSE, REFUND, SAVING, TRANSFER)
    fun label(type: String) = when(type) {
        INCOME -> "Revenu"; EXPENSE -> "Dépense"; REFUND -> "Remboursement"; SAVING -> "Épargne"; TRANSFER -> "Transfert"; else -> type
    }
}

data class Totals(val income: Long, val expense: Long, val savings: Long) {
    val available: Long get() = income - expense - savings
}

data class EnvelopeStatus(val allocated: Long, val spent: Long) {
    val remaining get() = allocated - spent
    val ratio get() = if (allocated > 0) spent.toDouble() / allocated else if (spent > 0) 1.0 else 0.0
}

object BudgetMath {
    fun within(transactions: List<Transaction>, from: LocalDate, through: LocalDate): List<Transaction> =
        transactions.filter { it.date >= from.toString() && it.date <= through.toString() }
    fun month(transactions: List<Transaction>, month: YearMonth): List<Transaction> = within(transactions, month.atDay(1), month.atEndOfMonth())
    fun totals(transactions: List<Transaction>): Totals = Totals(
        transactions.filter { it.type == Types.INCOME }.sumOf { it.amount },
        transactions.filter { it.type == Types.EXPENSE }.sumOf { it.amount } - transactions.filter { it.type == Types.REFUND }.sumOf { it.amount },
        transactions.filter { it.type == Types.SAVING }.sumOf { it.amount }
    )
    /** Net expense per calendar day, ending on the supplied date. */
    fun dailyExpenses(transactions: List<Transaction>, through: LocalDate, days: Int): List<Long> {
        require(days > 0)
        return (days - 1 downTo 0).map { offset ->
            val date = through.minusDays(offset.toLong())
            totals(within(transactions, date, date)).expense
        }
    }
    fun envelope(envelope: Envelope, transactions: List<Transaction>): EnvelopeStatus {
        val incoming = transactions.filter { it.type == Types.TRANSFER && it.destinationId == envelope.id }.sumOf { it.amount }
        val outgoing = transactions.filter { it.type == Types.TRANSFER && it.envelopeId == envelope.id }.sumOf { it.amount }
        val spent = transactions.filter { it.envelopeId == envelope.id && (it.type == Types.EXPENSE || it.type == Types.SAVING) }.sumOf { it.amount } -
            transactions.filter { it.envelopeId == envelope.id && it.type == Types.REFUND }.sumOf { it.amount }
        return EnvelopeStatus(envelope.monthlyBudget + incoming - outgoing, spent)
    }
    fun goalProgress(goal: Goal, transactions: List<Transaction>): Long = goal.initialAmount + transactions.filter { it.type == Types.SAVING && it.goalId == goal.id }.sumOf { it.amount }
    /** Inclusive calendar months, including the current month; null means no valid future deadline. */
    fun monthlyRequired(goal: Goal, current: Long, today: LocalDate): Long? {
        val deadline = runCatching { LocalDate.parse(goal.targetDate) }.getOrNull() ?: return null
        if (deadline < today) return null
        val months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)) + 1
        val remaining = (goal.target - current).coerceAtLeast(0)
        return remaining / months + if (remaining % months == 0L) 0 else 1
    }
}
