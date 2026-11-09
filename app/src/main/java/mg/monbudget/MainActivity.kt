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
private val Light = lightColorScheme(
    primary = Color(0xFF145D47), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEEDC), onPrimaryContainer = Color(0xFF103E30),
    secondary = Color(0xFF55796A), tertiary = Color(0xFFB36B29),
    background = Color(0xFFF5F7F3), onBackground = Color(0xFF192720),
    surface = Color.White, onSurface = Color(0xFF192720),
    surfaceContainerLow = Color.White, surfaceContainerHighest = Color(0xFFE8EDE8),
    onSurfaceVariant = Color(0xFF6C7B72), outlineVariant = Color(0xFFE4EBE5)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9DD8B4), onPrimary = Color(0xFF093A2B),
    primaryContainer = Color(0xFF234D3B), onPrimaryContainer = Color(0xFFD3EFDB),
    secondary = Color(0xFFB0CEBB), tertiary = Color(0xFFFFB77A),
    background = Color(0xFF101B16), surface = Color(0xFF17251D),
    surfaceContainerLow = Color(0xFF1C2D23), surfaceContainerHighest = Color(0xFF34463A),
    onSurface = Color(0xFFE2EDE3), onSurfaceVariant = Color(0xFFB9C8BB)
)

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
