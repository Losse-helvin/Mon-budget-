package mg.monbudget.domain

import mg.monbudget.data.Envelope
import mg.monbudget.data.Goal
import mg.monbudget.data.Transaction
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetMathTest {
    private fun tx(type: String, amount: Long, date: String = "2026-03-10", source: Long? = null, destination: Long? = null, goal: Long? = null) =
        Transaction(type = type, amount = amount, date = date, envelopeId = source, destinationId = destination, goalId = goal)

    @Test fun transferIsNeitherIncomeNorExpense() {
        val list = listOf(tx(Types.INCOME, 1_000_000), tx(Types.EXPENSE, 100_000, source = 1), tx(Types.SAVING, 50_000, source = 1), tx(Types.TRANSFER, 20_000, source = 1, destination = 2))
        val total = BudgetMath.totals(list)
        assertEquals(1_000_000L, total.income)
        assertEquals(100_000L, total.expense)
        assertEquals(50_000L, total.savings)
        assertEquals(850_000L, total.available)
        assertEquals(80_000L, BudgetMath.envelope(Envelope(1, "Source", 100_000), list).allocated)
        assertEquals(70_000L, BudgetMath.envelope(Envelope(2, "Destination", 50_000), list).allocated)
        assertEquals(150_000L, BudgetMath.envelope(Envelope(1, "Source", 100_000), list).spent)
    }
    @Test fun refundsReduceNetExpensesAndEnvelopeSpend() {
        val list = listOf(tx(Types.EXPENSE, 30_000, source = 1), tx(Types.REFUND, 5_000, source = 1))
        assertEquals(25_000L, BudgetMath.totals(list).expense)
        assertEquals(25_000L, BudgetMath.envelope(Envelope(1, "Nourriture", 100_000), list).spent)
        assertEquals(-25_000L, BudgetMath.totals(list).available)
    }
    @Test fun monthBoundariesAndLeapYear() {
        val list = listOf(tx(Types.EXPENSE, 1, "2024-02-01"), tx(Types.EXPENSE, 2, "2024-02-29"), tx(Types.EXPENSE, 4, "2024-03-01"), tx(Types.EXPENSE, 8, "2023-02-28"))
        assertEquals(3L, BudgetMath.totals(BudgetMath.month(list, YearMonth.of(2024, 2))).expense)
    }
    @Test fun goalUsesOnlyLinkedSavingsAndRoundsUpSimulation() {
        val goal = Goal(3, "Voyage", 100, 10, "2026-05-20")
        val list = listOf(tx(Types.SAVING, 20, goal = 3), tx(Types.SAVING, 30, goal = 4), tx(Types.EXPENSE, 40, goal = 3))
        assertEquals(30L, BudgetMath.goalProgress(goal, list))
        assertEquals(24L, BudgetMath.monthlyRequired(goal, 30, LocalDate.parse("2026-03-01"))!!)
        assertNull(BudgetMath.monthlyRequired(goal, 30, LocalDate.parse("2026-05-21")))
    }
    @Test fun zeroBudgetAndOverspendingAreVisible() {
        assertEquals(0.0, BudgetMath.envelope(Envelope(1, "Test", 0), emptyList()).ratio, 0.0)
        assertEquals(1.0, BudgetMath.envelope(Envelope(1, "Test", 0), listOf(tx(Types.EXPENSE, 10, source = 1))).ratio, 0.0)
        assertEquals(-10L, BudgetMath.envelope(Envelope(1, "Test", 0), listOf(tx(Types.EXPENSE, 10, source = 1))).remaining)
    }
}
