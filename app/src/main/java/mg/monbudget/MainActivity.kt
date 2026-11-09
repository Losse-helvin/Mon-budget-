package mg.monbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mg.monbudget.ui.BudgetApp

private val ComponentActivity.dataStore by preferencesDataStore(name = "preferences")
private val darkKey = booleanPreferencesKey("dark_theme")
private val Light = lightColorScheme(primary = Color(0xFF135C43), secondary = Color(0xFF5B776A), tertiary = Color(0xFFB66C22), background = Color(0xFFF7F8F3), surface = Color(0xFFF7F8F3))
private val Dark = darkColorScheme(primary = Color(0xFF9BD4B4), secondary = Color(0xFFB1CCB9), tertiary = Color(0xFFFFB77A))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val defaultDark = isSystemInDarkTheme()
            val dark by dataStore.data.map { it[darkKey] }.collectAsState(initial = null)
            val scope = rememberCoroutineScope()
            MaterialTheme(colorScheme = if (dark ?: defaultDark) Dark else Light) {
                BudgetApp(dark = dark ?: defaultDark, onDarkChange = { value -> scope.launch { dataStore.edit { it[darkKey] = value } } })
            }
        }
    }
}
