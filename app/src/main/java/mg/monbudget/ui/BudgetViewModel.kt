package mg.monbudget.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mg.monbudget.BudgetApplication
import mg.monbudget.data.*
import mg.monbudget.domain.Types
import java.time.LocalDate

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as BudgetApplication).database.budgetDao()
    val envelopes = dao.envelopes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val transactions = dao.transactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = dao.goals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val config = dao.configuration().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    init { viewModelScope.launch { runCatching { dao.seedIfNeeded() }.onFailure { _message.value = "Impossible d'initialiser les données : ${it.message}" } } }
    fun clearMessage() { _message.value = null }
    private fun perform(block: suspend () -> Unit) { viewModelScope.launch { runCatching { block() }.onFailure { _message.value = it.message ?: "Une erreur est survenue." } } }
    fun saveEnvelope(item: Envelope) = perform {
        require(item.name.isNotBlank() && item.monthlyBudget >= 0 && item.dailyLimit >= 0 && item.weeklyLimit >= 0) { "Vérifiez le nom et les montants." }
        dao.saveEnvelope(item.copy(name = item.name.trim()))
    }
    fun deleteEnvelope(item: Envelope) = perform {
        require(dao.envelopeUsage(item.id) == 0) { "Cette enveloppe possède un historique. Supprimez d'abord ses transactions." }
        dao.deleteEnvelope(item)
    }
    fun saveGoal(item: Goal) = perform {
        require(item.name.isNotBlank() && item.target > 0 && item.initialAmount >= 0) { "Vérifiez le nom et les montants de l'objectif." }
        require(item.targetDate.isBlank() || runCatching { LocalDate.parse(item.targetDate) }.isSuccess) { "Date invalide : utilisez AAAA-MM-JJ." }
        dao.saveGoal(item.copy(name = item.name.trim()))
    }
    fun deleteGoal(item: Goal) = perform {
        require(dao.goalUsage(item.id) == 0) { "Cet objectif possède des versements. Supprimez-les d'abord." }
        dao.deleteGoal(item)
    }
    fun saveTransaction(item: Transaction) = perform {
        require(item.type in Types.all && item.amount > 0 && runCatching { LocalDate.parse(item.date) }.isSuccess) { "Vérifiez le montant et la date (AAAA-MM-JJ)." }
        require(item.type == Types.INCOME || envelopes.value.any { it.id == item.envelopeId }) { "Choisissez une enveloppe." }
        require(item.type != Types.TRANSFER || (item.destinationId != item.envelopeId && envelopes.value.any { it.id == item.destinationId })) { "Choisissez deux enveloppes différentes." }
        require(item.goalId == null || (item.type == Types.SAVING && goals.value.any { it.id == item.goalId })) { "Objectif invalide." }
        dao.saveTransaction(item.copy(description = item.description.trim(), paymentMethod = item.paymentMethod.trim()))
    }
    fun deleteTransaction(item: Transaction) = perform { dao.deleteTransaction(item) }
    fun saveIncomeTarget(amount: Long) = perform {
        require(amount >= 0) { "Le revenu prévu doit être positif ou nul." }
        dao.saveConfiguration(Configuration(incomeTarget = amount))
    }
}
