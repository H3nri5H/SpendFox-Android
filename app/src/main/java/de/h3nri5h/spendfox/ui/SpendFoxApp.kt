package de.h3nri5h.spendfox.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.h3nri5h.spendfox.data.Expense
import de.h3nri5h.spendfox.data.ExpenseCategory
import de.h3nri5h.spendfox.data.FuelType
import de.h3nri5h.spendfox.data.Product
import de.h3nri5h.spendfox.data.ProductCategory
import de.h3nri5h.spendfox.data.Vehicle
import de.h3nri5h.spendfox.domain.Money

private enum class SpendFoxTab(val label: String, val symbol: String) {
    Dashboard("Übersicht", "◎"),
    Expenses("Ausgaben", "€"),
    Products("Produkte", "□"),
    Vehicles("Fahrzeuge", "🚗"),
    Settings("Einstellungen", "⚙")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendFoxApp(viewModel: SpendFoxViewModel = viewModel(factory = SpendFoxViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(SpendFoxTab.Dashboard) }
    var showingExpenseDialog by rememberSaveable { mutableStateOf(false) }
    var showingProductDialog by rememberSaveable { mutableStateOf(false) }
    var showingVehicleDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(selectedTab.label) })
        },
        bottomBar = {
            NavigationBar {
                SpendFoxTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.symbol) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            SpendFoxTab.Dashboard -> DashboardScreen(state, padding)
            SpendFoxTab.Expenses -> ExpensesScreen(
                expenses = state.expenses,
                padding = padding,
                onAdd = { showingExpenseDialog = true },
                onDelete = viewModel::deleteExpense
            )
            SpendFoxTab.Products -> ProductsScreen(
                products = state.products,
                padding = padding,
                onAdd = { showingProductDialog = true },
                onDelete = viewModel::deleteProduct
            )
            SpendFoxTab.Vehicles -> VehiclesScreen(
                vehicles = state.vehicles,
                padding = padding,
                onAdd = { showingVehicleDialog = true },
                onDelete = viewModel::deleteVehicle
            )
            SpendFoxTab.Settings -> SettingsScreen(
                state = state,
                padding = padding,
                onClearAll = viewModel::clearAll
            )
        }
    }

    if (showingExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showingExpenseDialog = false },
            onSave = viewModel::addExpense
        )
    }
    if (showingProductDialog) {
        AddProductDialog(
            onDismiss = { showingProductDialog = false },
            onSave = viewModel::addProduct
        )
    }
    if (showingVehicleDialog) {
        AddVehicleDialog(
            onDismiss = { showingVehicleDialog = false },
            onSave = viewModel::addVehicle
        )
    }
}

@Composable
private fun DashboardScreen(state: SpendFoxUiState, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryCard("Ausgaben gesamt", Money.format(state.monthlyExpenseTotalCents), "Aktueller lokaler Datenstand")
        }
        item {
            SummaryCard("Produktwert", Money.format(state.productValueCents), "Summe der erfassten Produkte")
        }
        item {
            SummaryCard("Fahrzeuge", state.vehicles.size.toString(), "Erfasste Fahrzeuge")
        }
        item {
            Text("Letzte Ausgaben", style = MaterialTheme.typography.titleMedium)
        }
        if (state.expenses.isEmpty()) {
            item { EmptyState("Noch keine Ausgaben vorhanden.") }
        } else {
            items(state.expenses.take(5), key = { it.id }) { expense ->
                ExpenseCard(expense, onDelete = null)
            }
        }
    }
}

@Composable
private fun ExpensesScreen(expenses: List<Expense>, padding: PaddingValues, onAdd: () -> Unit, onDelete: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = expenses.filter {
        val text = listOf(it.merchant, it.category.label, it.note).joinToString(" ").lowercase()
        text.contains(query.lowercase())
    }

    ListScreen(padding, onAdd, "Neue Ausgabe") {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Suchen") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filtered.isEmpty()) {
            item { EmptyState("Keine passenden Ausgaben gefunden.") }
        } else {
            items(filtered, key = { it.id }) { expense ->
                ExpenseCard(expense) { onDelete(expense.id) }
            }
        }
    }
}

@Composable
private fun ProductsScreen(products: List<Product>, padding: PaddingValues, onAdd: () -> Unit, onDelete: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = products.filter {
        val text = listOf(it.name, it.manufacturer, it.category.label).joinToString(" ").lowercase()
        text.contains(query.lowercase())
    }

    ListScreen(padding, onAdd, "Neues Produkt") {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Suchen") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filtered.isEmpty()) {
            item { EmptyState("Keine passenden Produkte gefunden.") }
        } else {
            items(filtered, key = { it.id }) { product ->
                ProductCard(product) { onDelete(product.id) }
            }
        }
    }
}

@Composable
private fun VehiclesScreen(vehicles: List<Vehicle>, padding: PaddingValues, onAdd: () -> Unit, onDelete: (String) -> Unit) {
    ListScreen(padding, onAdd, "Neues Fahrzeug") {
        if (vehicles.isEmpty()) {
            item { EmptyState("Noch keine Fahrzeuge vorhanden.") }
        } else {
            items(vehicles, key = { it.id }) { vehicle ->
                VehicleCard(vehicle) { onDelete(vehicle.id) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: SpendFoxUiState, padding: PaddingValues, onClearAll: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SummaryCard("Lokale Datenbank", "SQLite", "Keine Cloud, keine Analytics, keine externen APIs") }
        item { SummaryCard("Ausgaben", state.expenses.size.toString(), "Lokale Datensätze") }
        item { SummaryCard("Produkte", state.products.size.toString(), "Lokale Datensätze") }
        item { SummaryCard("Fahrzeuge", state.vehicles.size.toString(), "Lokale Datensätze") }
        item {
            Button(onClick = onClearAll, modifier = Modifier.fillMaxWidth()) {
                Text("Alle lokalen Daten löschen")
            }
        }
    }
}

@Composable
private fun ListScreen(
    padding: PaddingValues,
    onAdd: () -> Unit,
    addLabel: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text(addLabel)
            }
        }
        content()
    }
}

@Composable
private fun SummaryCard(title: String, value: String, caption: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExpenseCard(expense: Expense, onDelete: (() -> Unit)?) {
    RecordCard(
        title = expense.merchant,
        value = Money.format(expense.amountCents),
        subtitle = "${expense.category.label} · ${Money.formatDate(expense.occurredAtEpochMillis)}",
        onDelete = onDelete
    )
}

@Composable
private fun ProductCard(product: Product, onDelete: () -> Unit) {
    RecordCard(
        title = product.name,
        value = Money.format(product.purchasePriceCents),
        subtitle = listOf(product.manufacturer, product.category.label).filter { it.isNotBlank() }.joinToString(" · "),
        onDelete = onDelete
    )
}

@Composable
private fun VehicleCard(vehicle: Vehicle, onDelete: () -> Unit) {
    RecordCard(
        title = vehicle.displayName,
        value = vehicle.fuelType.label,
        subtitle = listOf(vehicle.manufacturer, vehicle.modelName, vehicle.licensePlate).filter { it.isNotBlank() }.joinToString(" · "),
        onDelete = onDelete
    )
}

@Composable
private fun RecordCard(title: String, value: String, subtitle: String, onDelete: (() -> Unit)?) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDelete) { Text("Löschen") }
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (String, String, ExpenseCategory, String) -> Boolean) {
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ExpenseCategory.Groceries) }
    var hasError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Ausgabe") },
        text = {
            DialogForm(hasError) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Betrag") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Händler") }, modifier = Modifier.fillMaxWidth())
                CategoryChips(ExpenseCategory.entries, category, { category = it }) { it.label }
                OutlinedTextField(note, { note = it }, label = { Text("Notiz") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (onSave(amount, merchant, category, note)) onDismiss() else hasError = true }) { Text("Sichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun AddProductDialog(onDismiss: () -> Unit, onSave: (String, String, String, ProductCategory) -> Boolean) {
    var name by rememberSaveable { mutableStateOf("") }
    var manufacturer by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ProductCategory.Technology) }
    var hasError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Produkt") },
        text = {
            DialogForm(hasError) {
                OutlinedTextField(name, { name = it }, label = { Text("Bezeichnung") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(manufacturer, { manufacturer = it }, label = { Text("Hersteller") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(price, { price = it }, label = { Text("Kaufpreis") }, modifier = Modifier.fillMaxWidth())
                CategoryChips(ProductCategory.entries, category, { category = it }) { it.label }
            }
        },
        confirmButton = {
            Button(onClick = { if (onSave(name, manufacturer, price, category)) onDismiss() else hasError = true }) { Text("Sichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun AddVehicleDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, FuelType) -> Boolean) {
    var name by rememberSaveable { mutableStateOf("") }
    var manufacturer by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var plate by rememberSaveable { mutableStateOf("") }
    var fuelType by rememberSaveable { mutableStateOf(FuelType.Petrol) }
    var hasError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Fahrzeug") },
        text = {
            DialogForm(hasError) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(manufacturer, { manufacturer = it }, label = { Text("Hersteller") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(model, { model = it }, label = { Text("Modell") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(plate, { plate = it }, label = { Text("Kennzeichen") }, modifier = Modifier.fillMaxWidth())
                CategoryChips(FuelType.entries, fuelType, { fuelType = it }) { it.label }
            }
        },
        confirmButton = {
            Button(onClick = { if (onSave(name, manufacturer, model, plate, fuelType)) onDismiss() else hasError = true }) { Text("Sichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun DialogForm(hasError: Boolean, content: @Composable Column.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (hasError) {
            Text("Bitte Pflichtfelder und Betrag prüfen.", color = MaterialTheme.colorScheme.error)
        }
        content()
    }
}

@Composable
private fun <T> CategoryChips(options: List<T>, selected: T, onSelected: (T) -> Unit, label: (T) -> String) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelected(option) },
                label = { Text(label(option)) }
            )
        }
    }
}
