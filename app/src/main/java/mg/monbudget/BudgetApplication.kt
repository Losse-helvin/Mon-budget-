package mg.monbudget

import android.app.Application
import mg.monbudget.data.BudgetDatabase

class BudgetApplication : Application() {
    val database by lazy { BudgetDatabase.create(this) }
}
