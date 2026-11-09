package mg.monbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import mg.monbudget.data.*
import mg.monbudget.domain.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private fun money(value: Long): String = NumberFormat.getIntegerInstance(Locale.FRANCE).format(value) + " Ar"
private val labels = listOf("Accueil", "Enveloppes", "Historique", "Objectifs", "Réglages")
private val icons = listOf(Icons.Default.Home, Icons.Default.AccountBalanceWallet, Icons.AutoMirrored.Filled.List, Icons.Default.Flag, Icons.Default.Settings)

@Composable
fun BudgetApp(dark: Boolean, onDarkChange: (Boolean) -> Unit, vm: BudgetViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val selected = entry?.destination?.route ?: labels.first()
    val envelopes by vm.envelopes.collectAsState()
    val transactions by vm.transactions.collectAsState()
    val goals by vm.goals.collectAsState()
    val config by vm.config.collectAsState()
    val message by vm.message.collectAsState()
    var addTransaction by remember { mutableStateOf(false) }
    Scaffold(
        bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) { labels.forEachIndexed { index, title ->
            NavigationBarItem(selected = selected == title, onClick = { nav.navigate(title) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(icons[index], contentDescription = null) }, label = { Text(title, maxLines = 1, style = MaterialTheme.typography.labelSmall) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer))
        } } },
        floatingActionButton = { if (selected == "Historique") FloatingActionButton(onClick = { addTransaction = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Default.Add, contentDescription = "Ajouter une transaction") } }
    ) { padding ->
        NavHost(navController = nav, startDestination = labels.first(), modifier = Modifier.padding(padding)) {
            composable(labels[0]) { Dashboard(envelopes, transactions, goals, config, onAdd = { addTransaction = true }, onNavigate = { route -> nav.navigate(route) { launchSingleTop = true } }) }
            composable(labels[1]) { EnvelopesScreen(envelopes, transactions, vm) }
            composable(labels[2]) { HistoryScreen(envelopes, goals, transactions, vm) }
            composable(labels[3]) { GoalsScreen(goals, transactions, vm) }
            composable(labels[4]) { SettingsScreen(config, dark, onDarkChange, vm) }
        }
    }
    if (addTransaction) TransactionDialog(null, envelopes, goals, onDismiss = { addTransaction = false }, onSave = { vm.saveTransaction(it); addTransaction = false })
    if (message != null) AlertDialog(onDismissRequest = vm::clearMessage, title = { Text("Information") }, text = { Text(message.orEmpty()) }, confirmButton = { TextButton(onClick = vm::clearMessage) { Text("Compris") } })
}

private val AccentColors = listOf(Color(0xFF65A882), Color(0xFFE5968D), Color(0xFF6C96B0), Color(0xFFD8AA54), Color(0xFFAC94C6))
private fun accent(id: Long): Color = AccentColors[(id % AccentColors.size).toInt()]
private fun monthLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)).replaceFirstChar { it.titlecase(Locale.FRENCH) }

@Composable private fun PageTitle(title: String, subtitle: String) {
    Column { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp); Spacer(Modifier.height(4.dp)); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
@Composable private fun SectionTitle(title: String) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) } }
@Composable private fun Line(label: String, value: String, emphasized: Boolean = false) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.width(8.dp)); Text(value, fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium, style = MaterialTheme.typography.bodyMedium) } }
@Composable private fun Progress(value: Double, color: Color = MaterialTheme.colorScheme.primary) { LinearProgressIndicator(progress = { value.coerceIn(0.0, 1.0).toFloat() }, modifier = Modifier.fillMaxWidth().height(7.dp), color = color, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round) }

@Composable private fun IconTile(icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(42.dp).background(color.copy(alpha = .14f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(21.dp)) }
}
@Composable private fun SectionHeading(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(title); Spacer(Modifier.weight(1f)); TextButton(onClick = onClick) { Text(action, fontWeight = FontWeight.SemiBold) }
    }
}
@Composable private fun StatCard(label: String, amount: Long, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTile(icon, color)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(money(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Dashboard(envelopes: List<Envelope>, transactions: List<Transaction>, goals: List<Goal>, config: Configuration?, onAdd: () -> Unit, onNavigate: (String) -> Unit) {
    val today = LocalDate.now(); val month = YearMonth.from(today)
    val thisMonth = BudgetMath.month(transactions, month); val totals = BudgetMath.totals(thisMonth)
    val previous = BudgetMath.totals(BudgetMath.month(transactions, month.minusMonths(1)))
    val throughToday = transactions.filter { it.date <= today.toString() }
    val actualBalance = BudgetMath.totals(throughToday).available
    val monthBudget = envelopes.sumOf { it.monthlyBudget }
    val monthUsed = envelopes.sumOf { BudgetMath.envelope(it, thisMonth).spent }
    val remaining = monthBudget - monthUsed
    val goalSavings = goals.sumOf { BudgetMath.goalProgress(it, throughToday) }
    val daysLeft = month.lengthOfMonth() - today.dayOfMonth + 1
    val weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val weekExpense = BudgetMath.totals(BudgetMath.within(transactions, weekStart, today)).expense
    val previousWeekExpense = BudgetMath.totals(BudgetMath.within(transactions, weekStart.minusWeeks(1), weekStart.minusDays(1))).expense
    val alerts = envelopes.map { it to BudgetMath.envelope(it, thisMonth) }.filter { it.second.ratio >= 0.7 }.sortedByDescending { it.second.ratio }
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MON BUDGET", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp)); Text("Bonjour !", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                    Text(monthLabel(today), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                FilledTonalIconButton(onClick = { onNavigate("Réglages") }) { Icon(Icons.Default.Settings, "Ouvrir les réglages") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF0C4937), Color(0xFF28795A)))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFFC5E5D0), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("SOLDE DISPONIBLE", color = Color(0xFFCEE9D7), letterSpacing = 1.sp, style = MaterialTheme.typography.labelMedium) }
                    Text(money(actualBalance), style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Revenus − dépenses nettes − épargne", color = Color(0xFFC8E5D2), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(3.dp))
                    Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDF09B), contentColor = Color(0xFF174432)), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Ajouter un mouvement", fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Revenus du mois", totals.income, Icons.Default.SouthWest, Color(0xFF468A60), Modifier.weight(1f))
            StatCard("Dépenses nettes", totals.expense, Icons.Default.NorthEast, Color(0xFFD37868), Modifier.weight(1f))
        } }
        item { Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { SectionTitle("Dépenses récentes"); Text("Les 7 derniers jours", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }; IconTile(Icons.Default.BarChart, MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(6.dp)); SpendingChart(transactions, today)
        } }
        item { SectionHeading("Mes enveloppes", "Voir tout") { onNavigate("Enveloppes") } }
        if (envelopes.isEmpty()) item { Panel { Text("Créez une enveloppe pour organiser votre budget.") } }
        items(envelopes.take(3), key = { "preview-${it.id}" }) { env ->
            val status = BudgetMath.envelope(env, thisMonth)
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Default.AccountBalanceWallet, accent(env.id)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(env.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Utilisé : ${money(status.spent)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.width(4.dp)); Text(money(status.remaining), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                Progress(status.ratio, if (status.ratio >= 1) MaterialTheme.colorScheme.error else accent(env.id))
            }
        }
        item { SectionHeading("Mes objectifs", "Voir tout") { onNavigate("Objectifs") } }
        item { Panel {
            Line("Épargne liée aux objectifs", money(goalSavings), true)
            goals.firstOrNull()?.let { goal -> val current = BudgetMath.goalProgress(goal, throughToday); Line(goal.name, "${money(current)} / ${money(goal.target)}"); Progress(current.toDouble() / goal.target, accent(goal.id)) }
            if (goals.isEmpty()) Text("Créez un objectif pour suivre votre progression.", style = MaterialTheme.typography.bodySmall)
        } }
        item { SectionTitle("Votre budget en bref") }
        item { Panel {
            Line("Budget restant ce mois", money(remaining), true); Progress(if (monthBudget > 0) monthUsed.toDouble() / monthBudget else 0.0)
            Text("${if (monthBudget > 0) (monthUsed.toDouble() / monthBudget * 100).toInt() else 0} % utilisé · ${money(monthBudget)} prévu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(vertical = 4.dp)); Line("Épargne versée ce mois", money(totals.savings))
            Line("Dépensé aujourd’hui", money(BudgetMath.totals(BudgetMath.within(transactions, today, today)).expense))
            Line("Budget conseillé / jour", money(remaining.coerceAtLeast(0) / daysLeft))
            Line("Dépenses du mois précédent", money(previous.expense)); Line("Revenu mensuel prévu", money(config?.incomeTarget ?: 0))
            Text("Le revenu prévu n’est pas ajouté au solde : enregistrez chaque revenu reçu.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { Panel {
            Row(verticalAlignment = Alignment.CenterVertically) { IconTile(Icons.Default.Lightbulb, Color(0xFFB9853B)); Spacer(Modifier.width(12.dp)); SectionTitle("Conseil du moment") }
            val alert = alerts.firstOrNull()
            Text(if (alert != null) "${alert.first.name} : ${(alert.second.ratio * 100).toInt()} % du budget utilisé. Il reste ${money(alert.second.remaining)} ce mois-ci." else if (transactions.isEmpty()) "Commencez par enregistrer vos revenus et dépenses pour obtenir un suivi personnalisé." else "Il reste ${money(remaining)} dans vos enveloppes pour ce mois. Gardez une marge pour les imprévus.", style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(Modifier.padding(vertical = 4.dp)); Line("Dépenses depuis lundi", money(weekExpense)); Line("Semaine précédente", money(previousWeekExpense))
        } }
    }
}

/** Each bar represents recorded net expenses for one day; transfers and savings are excluded. */
@Composable private fun SpendingChart(transactions: List<Transaction>, today: LocalDate) {
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = BudgetMath.dailyExpenses(transactions, today, days.size)
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { index, day ->
            val fraction = (values[index].coerceAtLeast(0).toDouble() / max).toFloat()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f)) {
                Box(Modifier.height(96.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.width(20.dp).height((4 + 92 * fraction).dp).background(if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .30f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                }
                Spacer(Modifier.height(7.dp)); Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH).take(3).replaceFirstChar { it.titlecase(Locale.FRENCH) }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Text("Dépenses nettes par jour · les valeurs négatives ne sont pas représentées", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun EnvelopesScreen(envelopes: List<Envelope>, transactions: List<Transaction>, vm: BudgetViewModel) {
    var editing by remember { mutableStateOf<Envelope?>(null) }; var show by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Envelope?>(null) }
    val today = LocalDate.now(); val monthTransactions = BudgetMath.month(transactions, YearMonth.from(today))
    val allocated = envelopes.sumOf { BudgetMath.envelope(it, monthTransactions).allocated }
    val spent = envelopes.sumOf { BudgetMath.envelope(it, monthTransactions).spent }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Mes enveloppes", "Un budget pour chaque priorité · ${monthLabel(today)}") }
        item { Spacer(Modifier.height(2.dp)); Panel {
            Text("BUDGET RESTANT", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(money(allocated - spent), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text("sur ${money(allocated)} prévu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                FilledTonalIconButton(onClick = { editing = null; show = true }) { Icon(Icons.Default.Add, "Nouvelle enveloppe") }
            }
        } }
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { SectionTitle("Vos catégories"); Spacer(Modifier.weight(1f)); Text("${envelopes.size} enveloppes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        if (envelopes.isEmpty()) item { Panel { Text("Aucune enveloppe. Ajoutez-en une pour commencer.") } }
        items(envelopes, key = { it.id }) { env ->
            val status = BudgetMath.envelope(env, monthTransactions); val color = accent(env.id)
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Default.AccountBalanceWallet, color); Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text(env.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("Budget : ${money(status.allocated)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    IconButton(onClick = { editing = env; show = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Edit, "Modifier ${env.name}", modifier = Modifier.size(19.dp)) }
                    IconButton(onClick = { deleting = env }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.DeleteOutline, "Supprimer ${env.name}", modifier = Modifier.size(19.dp)) }
                }
                Spacer(Modifier.height(4.dp)); Progress(status.ratio, if (status.ratio >= 1) MaterialTheme.colorScheme.error else color)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Utilisé ${money(status.spent)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text("Reste ${money(status.remaining)}", color = if (status.remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                val weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                if (env.dailyLimit > 0) Line("Aujourd’hui / limite", "${money(BudgetMath.envelope(env, BudgetMath.within(transactions, today, today)).spent)} / ${money(env.dailyLimit)}")
                if (env.weeklyLimit > 0) Line("Cette semaine / limite", "${money(BudgetMath.envelope(env, BudgetMath.within(transactions, weekStart, weekStart.plusDays(6))).spent)} / ${money(env.weeklyLimit)}")
                if (status.ratio >= .7) Text(if (status.ratio >= 1) "Budget atteint ou dépassé" else if (status.ratio >= .9) "Plus de 90 % utilisé" else "Plus de 70 % utilisé", color = if (status.ratio >= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    if (show) EnvelopeDialog(editing, onDismiss = { show = false }, onSave = { vm.saveEnvelope(it); show = false })
    deleting?.let { env -> ConfirmDelete("l’enveloppe ${env.name}", { deleting = null }) { vm.deleteEnvelope(env); deleting = null } }
}

@Composable
private fun HistoryScreen(envelopes: List<Envelope>, goals: List<Goal>, transactions: List<Transaction>, vm: BudgetViewModel) {
    var editing by remember { mutableStateOf<Transaction?>(null) }; var deleting by remember { mutableStateOf<Transaction?>(null) }
    var search by remember { mutableStateOf("") }; var filter by remember { mutableStateOf("Tout") }
    val visible = transactions.filter { t -> (filter == "Tout" || t.type == filter) && (search.isBlank() || t.date.contains(search, true) || t.description.contains(search, true) || envelopes.find { it.id == t.envelopeId }?.name?.contains(search, true) == true) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Historique", "Tous vos mouvements en un coup d’œil.") }
        item { OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Date, catégorie, description") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp)) }
        item { ChoiceField("Type", filter, listOf("Tout") + Types.all, { if (it == "Tout") it else Types.label(it) }, { filter = it }) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { SectionTitle("Mouvements"); Text("${visible.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) } }
        if (visible.isEmpty()) item { Panel { Text("Aucune transaction pour cette recherche.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        items(visible, key = { it.id }) { t ->
            val color = when(t.type) { Types.INCOME, Types.REFUND -> Color(0xFF52956A); Types.EXPENSE -> Color(0xFFD07970); Types.SAVING -> Color(0xFF8A82C4); else -> Color(0xFF668DA2) }
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(when(t.type) { Types.INCOME, Types.REFUND -> Icons.Default.SouthWest; Types.EXPENSE -> Icons.Default.NorthEast; Types.SAVING -> Icons.Default.Savings; else -> Icons.Default.SwapHoriz }, color)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(Types.label(t.type), fontWeight = FontWeight.Bold); Text("${t.date} · ${envelopes.find { it.id == t.envelopeId }?.name ?: "Sans enveloppe"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2); if (t.description.isNotBlank()) Text(t.description, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.width(6.dp)); Text(money(t.amount), fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.labelLarge)
                }
                if (t.type == Types.TRANSFER) Text("Vers ${envelopes.find { it.id == t.destinationId }?.name ?: "enveloppe supprimée"}", style = MaterialTheme.typography.bodySmall)
                Row { TextButton(onClick = { editing = t }) { Text("Modifier") }; TextButton(onClick = { deleting = t }) { Text("Supprimer") } }
            }
        }
        item { Spacer(Modifier.height(68.dp)) }
    }
    editing?.let { TransactionDialog(it, envelopes, goals, { editing = null }) { value -> vm.saveTransaction(value); editing = null } }
    deleting?.let { t -> ConfirmDelete("cette transaction", { deleting = null }) { vm.deleteTransaction(t); deleting = null } }
}

@Composable
private fun GoalsScreen(goals: List<Goal>, transactions: List<Transaction>, vm: BudgetViewModel) {
    var editing by remember { mutableStateOf<Goal?>(null) }; var show by remember { mutableStateOf(false) }; var deleting by remember { mutableStateOf<Goal?>(null) }
    val today = LocalDate.now(); val throughToday = transactions.filter { it.date <= today.toString() }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { PageTitle("Mes objectifs", "Un peu aujourd’hui, beaucoup demain.") }; FilledTonalIconButton(onClick = { editing = null; show = true }) { Icon(Icons.Default.Add, "Nouvel objectif") } } }
        item { Panel { Text("VOTRE ÉPARGNE", color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, style = MaterialTheme.typography.labelSmall); Text(money(goals.sumOf { BudgetMath.goalProgress(it, throughToday) }), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text("Montants de départ et versements liés aux objectifs", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }
        item { SectionTitle("Vos projets") }
        if (goals.isEmpty()) item { Panel { Text("Ajoutez un objectif pour donner vie à vos projets.") } }
        items(goals, key = { it.id }) { goal ->
            val current = BudgetMath.goalProgress(goal, throughToday); val needed = BudgetMath.monthlyRequired(goal, current, today); val color = accent(goal.id)
            Panel {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Default.Flag, color); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(goal.name, fontWeight = FontWeight.Bold); if (goal.targetDate.isNotBlank()) Text("Date souhaitée : ${goal.targetDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { editing = goal; show = true }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Edit, "Modifier ${goal.name}", Modifier.size(19.dp)) }
                    IconButton(onClick = { deleting = goal }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.DeleteOutline, "Supprimer ${goal.name}", Modifier.size(19.dp)) }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) { Text("Épargné", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(money(current), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) }
                    Column(Modifier.weight(1f)) { Text("Reste à épargner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(money((goal.target - current).coerceAtLeast(0)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { Progress(current.toDouble() / goal.target, color) }; Spacer(Modifier.width(10.dp)); Text("${(current.toDouble() / goal.target * 100).toInt()} %", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("Objectif : ${money(goal.target)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (needed != null) Line("À mettre de côté / mois", money(needed))
                else Text(if (goal.targetDate.isBlank()) "Ajoutez une date pour simuler l’effort mensuel." else "La date souhaitée est passée.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (show) GoalDialog(editing, { show = false }) { vm.saveGoal(it); show = false }
    deleting?.let { goal -> ConfirmDelete("l’objectif ${goal.name}", { deleting = null }) { vm.deleteGoal(goal); deleting = null } }
}

@Composable
private fun SettingsScreen(config: Configuration?, dark: Boolean, onDarkChange: (Boolean) -> Unit, vm: BudgetViewModel) {
    var income by remember(config?.incomeTarget) { mutableStateOf(config?.incomeTarget?.toString() ?: "") }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageTitle("Réglages", "Un budget qui s'adapte à vous.")
        Panel {
            Text("Apparence", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Thème sombre", modifier = Modifier.weight(1f)); Switch(checked = dark, onCheckedChange = onDarkChange) }
        }
        Panel {
            Text("Revenu mensuel prévu", fontWeight = FontWeight.SemiBold)
            AmountField(income, { income = it }, "Montant prévu en Ar")
            if (error) Text("Saisissez un montant entier positif ou nul.", color = MaterialTheme.colorScheme.error)
            Button(onClick = { val value = income.toLongOrNull(); if (value == null || value < 0) error = true else { vm.saveIncomeTarget(value); error = false } }) { Text("Enregistrer") }
            Text("Cette prévision ne crée pas de revenu réel. Modifiez les montants des enveloppes dans l'onglet Enveloppes.", style = MaterialTheme.typography.bodySmall)
        }
        Panel { Text("Vos données restent sur cet appareil", fontWeight = FontWeight.SemiBold); Text("Aucun compte ni connexion Internet. Les données ne sont pas automatiquement sauvegardées. La désinstallation efface les données locales.", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable private fun ConfirmDelete(what: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Confirmer la suppression") }, text = { Text("Supprimer $what ? Cette action est définitive.") }, confirmButton = { TextButton(onClick = onConfirm) { Text("Supprimer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}

@Composable private fun AmountField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, { if (it.length <= 15 && it.all(Char::isDigit)) onChange(it) }, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable private fun <T> ChoiceField(label: String, value: T, options: List<T>, display: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label : ${display(value)}", modifier = Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { options.forEach { option -> DropdownMenuItem(text = { Text(display(option)) }, onClick = { onSelect(option); expanded = false }) } }
    }
}

@Composable
private fun EnvelopeDialog(original: Envelope?, onDismiss: () -> Unit, onSave: (Envelope) -> Unit) {
    var name by remember { mutableStateOf(original?.name ?: "") }; var budget by remember { mutableStateOf(original?.monthlyBudget?.toString() ?: "") }
    var daily by remember { mutableStateOf(original?.dailyLimit?.toString() ?: "0") }; var weekly by remember { mutableStateOf(original?.weeklyLimit?.toString() ?: "0") }; var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (original == null) "Nouvelle enveloppe" else "Modifier l'enveloppe") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true)
            AmountField(budget, { budget = it }, "Budget mensuel (Ar)")
            AmountField(daily, { daily = it }, "Limite quotidienne (0 = aucune)")
            AmountField(weekly, { weekly = it }, "Limite hebdomadaire (0 = aucune)")
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(onClick = {
        val b = budget.toLongOrNull(); val d = daily.toLongOrNull(); val w = weekly.toLongOrNull()
        if (name.isBlank() || b == null || d == null || w == null) error = "Renseignez un nom et des montants valides." else onSave(Envelope(original?.id ?: 0, name, b, d, w))
    }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}

@Composable
private fun GoalDialog(original: Goal?, onDismiss: () -> Unit, onSave: (Goal) -> Unit) {
    var name by remember { mutableStateOf(original?.name ?: "") }; var target by remember { mutableStateOf(original?.target?.toString() ?: "") }
    var initial by remember { mutableStateOf(original?.initialAmount?.toString() ?: "0") }; var date by remember { mutableStateOf(original?.targetDate ?: "") }; var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (original == null) "Nouvel objectif" else "Modifier l'objectif") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nom") }, singleLine = true)
            AmountField(target, { target = it }, "Montant cible (Ar)")
            AmountField(initial, { initial = it }, "Montant de départ (Ar)")
            OutlinedTextField(date, { date = it }, label = { Text("Date souhaitée : AAAA-MM-JJ") }, singleLine = true)
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(onClick = {
        val t = target.toLongOrNull(); val i = initial.toLongOrNull()
        if (name.isBlank() || t == null || t <= 0 || i == null || (date.isNotBlank() && runCatching { LocalDate.parse(date) }.isFailure)) error = "Vérifiez les montants et la date (AAAA-MM-JJ)." else onSave(Goal(original?.id ?: 0, name, t, i, date))
    }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}

@Composable
private fun TransactionDialog(original: Transaction?, envelopes: List<Envelope>, goals: List<Goal>, onDismiss: () -> Unit, onSave: (Transaction) -> Unit) {
    var type by remember { mutableStateOf(original?.type ?: Types.EXPENSE) }
    var amount by remember { mutableStateOf(original?.amount?.toString() ?: "") }
    var date by remember { mutableStateOf(original?.date ?: LocalDate.now().toString()) }
    var envelopeId by remember { mutableStateOf(original?.envelopeId ?: envelopes.firstOrNull()?.id) }
    var destinationId by remember { mutableStateOf(original?.destinationId) }
    var goalId by remember { mutableStateOf(original?.goalId) }
    var description by remember { mutableStateOf(original?.description ?: "") }; var payment by remember { mutableStateOf(original?.paymentMethod ?: "") }; var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (original == null) "Ajouter un mouvement" else "Modifier le mouvement") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceField("Type", type, Types.all, Types::label) { type = it }
            AmountField(amount, { amount = it }, "Montant (Ar)")
            if (type != Types.INCOME) ChoiceField("Enveloppe", envelopeId, envelopes.map { it.id }, { id -> envelopes.find { it.id == id }?.name ?: "Choisir" }) { envelopeId = it }
            if (type == Types.TRANSFER) ChoiceField("Vers", destinationId, envelopes.filter { it.id != envelopeId }.map { it.id }, { id -> envelopes.find { it.id == id }?.name ?: "Choisir" }) { destinationId = it }
            if (type == Types.SAVING) ChoiceField("Objectif", goalId, listOf(null) + goals.map { it.id }, { id -> goals.find { it.id == id }?.name ?: "Aucun" }) { goalId = it }
            OutlinedTextField(date, { date = it }, label = { Text("Date : AAAA-MM-JJ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description (facultatif)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(payment, { payment = it }, label = { Text("Moyen de paiement (facultatif)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(onClick = {
        val a = amount.toLongOrNull()
        if (a == null || a <= 0 || runCatching { LocalDate.parse(date) }.isFailure || (type != Types.INCOME && envelopes.none { it.id == envelopeId }) || (type == Types.TRANSFER && (destinationId == envelopeId || envelopes.none { it.id == destinationId }))) error = "Vérifiez le montant, la date et les enveloppes."
        else onSave(Transaction(original?.id ?: 0, type, a, date, if (type == Types.INCOME) null else envelopeId, if (type == Types.TRANSFER) destinationId else null, if (type == Types.SAVING) goalId else null, description, payment))
    }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}
